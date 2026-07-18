package com.tracker.core.provider

import com.tracker.core.collector.ExerciseMetadata
import com.tracker.core.collector.HealthConnectBodyMeasurementsCollector
import com.tracker.core.collector.HealthConnectDistanceCollector
import com.tracker.core.collector.HealthConnectExerciseCollector
import com.tracker.core.collector.HealthConnectStepCollector
import com.tracker.core.collector.HealthConnectTrainingCollector
import com.tracker.core.collector.SystemServiceUnavailableException
import com.tracker.core.model.DistanceBucket
import com.tracker.core.model.BodyMeasurementsEvidence
import com.tracker.core.model.DistanceEvidence
import com.tracker.core.model.DurationEvidence
import com.tracker.core.model.StepBucket
import com.tracker.core.model.StepEvidence
import com.tracker.core.model.TrainingEvidence
import com.tracker.core.types.DataSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import java.time.Instant

/** Covers the thin Health Connect provider wrappers (steps, distance, exercise). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HealthConnectProvidersTest {

    // --- Body measurements ---

    @Test
    fun bodyMeasurements_preservesSeparateRecordStreams() = runBlocking {
        val collector = mockk<HealthConnectBodyMeasurementsCollector>()
        val earlierWeight = bodyWeightRecord(time = 1_000L)
        val laterWeight = bodyWeightRecord(time = 5_000L)
        coEvery { collector.collect(FROM, TO) } returns BodyMeasurementsEvidence(
            source = DataSource.HEALTH_CONNECT,
            metadata = emptyMap(),
            weightRecords = listOf(earlierWeight, laterWeight),
            bodyFatRecords = emptyList(),
            leanBodyMassRecords = emptyList(),
            boneMassRecords = emptyList(),
            bodyWaterMassRecords = emptyList(),
            basalMetabolicRateRecords = emptyList(),
            heightRecords = emptyList()
        )

        val result = BodyMeasurementsProvider(collector).query(FROM, TO) ?: error("expected result")

        assertEquals(listOf(earlierWeight, laterWeight), result.weightRecords)
        assertEquals(2, result.recordCount)
        assertEquals(listOf(DataSource.HEALTH_CONNECT), result.sources)
    }

    @Test
    fun bodyMeasurements_emptyEvidence_returnsNull() = runBlocking {
        val collector = mockk<HealthConnectBodyMeasurementsCollector>()
        coEvery { collector.collect(FROM, TO) } returns BodyMeasurementsEvidence(
            source = DataSource.HEALTH_CONNECT,
            metadata = emptyMap(),
            weightRecords = emptyList(),
            bodyFatRecords = emptyList(),
            leanBodyMassRecords = emptyList(),
            boneMassRecords = emptyList(),
            bodyWaterMassRecords = emptyList(),
            basalMetabolicRateRecords = emptyList(),
            heightRecords = emptyList()
        )

        assertNull(BodyMeasurementsProvider(collector).query(FROM, TO))
    }

    // --- Steps ---

    @Test
    fun steps_mapsBucketsToSessionsAndTotals() = runBlocking {
        val collector = mockk<HealthConnectStepCollector>()
        coEvery { collector.collect(FROM, TO) } returns StepEvidence(
            source = DataSource.HEALTH_CONNECT,
            metadata = emptyMap(),
            buckets = listOf(
                StepBucket(startTime = 0L, endTime = 10L, steps = 100L),
                StepBucket(startTime = 10L, endTime = 20L, steps = 50L)
            )
        )

        val result = StepCountingProvider(collector).query(FROM, TO) ?: error("expected result")

        assertEquals(listOf(DataSource.HEALTH_CONNECT), result.sources)
        assertEquals(150L, result.totalSteps)
        assertEquals(2, result.sessions.size)
    }

    @Test
    fun steps_collectorException_returnsNull() = runBlocking {
        val collector = mockk<HealthConnectStepCollector>()
        coEvery { collector.collect(FROM, TO) } throws SystemServiceUnavailableException("HC")

        assertNull(StepCountingProvider(collector).query(FROM, TO))
    }

    @Test
    fun steps_emptyBuckets_returnsResultWithZeroTotal() = runBlocking {
        val collector = mockk<HealthConnectStepCollector>()
        coEvery { collector.collect(FROM, TO) } returns StepEvidence(
            source = DataSource.HEALTH_CONNECT, metadata = emptyMap(), buckets = emptyList()
        )

        val result = StepCountingProvider(collector).query(FROM, TO) ?: error("expected result")

        assertEquals(0L, result.totalSteps)
        assertEquals(0, result.sessions.size)
    }

    // --- Distance ---

    @Test
    fun distance_mapsBucketsToSessionsAndTotals() = runBlocking {
        val collector = mockk<HealthConnectDistanceCollector>()
        coEvery { collector.collect(FROM, TO) } returns DistanceEvidence(
            source = DataSource.HEALTH_CONNECT,
            metadata = emptyMap(),
            buckets = listOf(
                DistanceBucket(startTime = 0L, endTime = 10L, meters = 1500.0),
                DistanceBucket(startTime = 10L, endTime = 20L, meters = 500.0)
            )
        )

        val result = DistanceProvider(collector).query(FROM, TO) ?: error("expected result")

        assertEquals(2000.0, result.totalMeters, 0.0001)
        assertEquals(2.0, result.totalKilometers, 0.0001)
    }

    @Test
    fun distance_collectorException_returnsNull() = runBlocking {
        val collector = mockk<HealthConnectDistanceCollector>()
        coEvery { collector.collect(FROM, TO) } throws SystemServiceUnavailableException("HC")

        assertNull(DistanceProvider(collector).query(FROM, TO))
    }

    // --- Exercise ---

    @Test
    fun exercise_emptyEvidence_returnsNull() = runBlocking {
        val collector = mockk<HealthConnectExerciseCollector>()
        coEvery { collector.collect(FROM, TO) } returns emptyList()

        assertNull(ExerciseProvider(collector).query(FROM, TO))
    }

    @Test
    fun exercise_collectorException_returnsNull() = runBlocking {
        val collector = mockk<HealthConnectExerciseCollector>()
        coEvery { collector.collect(FROM, TO) } throws SystemServiceUnavailableException("HC")

        assertNull(ExerciseProvider(collector).query(FROM, TO))
    }

    @Test
    fun exercise_sortsSessionsByStartTime_andSumsDuration() = runBlocking {
        val collector = mockk<HealthConnectExerciseCollector>()
        coEvery { collector.collect(FROM, TO) } returns listOf(
            exerciseEvidence(start = 5_000L, end = 9_000L, minutes = 30, typeId = 56, type = "running"),
            exerciseEvidence(start = 1_000L, end = 2_000L, minutes = 0, typeId = 79, type = "strength_training")
        )

        val result = ExerciseProvider(collector).query(FROM, TO) ?: error("expected result")

        assertEquals(listOf(1_000L, 5_000L), result.sessions.map { it.startTime })
        assertEquals(listOf("strength_training", "running"), result.sessions.map { it.exerciseType })
        assertEquals(30, result.durationMinutes)
    }

    @Test
    fun exercise_allEvidenceUnparseable_returnsNull() = runBlocking {
        val collector = mockk<HealthConnectExerciseCollector>()
        coEvery { collector.collect(FROM, TO) } returns listOf(
            DurationEvidence(
                source = DataSource.HEALTH_CONNECT,
                metadata = mapOf("garbage" to "value"), // fails ExerciseMetadata.fromMap
                durationMinutes = 10,
                startTimeMillis = 1_000L,
                endTimeMillis = 2_000L
            )
        )

        assertNull(ExerciseProvider(collector).query(FROM, TO))
    }

    // --- Training ---

    @Test
    fun training_preservesRecords_sortsSessions_andSumsDuration() = runBlocking {
        val collector = mockk<HealthConnectTrainingCollector>()
        val later = trainingRecord(start = 5_000L, end = 125_000L, type = 56)
        val earlier = trainingRecord(start = 1_000L, end = 61_000L, type = 70)
        coEvery { collector.collect(FROM, TO) } returns TrainingEvidence(
            source = DataSource.HEALTH_CONNECT,
            metadata = emptyMap(),
            sessions = listOf(later, earlier)
        )

        val result = TrainingProvider(collector).query(FROM, TO) ?: error("expected result")

        assertEquals(listOf(1_000L, 5_000L), result.sessions.map { it.startTime })
        assertEquals(listOf(earlier, later), result.sessions.map { it.record })
        assertEquals(listOf("strength_training", "running"), result.sessions.map { it.exerciseType })
        assertEquals(3L, result.durationMinutes)
    }

    @Test
    fun training_collectorException_returnsNull() = runBlocking {
        val collector = mockk<HealthConnectTrainingCollector>()
        coEvery { collector.collect(FROM, TO) } throws SystemServiceUnavailableException("HC")

        assertNull(TrainingProvider(collector).query(FROM, TO))
    }

    private fun exerciseEvidence(
        start: Long, end: Long, minutes: Int, typeId: Int, type: String
    ): DurationEvidence = DurationEvidence(
        source = DataSource.HEALTH_CONNECT,
        metadata = ExerciseMetadata(exerciseTypeId = typeId, exerciseType = type).toMap(),
        durationMinutes = minutes,
        startTimeMillis = start,
        endTimeMillis = end
    )

    private fun trainingRecord(start: Long, end: Long, type: Int): PlannedExerciseSessionRecord {
        val record = mockk<PlannedExerciseSessionRecord>()
        every { record.startTime } returns Instant.ofEpochMilli(start)
        every { record.endTime } returns Instant.ofEpochMilli(end)
        every { record.exerciseType } returns type
        return record
    }

    private fun bodyWeightRecord(time: Long): WeightRecord {
        val record = mockk<WeightRecord>()
        every { record.time } returns Instant.ofEpochMilli(time)
        return record
    }

    private companion object {
        const val FROM = 0L
        const val TO = 100_000L
    }
}
