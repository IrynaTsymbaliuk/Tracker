package com.tracker.core

import android.content.Context
import com.tracker.core.collector.CollectorException
import com.tracker.core.config.KnownApps
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers the [Tracker] facade: the tracked-app catalogues, the builder/username plumbing,
 * the query-window guard, and the graceful-degradation behavior of each query method when
 * no permissions or Health Connect are available (the Robolectric default).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TrackerTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private fun tracker() = Tracker.Builder(context).build()

    @Test
    fun getTrackedApps_returnKnownAppCatalogues() {
        val tracker = tracker()
        assertEquals(KnownApps.reading.values.toList(), tracker.getTrackedReadingApps())
        assertEquals(KnownApps.languageLearning.values.toList(), tracker.getTrackedLanguageLearningApps())
        assertEquals(KnownApps.socialMedia.values.toList(), tracker.getTrackedSocialMediaApps())
        assertEquals(KnownApps.meditation.values.toList(), tracker.getTrackedMeditationApps())
    }

    @Test
    fun queryMovieWatching_withoutUsername_throwsIllegalState() {
        val tracker = Tracker.Builder(context).setLetterboxdUsername("initial").build()
        tracker.setLetterboxdUsername(null) // clear it again

        assertThrows(IllegalStateException::class.java) {
            runBlocking { tracker.queryMovieWatching() }
        }
    }

    @Test
    fun queryReading_withZeroDays_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { tracker().queryReading(days = 0) }
        }
    }

    @Test
    fun usageQueries_withoutPermission_throwCollectorException() {
        val tracker = tracker()
        // No PACKAGE_USAGE_STATS granted and no known apps installed -> a CollectorException
        // subtype (PermissionDenied or NoMonitorableApps) is raised for every usage-based metric.
        assertThrows(CollectorException::class.java) { runBlocking { tracker.queryReading() } }
        assertThrows(CollectorException::class.java) { runBlocking { tracker.queryLanguageLearning() } }
        assertThrows(CollectorException::class.java) { runBlocking { tracker.querySocialMedia() } }
    }

    @Test
    fun healthConnectQueries_withoutHealthConnect_returnNull() = runBlocking {
        val tracker = tracker()
        // Health Connect is unavailable under Robolectric, so these degrade to null.
        assertNull(tracker.queryStepCounting())
        assertNull(tracker.queryDistance())
        assertNull(tracker.queryExercise())
        assertNull(tracker.queryMeditation())
    }
}
