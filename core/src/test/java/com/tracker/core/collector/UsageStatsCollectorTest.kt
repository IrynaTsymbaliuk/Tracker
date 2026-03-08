package com.tracker.core.collector

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.tracker.core.permission.Permission
import com.tracker.core.permission.PermissionManager
import com.tracker.core.permission.PermissionStatus
import com.tracker.core.types.DataSource
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Test suite for UsageStatsCollector.
 *
 * Tests Result<T>-based collection with clear distinction between:
 * - Failures: can't collect (permission denied, service unavailable, no apps, errors)
 * - Success with empty: collected successfully but no usage found
 * - Success with data: collected successfully with evidence
 */
class UsageStatsCollectorTest {

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ============================================================
    // Permission Handling Tests
    // ============================================================

    @Test
    fun `collect returns failure when PACKAGE_USAGE_STATS permission not granted`() = runTest {
        // Arrange
        val collector = createCollector(permissionGranted = false)

        // Act
        val result = collector.collect(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PermissionDeniedException)
    }

    @Test
    fun `collect proceeds when PACKAGE_USAGE_STATS permission granted`() = runTest {
        // Arrange
        val collector = createCollector(
            permissionGranted = true,
            installedApps = listOf("com.duolingo")
        )

        // Act
        val result = collector.collect(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        assertTrue(result.isSuccess)
    }

    // ============================================================
    // Data Collection Tests
    // ============================================================

    @Test
    fun `collect queries UsageStatsManager for correct time range`() = runTest {
        // Arrange
        val fromMillis = 1000L
        val toMillis = 5000L
        val mockUsageStatsManager = mockk<UsageStatsManager>()
        every { mockUsageStatsManager.queryUsageStats(any(), any(), any()) } returns emptyList()

        val collector = createCollector(
            permissionGranted = true,
            installedApps = listOf("com.duolingo"),
            usageStatsManager = mockUsageStatsManager
        )

        // Act
        collector.collect(fromMillis = fromMillis, toMillis = toMillis)

        // Assert
        verify { mockUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, fromMillis, toMillis) }
    }

    @Test
    fun `collect returns failure when UsageStatsManager is null`() = runTest {
        // Arrange
        val collector = createCollector(
            permissionGranted = true,
            usageStatsManager = null
        )

        // Act
        val result = collector.collect(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SystemServiceUnavailableException)
    }

    @Test
    fun `collect returns failure when no language learning apps installed`() = runTest {
        // Arrange
        val collector = createCollector(
            permissionGranted = true,
            installedApps = emptyList()
        )

        // Act
        val result = collector.collect(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoMonitorableAppsException)
    }

    @Test
    fun `collect returns success with empty list when queryUsageStats returns null`() = runTest {
        // Arrange
        val mockUsageStatsManager = mockk<UsageStatsManager>()
        every { mockUsageStatsManager.queryUsageStats(any(), any(), any()) } returns null

        val collector = createCollector(
            permissionGranted = true,
            installedApps = listOf("com.duolingo"),
            usageStatsManager = mockUsageStatsManager
        )

        // Act
        val result = collector.collect(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `collect creates Evidence for apps exceeding minimum session duration`() = runTest {
        // Arrange - Duolingo min is 5 minutes, using 30 minutes
        val usageStats = createUsageStats("com.duolingo", minutes = 30)
        val collector = createCollector(
            permissionGranted = true,
            installedApps = listOf("com.duolingo"),
            usageStatsList = listOf(usageStats)
        )

        // Act
        val result = collector.collect(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `collect skips apps below minimum session duration`() = runTest {
        // Arrange - Duolingo min is 5 minutes, using 3 minutes
        val usageStats = createUsageStats("com.duolingo", minutes = 3)
        val collector = createCollector(
            permissionGranted = true,
            installedApps = listOf("com.duolingo"),
            usageStatsList = listOf(usageStats)
        )

        // Act
        val result = collector.collect(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `collect uses INTERVAL_DAILY for query`() = runTest {
        // Arrange
        val mockUsageStatsManager = mockk<UsageStatsManager>()
        every { mockUsageStatsManager.queryUsageStats(any(), any(), any()) } returns emptyList()

        val collector = createCollector(
            permissionGranted = true,
            installedApps = listOf("com.duolingo"),
            usageStatsManager = mockUsageStatsManager
        )

        // Act
        collector.collect(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        verify { mockUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, any(), any()) }
    }

    // ============================================================
    // Evidence Creation Tests
    // ============================================================

    @Test
    fun `evidence has correct source USAGE_STATS`() = runTest {
        // Arrange
        val collector = createCollectorWithEvidence()

        // Act
        val evidence = collector.collect(1000L, 2000L).getOrNull()!![0]

        // Assert
        assertEquals(DataSource.USAGE_STATS, evidence.source)
    }

    @Test
    fun `evidence timestamp is firstTimeStamp from UsageStats`() = runTest {
        // Arrange
        val usageStats = createUsageStats("com.duolingo", firstTimeStamp = 12345L)
        val collector = createCollector(
            permissionGranted = true,
            installedApps = listOf("com.duolingo"),
            usageStatsList = listOf(usageStats)
        )

        // Act
        val evidence = collector.collect(1000L, 2000L).getOrNull()!![0]

        // Assert
        assertEquals(12345L, evidence.timestampMillis)
    }

    @Test
    fun `evidence confidence is app's confidenceMultiplier`() = runTest {
        // Arrange
        val collector = createCollectorWithEvidence()

        // Act
        val evidence = collector.collect(1000L, 2000L).getOrNull()!![0]

        // Assert
        assertEquals(0.80f, evidence.confidence, 0.001f) // Duolingo = 0.80
    }

    @Test
    fun `evidence duration in minutes is converted from milliseconds correctly`() = runTest {
        // Arrange
        val usageStats = createUsageStats("com.duolingo", minutes = 45)
        val collector = createCollector(
            permissionGranted = true,
            installedApps = listOf("com.duolingo"),
            usageStatsList = listOf(usageStats)
        )

        // Act
        val evidence = collector.collect(1000L, 2000L).getOrNull()!![0]

        // Assert
        assertEquals(45, evidence.durationMinutes)
    }

    @Test
    fun `evidence startTimeMillis is firstTimeStamp`() = runTest {
        // Arrange
        val usageStats = createUsageStats("com.duolingo", firstTimeStamp = 98765L)
        val collector = createCollector(
            permissionGranted = true,
            installedApps = listOf("com.duolingo"),
            usageStatsList = listOf(usageStats)
        )

        // Act
        val evidence = collector.collect(1000L, 2000L).getOrNull()!![0]

        // Assert
        assertEquals(98765L, evidence.startTimeMillis)
    }

    @Test
    fun `evidence endTimeMillis is lastTimeStamp`() = runTest {
        // Arrange
        val usageStats = createUsageStats("com.duolingo", lastTimeStamp = 56789L)
        val collector = createCollector(
            permissionGranted = true,
            installedApps = listOf("com.duolingo"),
            usageStatsList = listOf(usageStats)
        )

        // Act
        val evidence = collector.collect(1000L, 2000L).getOrNull()!![0]

        // Assert
        assertEquals(56789L, evidence.endTimeMillis)
    }

    @Test
    fun `evidence metadata includes packageName`() = runTest {
        // Arrange
        val collector = createCollectorWithEvidence()

        // Act
        val evidence = collector.collect(1000L, 2000L).getOrNull()!![0]

        // Assert
        assertEquals("com.duolingo", evidence.metadata["packageName"])
    }

    @Test
    fun `evidence metadata includes appName human-readable`() = runTest {
        // Arrange
        val collector = createCollectorWithEvidence()

        // Act
        val evidence = collector.collect(1000L, 2000L).getOrNull()!![0]

        // Assert
        assertEquals("Duolingo", evidence.metadata["appName"])
    }

    @Test
    fun `app name falls back to packageName when lookup fails`() = runTest {
        // Arrange
        val mockPackageManager = mockk<PackageManager>()
        val duolingoInfo = ApplicationInfo().apply { packageName = "com.duolingo" }
        every { mockPackageManager.getInstalledApplications(any<Int>()) } returns listOf<ApplicationInfo>(duolingoInfo)
        every { mockPackageManager.getApplicationInfo(any<String>(), any<Int>()) } throws PackageManager.NameNotFoundException()

        val usageStats = createUsageStats("com.duolingo", minutes = 30)
        val collector = createCollector(
            permissionGranted = true,
            packageManager = mockPackageManager,
            usageStatsList = listOf(usageStats)
        )

        // Act
        val evidence = collector.collect(1000L, 2000L).getOrNull()!![0]

        // Assert
        assertEquals("com.duolingo", evidence.metadata["appName"])
    }

    // ============================================================
    // Edge Cases Tests
    // ============================================================

    @Test
    fun `collect handles multiple language learning apps correctly`() = runTest {
        // Arrange
        val mockPackageManager = mockk<PackageManager>()
        val duolingoInfo = ApplicationInfo().apply { packageName = "com.duolingo" }
        val ankiInfo = ApplicationInfo().apply { packageName = "com.ichi2.anki" }
        val lingodeerInfo = ApplicationInfo().apply { packageName = "com.lingodeer" }

        every { mockPackageManager.getInstalledApplications(any<Int>()) } returns listOf<ApplicationInfo>(
            duolingoInfo, ankiInfo, lingodeerInfo
        )
        every { mockPackageManager.getApplicationInfo(any<String>(), any<Int>()) } answers {
            val pkg: String = firstArg()
            ApplicationInfo().apply { packageName = pkg }
        }
        every { mockPackageManager.getApplicationLabel(any()) } answers {
            when ((firstArg() as ApplicationInfo).packageName) {
                "com.duolingo" -> "Duolingo"
                "com.ichi2.anki" -> "Anki"
                "com.lingodeer" -> "LingoDeer"
                else -> "Unknown"
            }
        }

        val usageStatsList = listOf(
            createUsageStats("com.duolingo", minutes = 20),
            createUsageStats("com.ichi2.anki", minutes = 15),
            createUsageStats("com.lingodeer", minutes = 25)
        )

        val collector = createCollector(
            permissionGranted = true,
            packageManager = mockPackageManager,
            usageStatsList = usageStatsList
        )

        // Act
        val result = collector.collect(1000L, 5000L)

        // Assert
        assertTrue(result.isSuccess)
        val evidence = result.getOrNull()!!
        assertEquals(3, evidence.size)
        assertTrue(evidence.any { it.metadata["packageName"] == "com.duolingo" })
        assertTrue(evidence.any { it.metadata["packageName"] == "com.ichi2.anki" })
        assertTrue(evidence.any { it.metadata["packageName"] == "com.lingodeer" })
    }

    @Test
    fun `collect handles apps with zero foreground time`() = runTest {
        // Arrange
        val usageStats = createUsageStats("com.duolingo", minutes = 0)
        val collector = createCollector(
            permissionGranted = true,
            installedApps = listOf("com.duolingo"),
            usageStatsList = listOf(usageStats)
        )

        // Act
        val result = collector.collect(1000L, 2000L)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `collect handles PackageManager exceptions gracefully`() = runTest {
        // Arrange
        val mockPackageManager = mockk<PackageManager>()
        every { mockPackageManager.getInstalledApplications(any<Int>()) } throws RuntimeException("Test exception")

        val collector = createCollector(
            permissionGranted = true,
            packageManager = mockPackageManager
        )

        // Act
        val result = collector.collect(1000L, 2000L)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PackageManagerException)
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    /**
     * Create a mock UsageStats for a given package with specified duration.
     */
    private fun createUsageStats(
        packageName: String,
        minutes: Int = 30,
        firstTimeStamp: Long = 1000L,
        lastTimeStamp: Long = 2000L
    ): UsageStats {
        val usageStats = mockk<UsageStats>(relaxed = true)
        every { usageStats.packageName } returns packageName
        every { usageStats.totalTimeInForeground } returns TimeUnit.MINUTES.toMillis(minutes.toLong())
        every { usageStats.firstTimeStamp } returns firstTimeStamp
        every { usageStats.lastTimeStamp } returns lastTimeStamp
        return usageStats
    }

    /**
     * Create a collector with configurable mocks.
     */
    private fun createCollector(
        permissionGranted: Boolean,
        installedApps: List<String>? = null,
        usageStatsList: List<UsageStats>? = null,
        usageStatsManager: UsageStatsManager? = mockk(relaxed = true),
        packageManager: PackageManager? = null
    ): UsageStatsCollector {
        val mockPermissionManager = mockk<PermissionManager>()
        every { mockPermissionManager.checkPermission(Permission.PACKAGE_USAGE_STATS) } returns
            if (permissionGranted) PermissionStatus.GRANTED else PermissionStatus.MISSING

        val mockPackageManager = packageManager ?: mockk<PackageManager>().also { pm ->
            val appInfoList = (installedApps ?: emptyList()).map { pkg ->
                ApplicationInfo().apply { packageName = pkg }
            }
            every { pm.getInstalledApplications(any<Int>()) } returns appInfoList
            val duolingoAppInfo = ApplicationInfo().apply { packageName = "com.duolingo" }
            every { pm.getApplicationInfo("com.duolingo", 0) } returns duolingoAppInfo
            every { pm.getApplicationLabel(any<ApplicationInfo>()) } returns "Duolingo"
        }

        val mockUsageStatsManager = usageStatsManager?.apply {
            if (usageStatsList != null) {
                every { queryUsageStats(any(), any(), any()) } returns usageStatsList
            }
        }

        val mockContext = mockk<Context>()
        every { mockContext.getSystemService(Context.USAGE_STATS_SERVICE) } returns mockUsageStatsManager
        every { mockContext.packageManager } returns mockPackageManager

        return UsageStatsCollector(mockContext, mockPermissionManager)
    }

    /**
     * Create a collector that will return one evidence (for testing evidence fields).
     */
    private fun createCollectorWithEvidence(): UsageStatsCollector {
        return createCollector(
            permissionGranted = true,
            installedApps = listOf("com.duolingo"),
            usageStatsList = listOf(createUsageStats("com.duolingo", minutes = 30))
        )
    }
}
