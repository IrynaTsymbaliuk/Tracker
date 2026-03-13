package com.tracker.core

import android.content.Context
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
 */
@RunWith(RobolectricTestRunner::class)
class TrackerBuilderValidationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    /**
     * Test: Building without any configuration succeeds.
     *
     * Since metrics are no longer required upfront, the builder
     * can be built with just a context.
     *
     * Given: A Builder instance without any configuration
     * When: build() is called
     * Then: A valid Tracker instance should be created with defaults
     */
    @Test
    fun `build without configuration succeeds with defaults`() {
        val tracker = Tracker.Builder(context)
            .build()

        assertNotNull(tracker)
        assertEquals(0.50f, tracker.getMinConfidence(), 0.001f) // Default confidence
    }

    /**
     * Test: Builder accepts custom minConfidence.
     *
     * Given: A Builder instance with custom minConfidence
     * When: build() is called
     * Then: A valid Tracker instance should be created
     */
    @Test
    fun `build with custom minConfidence succeeds`() {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.75f)
            .build()

        assertNotNull(tracker)
        assertEquals(0.75f, tracker.getMinConfidence(), 0.001f)
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
        // Act: Chain builder methods together
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.75f)
            .build()

        // Assert: Verify all configurations were applied
        assertNotNull(tracker)
        assertEquals(0.75f, tracker.getMinConfidence(), 0.001f)
    }

    /**
     * Test: minConfidence validation - rejects values below 0.
     *
     * Given: A Builder with minConfidence < 0
     * When: setMinConfidence() is called
     * Then: IllegalArgumentException should be thrown
     */
    @Test(expected = IllegalArgumentException::class)
    fun `minConfidence below 0 throws exception`() {
        Tracker.Builder(context)
            .setMinConfidence(-0.1f)
            .build()
    }

    /**
     * Test: minConfidence validation - rejects values above 1.
     *
     * Given: A Builder with minConfidence > 1
     * When: setMinConfidence() is called
     * Then: IllegalArgumentException should be thrown
     */
    @Test(expected = IllegalArgumentException::class)
    fun `minConfidence above 1 throws exception`() {
        Tracker.Builder(context)
            .setMinConfidence(1.1f)
            .build()
    }

    /**
     * Test: minConfidence validation - accepts 0.
     *
     * Given: A Builder with minConfidence = 0
     * When: build() is called
     * Then: A valid Tracker instance should be created
     */
    @Test
    fun `minConfidence 0 is valid`() {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.0f)
            .build()

        assertNotNull(tracker)
        assertEquals(0.0f, tracker.getMinConfidence(), 0.001f)
    }

    /**
     * Test: minConfidence validation - accepts 1.
     *
     * Given: A Builder with minConfidence = 1
     * When: build() is called
     * Then: A valid Tracker instance should be created
     */
    @Test
    fun `minConfidence 1 is valid`() {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(1.0f)
            .build()

        assertNotNull(tracker)
        assertEquals(1.0f, tracker.getMinConfidence(), 0.001f)
    }
}
