package com.tracker.core.result

import com.tracker.core.types.DataSource
import org.junit.Assert.assertEquals
import org.junit.Test

/** Covers the computed convenience accessors on the result types. */
class ResultAccessorsTest {

    private val range = TimeRange(0L, 100L)

    @Test
    fun stepCounting_totalSteps_sumsBuckets() {
        val result = StepCountingResult(
            sources = listOf(DataSource.HEALTH_CONNECT),
            timeRange = range,
            sessions = listOf(
                StepSession(startTime = 0L, endTime = 10L, steps = 100L),
                StepSession(startTime = 10L, endTime = 20L, steps = 22L)
            )
        )

        assertEquals(122L, result.totalSteps)
    }

    @Test
    fun stepCounting_totalSteps_isZeroWhenEmpty() {
        val result = StepCountingResult(
            sources = listOf(DataSource.HEALTH_CONNECT),
            timeRange = range,
            sessions = emptyList()
        )

        assertEquals(0L, result.totalSteps)
    }

    @Test
    fun distance_totalMetersAndKilometers() {
        val result = DistanceResult(
            sources = listOf(DataSource.HEALTH_CONNECT),
            timeRange = range,
            sessions = listOf(
                DistanceSession(startTime = 0L, endTime = 10L, meters = 1500.0),
                DistanceSession(startTime = 10L, endTime = 20L, meters = 420.0)
            )
        )

        assertEquals(1920.0, result.totalMeters, 0.0001)
        assertEquals(1.92, result.totalKilometers, 0.0001)
    }

    @Test
    fun distance_totalsAreZeroWhenEmpty() {
        val result = DistanceResult(
            sources = listOf(DataSource.HEALTH_CONNECT),
            timeRange = range,
            sessions = emptyList()
        )

        assertEquals(0.0, result.totalMeters, 0.0001)
        assertEquals(0.0, result.totalKilometers, 0.0001)
    }

    @Test
    fun movieWatching_countMatchesSessionsSize() {
        val session = MovieSession(title = "A", publishedDate = 1L, watchedDate = 1L)
        val result = MovieWatchingResult(
            sources = listOf(DataSource.LETTERBOXD_RSS),
            timeRange = range,
            sessions = listOf(session, session.copy(title = "B"))
        )

        assertEquals(2, result.count)
    }

    @Test
    fun movieWatching_countIsZeroForDefaultEmptySessions() {
        val result = MovieWatchingResult(
            sources = listOf(DataSource.LETTERBOXD_RSS),
            timeRange = range
        )

        assertEquals(0, result.count)
    }
}
