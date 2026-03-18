package com.tracker.core.collector

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Interface for checking network connectivity.
 * Allows for easy testing and dependency injection.
 */
interface NetworkConnectivityChecker {
    /**
     * Checks if network is available.
     * @return true if network is available, false otherwise
     */
    fun isNetworkAvailable(): Boolean
}

/**
 * Default implementation of NetworkConnectivityChecker using ConnectivityManager.
 *
 * Requires permission: `android.permission.ACCESS_NETWORK_STATE`
 *
 * **Behavior:**
 * - API 23+: Checks for validated internet connectivity (actual working internet)
 * - API 21-22: Always returns true (connectivity check is unreliable on these versions)
 *
 * On older API levels, the library relies on retry logic to handle network failures
 * rather than pre-checking connectivity, as the old API cannot reliably determine
 * if internet is actually working (only if device is connected to a network).
 *
 * @param context Android context for accessing system services
 */
class AndroidNetworkConnectivityChecker(private val context: Context) : NetworkConnectivityChecker {
    override fun isNetworkAvailable(): Boolean {
        // Only perform validated connectivity check on API 23+
        // On older versions, let the fetch fail naturally with retry logic
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
