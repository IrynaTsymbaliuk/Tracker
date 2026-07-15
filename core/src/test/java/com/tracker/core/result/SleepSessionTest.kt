package com.tracker.core.result

import com.tracker.core.types.SleepQuality
import com.tracker.core.types.SleepStageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the derived figures on [SleepSession] (durations asleep/awake, per-stage minutes,
 * efficiency, and the [SleepQuality] banding) plus [SleepResult] totals.
 */
class SleepSessionTest {

    private val hour = 3_600_000L

    private fun stage(startHour: Long, endHour: Long, type: SleepStageType) =
        SleepStage(startTime = startHour * hour, endTime = endHour * hour, type = type)

    @Test
    fun stagedSession_computesDurationsEfficiencyAndQuality() {
        // 8h in bed: 1h light + 4h deep + 2h REM = 7h asleep, 1h awake -> 87.5% efficiency -> GOOD
        val session = SleepSession(
            startTime = 0L,
            endTime = 8 * hour,
            stages = listOf(
                stage(0, 1, SleepStageType.LIGHT),
                stage(1, 5, SleepStageType.DEEP),
                stage(5, 7, SleepStageType.REM),
                stage(7, 8, SleepStageType.AWAKE)
            )
        )

        assertEquals(8 * 60, session.timeInBedMinutes)
        assertEquals(7 * 60, session.asleepMinutes)
        assertEquals(60, session.awakeMinutes)
        assertEquals(60, session.lightMinutes)
        assertEquals(4 * 60, session.deepMinutes)
        assertEquals(2 * 60, session.remMinutes)
        assertEquals(0.875, session.efficiency!!, 0.0001)
        assertEquals(SleepQuality.GOOD, session.quality)
    }

    @Test
    fun unstagedSession_fallsBackToTimeInBed_andQualityUnknown() {
        val session = SleepSession(startTime = 0L, endTime = 7 * hour, stages = emptyList())

        assertEquals(7 * 60, session.timeInBedMinutes)
        assertEquals(7 * 60, session.asleepMinutes) // whole session assumed asleep
        assertEquals(0, session.awakeMinutes)
        assertNull(session.efficiency)              // no awake data to measure
        assertEquals(SleepQuality.UNKNOWN, session.quality)
    }

    @Test
    fun efficiencyBands_mapToExpectedQuality() {
        // 95% asleep -> EXCELLENT
        assertEquals(
            SleepQuality.EXCELLENT,
            SleepSession(
                0L, 100 * hour,
                listOf(stage(0, 95, SleepStageType.DEEP), stage(95, 100, SleepStageType.AWAKE))
            ).quality
        )
        // 70% asleep -> POOR
        assertEquals(
            SleepQuality.POOR,
            SleepSession(
                0L, 100 * hour,
                listOf(stage(0, 70, SleepStageType.LIGHT), stage(70, 100, SleepStageType.AWAKE))
            ).quality
        )
    }

    @Test
    fun sleepResult_totalsSumAcrossSessions() {
        val night1 = SleepSession(0L, 8 * hour, listOf(stage(0, 7, SleepStageType.DEEP), stage(7, 8, SleepStageType.AWAKE)))
        val nap = SleepSession(20 * hour, 21 * hour, emptyList())
        val result = SleepResult(
            sources = emptyList(),
            timeRange = TimeRange(0L, 24 * hour),
            sessions = listOf(night1, nap)
        )

        assertEquals(8 * 60L, result.totalSleepMinutes) // 7h + 1h asleep
        assertEquals(8.0, result.totalSleepHours, 0.0001)
    }
}
