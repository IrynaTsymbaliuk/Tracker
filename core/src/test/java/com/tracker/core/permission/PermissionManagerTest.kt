package com.tracker.core.permission

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Test suite for PermissionManager.
 *
 * Tests permission checking behavior for PACKAGE_USAGE_STATS.
 * Focuses on actual behavior, not implementation details.
 */
class PermissionManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockAppOpsManager: AppOpsManager

    @Before
    fun setUp() {
        mockContext = mockk()
        mockAppOpsManager = mockk()

        // Mock static Process.myUid()
        mockkStatic(Process::class)
        every { Process.myUid() } returns 12345

        every { mockContext.packageName } returns "com.test.app"
    }

    @After
    fun tearDown() {
        unmockkStatic(Process::class)
    }

    @Test
    fun `checkPermission returns GRANTED when AppOpsManager allows usage stats`() {
        // Arrange
        every { mockContext.getSystemService(Context.APP_OPS_SERVICE) } returns mockAppOpsManager
        every {
            mockAppOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                any(),
                any()
            )
        } returns AppOpsManager.MODE_ALLOWED

        val permissionManager = PermissionManager(mockContext)

        // Act
        val status = permissionManager.checkPermission(Permission.PACKAGE_USAGE_STATS)

        // Assert
        assertEquals(PermissionStatus.GRANTED, status)
    }

    @Test
    fun `checkPermission returns MISSING when AppOpsManager denies usage stats`() {
        // Arrange
        every { mockContext.getSystemService(Context.APP_OPS_SERVICE) } returns mockAppOpsManager
        every {
            mockAppOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                any(),
                any()
            )
        } returns AppOpsManager.MODE_IGNORED

        val permissionManager = PermissionManager(mockContext)

        // Act
        val status = permissionManager.checkPermission(Permission.PACKAGE_USAGE_STATS)

        // Assert
        assertEquals(PermissionStatus.MISSING, status)
    }

    @Test
    fun `checkPermission returns MISSING when AppOpsManager service unavailable`() {
        // Arrange
        every { mockContext.getSystemService(Context.APP_OPS_SERVICE) } returns null

        val permissionManager = PermissionManager(mockContext)

        // Act
        val status = permissionManager.checkPermission(Permission.PACKAGE_USAGE_STATS)

        // Assert
        assertEquals(PermissionStatus.MISSING, status)
    }
}
