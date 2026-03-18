package com.tracker.core.collector

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/** Checks network connectivity. */
fun interface NetworkConnectivityChecker {
    /** @return true if network is available */
    fun isNetworkAvailable(): Boolean
}

/**
 * Checks network connectivity using ConnectivityManager.
 *
 * Requires `android.permission.ACCESS_NETWORK_STATE`.
 *
 * - API 23+: Checks for validated internet connectivity
 * - API 21-22: Always returns true (relies on retry logic instead)
 */
class AndroidNetworkConnectivityChecker(private val context: Context) : NetworkConnectivityChecker {
    override fun isNetworkAvailable(): Boolean {
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
