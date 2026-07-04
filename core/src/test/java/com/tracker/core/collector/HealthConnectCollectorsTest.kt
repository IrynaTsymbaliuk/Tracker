package com.tracker.core.collector

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.units.Length
import com.tracker.core.types.DataSource
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

    @Before
    fun setUp() {
        mockkObject(HealthConnectClient.Companion)
        every { HealthConnectClient.getOrCreate(any(), any()) } returns client
        every { client.permissionController } returns permissionController
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
        assertEquals(0.99f, evidence.confidence)
        assertEquals(1, evidence.buckets.size)
        assertEquals(100L, evidence.buckets[0].steps)
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
    private fun exerciseCollector() = HealthConnectExerciseCollector(context, Dispatchers.Unconfined)
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

    private companion object {
        const val UNKNOWN_EXERCISE_TYPE = 9999 // not a real HC constant -> maps to "other"
    }
}
