package com.tracker.core

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class QueryWindowCalculatorTest {

    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun calculate_daysOneStartsAtLocalMidnightAndEndsAtNow() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val now = instant("2026-07-04T15:45:12.345Z")

        val (from, to) = QueryWindowCalculator.calculate(days = 1, nowMillis = now)

        assertEquals(instant("2026-07-04T00:00:00Z"), from)
        assertEquals(now, to)
    }

    @Test
    fun calculate_multiDayWindowStartsAtMidnightOfFirstIncludedLocalDay() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        val now = instant("2026-07-04T15:30:00Z") // 2026-07-05 00:30 in Tokyo.

        val (from, to) = QueryWindowCalculator.calculate(days = 2, nowMillis = now)

        assertEquals(instant("2026-07-03T15:00:00Z"), from) // 2026-07-04 00:00 in Tokyo.
        assertEquals(now, to)
    }

    @Test
    fun calculate_rejectsZeroDays() {
        assertThrows(IllegalArgumentException::class.java) {
            QueryWindowCalculator.calculate(days = 0, nowMillis = instant("2026-07-04T00:00:00Z"))
        }
    }

    private fun instant(value: String): Long = Instant.parse(value).toEpochMilli()
}
