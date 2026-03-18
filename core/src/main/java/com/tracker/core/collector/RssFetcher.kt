package com.tracker.core.collector

import android.os.Build
import android.util.Log
import com.tracker.core.BuildConfig
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlin.collections.contains

/**
 * Fetches RSS feed content from URLs.
 *
 * Example:
 * ```
 * val fetcher = RssFetcher { url -> "<rss>test data</rss>" }
 * ```
 */
fun interface RssFetcher {
    /**
     * Fetches content from the given URL.
     *
     * @throws NetworkException if the fetch fails
     */
    suspend fun fetch(url: String): String
}

/**
 * Fetches RSS feeds using HttpURLConnection with retry logic.
 *
 * Retries transient failures (timeouts, 5xx errors) with exponential backoff.
 *
 * @param connectTimeoutMs Connection timeout (default: 10000)
 * @param readTimeoutMs Read timeout (default: 10000)
 * @param userAgent User-Agent header (default: "Tracker/2.0.0 (Android XX)")
 * @param maxRetries Maximum retry attempts (default: 3)
 * @param retryDelayMs Initial retry delay, increases exponentially (default: 1000)
 * @param networkChecker Optional pre-flight connectivity check (default: null)
 */
class HttpRssFetcher(
    private val connectTimeoutMs: Int = 10000,
    private val readTimeoutMs: Int = 10000,
    private val userAgent: String = defaultUserAgent(),
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 1000,
    private val networkChecker: NetworkConnectivityChecker? = null
) : RssFetcher {

    companion object {
        private const val TAG = "HttpRssFetcher"
        private val RETRYABLE_STATUS_CODES = setOf(408, 429, 500, 502, 503, 504)

        private fun defaultUserAgent(): String {
            return "${BuildConfig.LIBRARY_NAME}/${BuildConfig.LIBRARY_VERSION} (Android ${Build.VERSION.SDK_INT})"
        }
    }

    override suspend fun fetch(url: String): String {
        checkNetworkConnectivity()

        var lastException: Exception? = null
        var attempt = 0

        while (attempt <= maxRetries) {
            try {
                if (attempt > 0) {
                    val delayTime = retryDelayMs * (1 shl (attempt - 1)) // Exponential backoff
                    Log.d(TAG, "Retrying request (attempt $attempt/$maxRetries) after ${delayTime}ms delay")
                    delay(delayTime)
                }

                return performFetch(url)
            } catch (e: Exception) {
                lastException = e

                val shouldRetry = getShouldRetry(lastException)

                if (!shouldRetry || attempt >= maxRetries) {
                    Log.e(TAG, "Failed to fetch RSS feed after ${attempt + 1} attempts", e)
                    throw when (e) {
                        is NetworkException -> e
                        else -> NetworkException("Failed to fetch RSS feed: ${e.message}", e)
                    }
                }

                Log.w(TAG, "Transient error on attempt ${attempt + 1}: ${e.message}")
                attempt++
            }
        }

        throw NetworkException("Failed to fetch RSS feed: ${lastException?.message}", lastException)
    }

    private fun checkNetworkConnectivity() {
        if (networkChecker != null && !networkChecker.isNetworkAvailable()) {
            Log.e(TAG, "No network connectivity available")
            throw NetworkException("No network connectivity available")
        }
    }

    private fun getShouldRetry(e: Exception): Boolean {
        return when (e) {
            is SocketTimeoutException -> true
            is UnknownHostException -> false // DNS failures not retryable
            is NetworkException -> {
                val statusCode = e.message?.let { msg ->
                    "HTTP error code: (\\d+)".toRegex().find(msg)?.groupValues?.get(1)?.toIntOrNull()
                }
                statusCode in RETRYABLE_STATUS_CODES
            }
            else -> false
        }
    }

    private fun performFetch(url: String): String {
        var connection: HttpURLConnection? = null
        try {
            val urlObj = URL(url)
            connection = urlObj.openConnection() as HttpURLConnection
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", userAgent)

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP error while fetching RSS feed: $responseCode from $url")
                throw NetworkException("HTTP error code: $responseCode")
            }

            return connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: NetworkException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch RSS feed from $url: ${e.message}", e)
            throw NetworkException("Failed to fetch RSS feed: ${e.message}", e)
        } finally {
            connection?.disconnect()
        }
    }
}
