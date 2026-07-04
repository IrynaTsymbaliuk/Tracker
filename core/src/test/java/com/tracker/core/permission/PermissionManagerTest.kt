package com.tracker.core.permission

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PermissionManagerTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    @Test
    fun checkPermission_returnsGranted_whenUsageStatsOpAllowed() {
        shadowOf(appOps).setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
            AppOpsManager.MODE_ALLOWED
        )

        val status = PermissionManager(context).checkPermission(Permission.PACKAGE_USAGE_STATS)

        assertEquals(PermissionStatus.GRANTED, status)
    }

    @Test
    fun checkPermission_returnsMissing_whenUsageStatsOpNotAllowed() {
        shadowOf(appOps).setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
            AppOpsManager.MODE_IGNORED
        )

        val status = PermissionManager(context).checkPermission(Permission.PACKAGE_USAGE_STATS)

        assertEquals(PermissionStatus.MISSING, status)
    }
}
