package com.tracker.core.collector

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.tracker.core.config.AppMetadata
import com.tracker.core.permission.Permission
import com.tracker.core.permission.PermissionManager
import com.tracker.core.permission.PermissionStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Exercises the [UsageEventsCollector] foreground-session state machine on API 29+
 * (ACTIVITY_RESUMED / ACTIVITY_PAUSED), driving events through Robolectric's
 * ShadowUsageStatsManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class UsageEventsCollectorTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val permissionManager = mockk<PermissionManager>()

    private val knownApps = mapOf(
        "com.a" to AppMetadata("com.a", 0.9f),
        "com.b" to AppMetadata("com.b", 0.8f)
    )

    @Before
    fun setUp() {
        every { permissionManager.checkPermission(Permission.PACKAGE_USAGE_STATS) } returns
            PermissionStatus.GRANTED
    }

    @Test
    fun singleResumePause_producesOneSessionWithCorrectDuration() {
        installApp("com.a")
        resume("com.a", 100_000)
        pause("com.a", 220_000) // +120_000ms = 2 min

        val sessions = collect()

        assertEquals(1, sessions.size)
        assertEquals(100_000, sessions[0].startTimeMillis)
        assertEquals(220_000, sessions[0].endTimeMillis)
        assertEquals(2, sessions[0].durationMinutes)
    }

    @Test
    fun gapWithinThreshold_mergesIntoOneSession() {
        installApp("com.a")
        resume("com.a", 100_000)
        pause("com.a", 160_000)
        resume("com.a", 180_000) // 20s gap (<= 30s) -> same session
        pause("com.a", 280_000)

        val sessions = collect()

        assertEquals(1, sessions.size)
        assertEquals(100_000, sessions[0].startTimeMillis)
        assertEquals(280_000, sessions[0].endTimeMillis)
    }

    @Test
    fun gapBeyondThreshold_splitsIntoTwoSessions() {
        installApp("com.a")
        resume("com.a", 100_000)
        pause("com.a", 160_000)
        resume("com.a", 200_000) // 40s gap (> 30s) -> new session
        pause("com.a", 260_000)

        val sessions = collect().sortedBy { it.startTimeMillis }

        assertEquals(2, sessions.size)
        assertEquals(100_000L to 160_000L, sessions[0].startTimeMillis to sessions[0].endTimeMillis)
        assertEquals(200_000L to 260_000L, sessions[1].startTimeMillis to sessions[1].endTimeMillis)
    }

    @Test
    fun appAlreadyOpenAtWindowStart_infersStartAsFromMillis() {
        installApp("com.a")
        pause("com.a", 120_000) // pause with no prior resume in window

        val sessions = collect(from = 0L)

        assertEquals(1, sessions.size)
        assertEquals(0L, sessions[0].startTimeMillis) // inferred
        assertEquals(120_000L, sessions[0].endTimeMillis)
    }

    @Test
    fun appStillOpenAtWindowEnd_infersEndAsToMillis() {
        installApp("com.a")
        resume("com.a", 100_000) // no matching pause

        val sessions = collect(to = 500_000L)

        assertEquals(1, sessions.size)
        assertEquals(100_000L, sessions[0].startTimeMillis)
        assertEquals(500_000L, sessions[0].endTimeMillis) // inferred
    }

    @Test
    fun interleavedApps_areTrackedIndependently() {
        installApp("com.a")
        installApp("com.b")
        resume("com.a", 100_000)
        resume("com.b", 150_000)
        pause("com.a", 220_000) // 2 min
        pause("com.b", 390_000) // 4 min

        val byPackage = collect().associateBy {
            UsageStatsMetadata.fromMap(it.metadata)?.packageName
        }

        assertEquals(2, byPackage.size)
        assertEquals(2, byPackage["com.a"]?.durationMinutes)
        assertEquals(4, byPackage["com.b"]?.durationMinutes)
    }

    @Test
    fun unknownPackage_isIgnored() {
        installApp("com.a")
        resume("com.unknown", 100_000)
        pause("com.unknown", 400_000)
        resume("com.a", 100_000)
        pause("com.a", 220_000)

        val sessions = collect()

        assertEquals(1, sessions.size)
        assertEquals("com.a", UsageStatsMetadata.fromMap(sessions[0].metadata)?.packageName)
    }

    @Test
    fun knownButNotInstalledPackage_isIgnored() {
        installApp("com.a") // com.b is known but NOT installed
        resume("com.a", 100_000)
        pause("com.a", 220_000)
        resume("com.b", 100_000)
        pause("com.b", 400_000)

        val sessions = collect()

        assertEquals(1, sessions.size)
        assertEquals("com.a", UsageStatsMetadata.fromMap(sessions[0].metadata)?.packageName)
    }

    @Test
    fun subMinuteSession_isKeptWithZeroDuration() {
        installApp("com.a")
        resume("com.a", 100_000)
        pause("com.a", 130_000) // 30s -> 0 minutes

        val sessions = collect()

        assertEquals(1, sessions.size)
        assertEquals(0, sessions[0].durationMinutes)
    }

    @Test
    fun permissionNotGranted_throwsPermissionDeniedException() {
        every { permissionManager.checkPermission(Permission.PACKAGE_USAGE_STATS) } returns
            PermissionStatus.MISSING
        installApp("com.a")

        assertThrows(PermissionDeniedException::class.java) { collect() }
    }

    @Test
    fun noKnownAppsInstalled_throwsNoMonitorableAppsException() {
        // Nothing installed.
        assertThrows(NoMonitorableAppsException::class.java) { collect() }
    }

    @Test
    fun usageStatsManagerUnavailable_throwsSystemServiceUnavailableException() {
        installApp("com.a")
        val nullServiceContext = object : ContextWrapper(context) {
            override fun getSystemService(name: String): Any? =
                if (name == Context.USAGE_STATS_SERVICE) null else super.getSystemService(name)
        }

        assertThrows(SystemServiceUnavailableException::class.java) {
            UsageEventsCollector(nullServiceContext, permissionManager)
                .collect(0L, 1_000_000L, knownApps)
        }
    }

    // --- helpers ---

    private fun collect(from: Long = 0L, to: Long = 1_000_000L) =
        UsageEventsCollector(context, permissionManager).collect(from, to, knownApps)

    private fun installApp(pkg: String) {
        val info = PackageInfo().apply {
            packageName = pkg
            applicationInfo = ApplicationInfo().apply {
                packageName = pkg
                name = pkg
            }
        }
        shadowOf(context.packageManager).installPackage(info)
    }

    private fun resume(pkg: String, ts: Long) =
        shadowOf(usageStatsManager).addEvent(pkg, ts, UsageEvents.Event.ACTIVITY_RESUMED)

    private fun pause(pkg: String, ts: Long) =
        shadowOf(usageStatsManager).addEvent(pkg, ts, UsageEvents.Event.ACTIVITY_PAUSED)
}
