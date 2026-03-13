package com.tracker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for TimeProvider interface.
 * TimeProvider is internal and used to make time-based queries testable.
 */
class TimeProviderTest {

    @Test
    fun `production TimeProvider returns current system time`() {
        val provider = TimeProvider { System.currentTimeMillis() }
        val before = System.currentTimeMillis()
        val actual = provider.now()
        val after = System.currentTimeMillis()

        // Verify the time is close to current system time (within 100ms)
        assertTrue("TimeProvider should return current system time", actual in before..after)
    }

    @Test
    fun `fixed TimeProvider returns exact value`() {
        val fixedTime = 1700000000000L // Nov 14, 2023
        val provider = TimeProvider { fixedTime }

        assertEquals(fixedTime, provider.now())
        assertEquals(fixedTime, provider.now()) // Should be stable
    }

    @Test
    fun `from and to computed from same now call have no drift`() {
        val fixedTime = 1700000000000L
        val provider = TimeProvider { fixedTime }

        // Simulate what Tracker.queryAsync() will do internally
        val now = provider.now()
        val fromMillis = now - 86_400_000L
        val toMillis = now

        assertEquals(1700000000000L - 86_400_000L, fromMillis)
        assertEquals(1700000000000L, toMillis)
        assertEquals(86_400_000L, toMillis - fromMillis) // Exactly 24 hours
    }

    @Test
    fun `TimeProvider can be used in multiple calls`() {
        var callCount = 0
        val provider = TimeProvider {
            callCount++
            1700000000000L + (callCount * 1000) // Increment by 1 second each call
        }

        assertEquals(1700000001000L, provider.now())
        assertEquals(1700000002000L, provider.now())
        assertEquals(1700000003000L, provider.now())
    }

    @Test
    fun `fixed TimeProvider makes tests deterministic`() {
        val fixedTime = 1646092800000L // March 1, 2022 00:00:00 UTC
        val provider = TimeProvider { fixedTime }

        // First query
        val now1 = provider.now()
        val from1 = now1 - 86_400_000L
        val to1 = now1

        // Second query (would normally be different time, but fixed provider returns same)
        val now2 = provider.now()
        val from2 = now2 - 86_400_000L
        val to2 = now2

        assertEquals(from1, from2)
        assertEquals(to1, to2)
    }
}
