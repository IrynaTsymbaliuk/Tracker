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
 * Functional interface for fetching RSS feed content from URLs.
 * This abstraction allows for easy testing by mocking network calls
 * and enables reuse across different RSS-based collectors (Letterboxd, Goodreads, etc.).
 *
 * The fetch function is suspending to allow non-blocking delays during retries
 * and to support coroutine cancellation.
 *
 * Use SAM conversion for simple test mocks:
 * ```
 * val fetcher = RssFetcher { url -> "<rss>test data</rss>" }
 * ```
 */
fun interface RssFetcher {
    /**
     * Fetches content from the given URL.
     * This is a suspending function that can be cancelled and supports non-blocking delays.
     *
     * @param url The URL to fetch from
     * @return The content as a String
     * @throws NetworkException if the fetch fails
     * @throws kotlinx.coroutines.CancellationException if the coroutine is cancelled
     */
    suspend fun fetch(url: String): String
}

/**
 * Default implementation of RssFetcher using HttpURLConnection.
 * Can be shared across multiple collectors for consistent network behavior.
 *
 * Features:
 * - Configurable timeouts and retry logic
 * - Optional network connectivity pre-checking
 * - Automatic retry for transient failures (timeouts, 5xx errors)
 * - Exponential backoff between retries (1s, 2s, 4s, ...)
 * - Non-blocking delays (coroutine-based)
 *
 * @param connectTimeoutMs Connection timeout in milliseconds (default: 10 seconds)
 * @param readTimeoutMs Read timeout in milliseconds (default: 10 seconds)
 * @param userAgent User-Agent header value (defaults to "Tracker/2.0.0 (Android XX)")
 * @param maxRetries Maximum number of retry attempts for transient failures (default: 3)
 * @param retryDelayMs Initial delay between retries in milliseconds (default: 1000ms, increases exponentially)
 * @param networkChecker Optional network connectivity checker (default: null, no pre-checking)
 *                       Pass AndroidNetworkConnectivityChecker(context) to enable pre-flight checks
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

        // HTTP status codes that should be retried
        private val RETRYABLE_STATUS_CODES = setOf(408, 429, 500, 502, 503, 504)

        /**
         * Builds the default User-Agent string using library name and version from BuildConfig.
         * Format: "LibraryName/Version (Android API_LEVEL)"
         * Example: "Tracker/2.0.0 (Android 33)"
         */
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
                    val delayTime =
                        retryDelayMs * (1 shl (attempt - 1)) // Exponential backoff: 1s, 2s, 4s
                    Log.d(
                        TAG,
                        "Retrying request (attempt $attempt/$maxRetries) after ${delayTime}ms delay"
                    )
                    delay(delayTime) // Non-blocking, cancellable delay
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
            is SocketTimeoutException -> true // Retry on timeout
            is UnknownHostException -> false // Don't retry DNS failures
            is NetworkException -> {
                // Retry for specific HTTP status codes (server errors, rate limiting)
                val statusCode = e.message?.let { msg ->
                    "HTTP error code: (\\d+)".toRegex().find(msg)?.groupValues?.get(1)
                        ?.toIntOrNull()
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
