package com.tracker.core.collector

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.units.Length
import com.tracker.core.types.DataSource
import com.tracker.core.types.SleepStageType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Covers the shared gating logic (SDK availability + permission) and the record→evidence
 * mapping of the Health Connect collectors, by mocking the [HealthConnectClient] companion
 * and the client instance.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HealthConnectCollectorsTest {

    private val context = RuntimeEnvironment.getApplication()
    private val client = mockk<HealthConnectClient>()
    private val permissionController = mockk<PermissionController>()
    private val features = mockk<HealthConnectFeatures>()

    @Before
    fun setUp() {
        mockkObject(HealthConnectClient.Companion)
        every { HealthConnectClient.getOrCreate(any(), any()) } returns client
        every { client.permissionController } returns permissionController
        every { client.features } returns features
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun sdkAvailable() {
        every { HealthConnectClient.getSdkStatus(any(), any()) } returns HealthConnectClient.SDK_AVAILABLE
    }

    // --- Steps ---

    @Test
    fun steps_sdkUnavailable_throwsSystemServiceUnavailable() {
        every { HealthConnectClient.getSdkStatus(any(), any()) } returns HealthConnectClient.SDK_UNAVAILABLE

        assertThrows(SystemServiceUnavailableException::class.java) {
            runBlocking { stepCollector().collect(0L, 100L) }
        }
    }

    @Test
    fun steps_permissionMissing_throwsPermissionDenied() {
        sdkAvailable()
        coEvery { permissionController.getGrantedPermissions() } returns emptySet()

        assertThrows(PermissionDeniedException::class.java) {
            runBlocking { stepCollector().collect(0L, 100L) }
        }
    }

    @Test
    fun steps_happyPath_mapsBucket() = runBlocking {
        sdkAvailable()
        coEvery { permissionController.getGrantedPermissions() } returns
            setOf(HealthConnectStepCollector.READ_STEPS_PERMISSION)
        coEvery { client.aggregateGroupByDuration(any()) } returns listOf(
            stepGroup(start = 0L, end = 3_600_000L, steps = 100L)
        )

        val evidence = stepCollector().collect(0L, 3_600_000L)

        assertEquals(DataSource.HEALTH_CONNECT, evidence.source)
        assertEquals(1, evidence.buckets.size)
        assertEquals(100L, evidence.buckets[0].steps)
    }

    // --- Body measurements ---

    @Test
    fun bodyMeasurements_permissionMissing_throwsPermissionDenied() {
        sdkAvailable()
        coEvery { permissionController.getGrantedPermissions() } returns emptySet()

        assertThrows(PermissionDeniedException::class.java) {
            runBlocking { bodyMeasurementsCollector().collect(0L, 100L) }
        }
    }

    @Test
    fun bodyMeasurements_readsGrantedTypesAndPreservesTheirRecords() = runBlocking {
        sdkAvailable()
        coEvery { permissionController.getGrantedPermissions() } returns
            setOf(HealthConnectBodyMeasurementsCollector.READ_WEIGHT_PERMISSION)
        val record = bodyWeightRecord(time = 10L)
        coEvery { client.readRecords(any<ReadRecordsRequest<WeightRecord>>()) } returns
            ReadRecordsResponse(records = listOf(record), pageToken = null)

        val evidence = bodyMeasurementsCollector().collect(0L, 100L)

        assertEquals(listOf(record), evidence.weightRecords)
        assertEquals(emptyList<Any>(), evidence.bodyFatRecords)
    }

    // --- Distance ---

    @Test
    fun distance_happyPath_mapsMeters() = runBlocking {
        sdkAvailable()
        coEvery { permissionController.getGrantedPermissions() } returns
            setOf(HealthConnectDistanceCollector.READ_DISTANCE_PERMISSION)
        coEvery { client.aggregateGroupByDuration(any()) } returns listOf(
            distanceGroup(start = 0L, end = 3_600_000L, meters = 1500.0)
        )

        val evidence = distanceCollector().collect(0L, 3_600_000L)

        assertEquals(1, evidence.buckets.size)
        assertEquals(1500.0, evidence.buckets[0].meters, 0.0001)
    }

    // --- Sleep ---

    @Test
    fun sleep_permissionMissing_throwsPermissionDenied() {
        sdkAvailable()
        coEvery { permissionController.getGrantedPermissions() } returns emptySet()

        assertThrows(PermissionDeniedException::class.java) {
            runBlocking { sleepCollector().collect(0L, 100L) }
        }
    }

    @Test
    fun sleep_happyPath_mapsSessionAndStages() = runBlocking {
        sdkAvailable()
        coEvery { permissionController.getGrantedPermissions() } returns
            setOf(HealthConnectSleepCollector.READ_SLEEP_PERMISSION)
        // One night: fell asleep at t=0, woke 8h later, with deep + awake stages.
        coEvery { client.readRecords(any<ReadRecordsRequest<SleepSessionRecord>>()) } returns
            ReadRecordsResponse(
                records = listOf(
                    sleepRecord(
                        start = 0L,
                        end = HOUR * 8,
                        stages = listOf(
                            stage(0L, HOUR * 6, SleepSessionRecord.STAGE_TYPE_DEEP),
                            stage(HOUR * 6, HOUR * 8, SleepSessionRecord.STAGE_TYPE_AWAKE)
                        )
                    )
                ),
                pageToken = null
            )

        val evidence = sleepCollector().collect(0L, HOUR * 8)

        assertEquals(DataSource.HEALTH_CONNECT, evidence.source)
        assertEquals(1, evidence.sessions.size)
        val session = evidence.sessions[0]
        assertEquals(0L, session.startTime)
        assertEquals(HOUR * 8, session.endTime)
        assertEquals(
            listOf(SleepStageType.DEEP, SleepStageType.AWAKE),
            session.stages.map { it.type }
        )
    }

    @Test
    fun sleep_sortsSessionsByStartTime() = runBlocking {
        sdkAvailable()
        coEvery { permissionController.getGrantedPermissions() } returns
            setOf(HealthConnectSleepCollector.READ_SLEEP_PERMISSION)
        coEvery { client.readRecords(any<ReadRecordsRequest<SleepSessionRecord>>()) } returns
            ReadRecordsResponse(
                records = listOf(
                    sleepRecord(start = HOUR * 10, end = HOUR * 11, stages = emptyList()),
                    sleepRecord(start = 0L, end = HOUR * 8, stages = emptyList())
                ),
                pageToken = null
            )

        val evidence = sleepCollector().collect(0L, HOUR * 24)

        assertEquals(listOf(0L, HOUR * 10), evidence.sessions.map { it.startTime })
    }

    // --- Exercise ---

    @Test
    fun exercise_mapsKnownTypeAndFallsBackToOtherForUnknown() = runBlocking {
        sdkAvailable()
        coEvery { permissionController.getGrantedPermissions() } returns
            setOf(HealthConnectExerciseCollector.READ_EXERCISE_PERMISSION)
        coEvery { client.readRecords(any<ReadRecordsRequest<ExerciseSessionRecord>>()) } returns
            ReadRecordsResponse(
                records = listOf(
                    exerciseRecord(start = 0L, end = 120_000L, type = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING),
                    exerciseRecord(start = 0L, end = 30_000L, type = UNKNOWN_EXERCISE_TYPE)
                ),
                pageToken = null
            )

        val evidence = exerciseCollector().collect(0L, 200_000L)

        val types = evidence.map { ExerciseMetadata.fromMap(it.metadata)?.exerciseType }
        assertEquals(listOf("running", "other"), types)
        assertEquals(listOf(2, 0), evidence.map { it.durationMinutes })
    }

    @Test
    fun exercise_permissionMissing_throwsPermissionDenied() {
        sdkAvailable()
        coEvery { permissionController.getGrantedPermissions() } returns emptySet()

        assertThrows(PermissionDeniedException::class.java) {
            runBlocking { exerciseCollector().collect(0L, 100L) }
        }
    }

    // --- Mindfulness (old-client path) ---

    // --- Training ---

    @Test
    fun training_featureUnavailable_throwsSystemServiceUnavailable() {
        sdkAvailable()
        every {
            features.getFeatureStatus(HealthConnectFeatures.FEATURE_PLANNED_EXERCISE)
        } returns HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE

        assertThrows(SystemServiceUnavailableException::class.java) {
            runBlocking { trainingCollector().collect(0L, 100L) }
        }
    }

    @Test
    fun training_happyPath_preservesCompleteRecords() = runBlocking {
        sdkAvailable()
        every {
            features.getFeatureStatus(HealthConnectFeatures.FEATURE_PLANNED_EXERCISE)
        } returns HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        coEvery { permissionController.getGrantedPermissions() } returns
            setOf(HealthConnectTrainingCollector.READ_PLANNED_EXERCISE_PERMISSION)
        val record = trainingRecord(start = 0L, end = HOUR)
        coEvery { client.readRecords(any<ReadRecordsRequest<PlannedExerciseSessionRecord>>()) } returns
            ReadRecordsResponse(records = listOf(record), pageToken = null)

        val evidence = trainingCollector().collect(0L, HOUR)

        assertEquals(DataSource.HEALTH_CONNECT, evidence.source)
        assertEquals(listOf(record), evidence.sessions)
    }

    @Test
    fun mindfulness_missingApiOnOldClient_throwsSystemServiceUnavailable() {
        sdkAvailable()
        coEvery { permissionController.getGrantedPermissions() } throws NoSuchMethodError("old client")

        assertThrows(SystemServiceUnavailableException::class.java) {
            runBlocking { mindfulnessCollector().collect(0L, 100L) }
        }
    }

    // --- collector factories (Unconfined dispatcher so runBlocking executes inline) ---

    private fun stepCollector() = HealthConnectStepCollector(context, Dispatchers.Unconfined)
    private fun distanceCollector() = HealthConnectDistanceCollector(context, Dispatchers.Unconfined)
    private fun sleepCollector() = HealthConnectSleepCollector(context, Dispatchers.Unconfined)
    private fun exerciseCollector() = HealthConnectExerciseCollector(context, Dispatchers.Unconfined)
    private fun trainingCollector() = HealthConnectTrainingCollector(context, Dispatchers.Unconfined)
    private fun bodyMeasurementsCollector() =
        HealthConnectBodyMeasurementsCollector(context, Dispatchers.Unconfined)
    private fun mindfulnessCollector() = HealthConnectMindfulnessCollector(context, Dispatchers.Unconfined)

    // --- fixtures ---

    private fun stepGroup(start: Long, end: Long, steps: Long): AggregationResultGroupedByDuration {
        val agg = mockk<AggregationResult>()
        every { agg[StepsRecord.COUNT_TOTAL] } returns steps
        return group(start, end, agg)
    }

    private fun distanceGroup(start: Long, end: Long, meters: Double): AggregationResultGroupedByDuration {
        val length = mockk<Length>()
        every { length.inMeters } returns meters
        val agg = mockk<AggregationResult>()
        every { agg[DistanceRecord.DISTANCE_TOTAL] } returns length
        return group(start, end, agg)
    }

    private fun sleepRecord(
        start: Long,
        end: Long,
        stages: List<SleepSessionRecord.Stage>
    ): SleepSessionRecord {
        val record = mockk<SleepSessionRecord>()
        every { record.startTime } returns Instant.ofEpochMilli(start)
        every { record.endTime } returns Instant.ofEpochMilli(end)
        every { record.stages } returns stages
        return record
    }

    private fun stage(start: Long, end: Long, type: Int): SleepSessionRecord.Stage =
        SleepSessionRecord.Stage(
            startTime = Instant.ofEpochMilli(start),
            endTime = Instant.ofEpochMilli(end),
            stage = type
        )

    private fun group(start: Long, end: Long, agg: AggregationResult): AggregationResultGroupedByDuration {
        val g = mockk<AggregationResultGroupedByDuration>()
        every { g.startTime } returns Instant.ofEpochMilli(start)
        every { g.endTime } returns Instant.ofEpochMilli(end)
        every { g.result } returns agg
        return g
    }

    private fun exerciseRecord(start: Long, end: Long, type: Int): ExerciseSessionRecord {
        val record = mockk<ExerciseSessionRecord>()
        every { record.startTime } returns Instant.ofEpochMilli(start)
        every { record.endTime } returns Instant.ofEpochMilli(end)
        every { record.exerciseType } returns type
        return record
    }

    private fun trainingRecord(start: Long, end: Long): PlannedExerciseSessionRecord {
        val record = mockk<PlannedExerciseSessionRecord>()
        every { record.startTime } returns Instant.ofEpochMilli(start)
        every { record.endTime } returns Instant.ofEpochMilli(end)
        return record
    }

    private fun bodyWeightRecord(time: Long): WeightRecord {
        val record = mockk<WeightRecord>()
        every { record.time } returns Instant.ofEpochMilli(time)
        return record
    }

    private companion object {
        const val UNKNOWN_EXERCISE_TYPE = 9999 // not a real HC constant -> maps to "other"
        const val HOUR = 3_600_000L
    }
}
