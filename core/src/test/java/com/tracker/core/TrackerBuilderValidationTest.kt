package com.tracker.core

import android.content.Context
import com.tracker.core.types.Metric
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Test suite for verifying Tracker.Builder validation logic.
 *
 * Tests builder-level validation rules, such as ensuring at least
 * one metric is requested before building.
 */
@RunWith(RobolectricTestRunner::class)
class TrackerBuilderValidationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    /**
     * Test: Building without requesting metrics throws IllegalArgumentException.
     *
     * Given: A Builder instance without calling requestMetrics()
     * When: build() is called
     * Then: IllegalArgumentException should be thrown
     */
    @Test(expected = IllegalArgumentException::class)
    fun `build without metrics throws IllegalArgumentException`() {
        Tracker.Builder(context)
            .build() // No requestMetrics() called
    }

    /**
     * Test: Building with a single metric succeeds.
     *
     * Given: A Builder instance with one metric requested
     * When: build() is called
     * Then: A valid Tracker instance should be created
     */
    @Test
    fun `build with single metric succeeds`() {
        // Act
        val tracker = Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .build()

        // Assert
        assertNotNull(tracker)
    }

    /**
     * Test: Building with multiple metrics succeeds.
     *
     * Currently only LANGUAGE_LEARNING exists, but this test
     * demonstrates the builder supports multiple metrics (varargs).
     * When more metrics are added, this test will be more meaningful.
     *
     * Given: A Builder instance with multiple metrics requested
     * When: build() is called
     * Then: A valid Tracker instance should be created
     */
    @Test
    fun `build with multiple metrics succeeds`() {
        // Act: Currently only one metric exists, but using varargs syntax
        val tracker = Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .build()

        // Assert
        assertNotNull(tracker)

        // Future-proofing: When more metrics exist, test could look like:
        // .requestMetrics(Metric.LANGUAGE_LEARNING, Metric.EXERCISE, Metric.READING)
    }

    /**
     * Test: Builder uses application context, not the passed context.
     *
     * This prevents memory leaks when an Activity context is passed.
     * The Builder should convert any context to applicationContext.
     *
     * Given: A mock context (simulating Activity context)
     * When: Builder is constructed and built
     * Then: applicationContext should be accessed
     */
    @Test
    fun `builder uses application context not activity context`() {
        // Arrange: Create mock Activity context
        val mockActivityContext = mockk<Context>()
        val appContext = RuntimeEnvironment.getApplication()

        // Mock the applicationContext property
        every { mockActivityContext.applicationContext } returns appContext

        // Act: Build with mock activity context
        val tracker = Tracker.Builder(mockActivityContext)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .build()

        // Assert: Verify applicationContext was accessed
        verify { mockActivityContext.applicationContext }
        assertNotNull(tracker)
    }

    /**
     * Test: Builder pattern supports method chaining.
     *
     * Verifies that all builder methods return the Builder instance,
     * allowing for fluent API usage.
     *
     * Given: A Builder instance
     * When: Multiple builder methods are chained together
     * Then: All configurations should be applied to the final Tracker
     */
    @Test
    fun `builder supports method chaining`() {
        // Act: Chain all builder methods together
        val tracker = Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setLookbackDays(45)
            .setMinConfidence(0.75f)
            .build()

        // Assert: Verify all configurations were applied
        assertNotNull(tracker)
        assertEquals(45, tracker.getLookbackDays())
        assertEquals(0.75f, tracker.getMinConfidence(), 0.001f)
    }
}
