package com.tracker.core.collector

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

/**
 * Functional interface for fetching RSS feed content from URLs.
 * This abstraction allows for easy testing by mocking network calls
 * and enables reuse across different RSS-based collectors (Letterboxd, Goodreads, etc.).
 */
fun interface RssFetcher {
    /**
     * Fetches content from the given URL.
     * @param url The URL to fetch from
     * @return The content as a String
     * @throws NetworkException if the fetch fails
     */
    fun fetch(url: String): String
}

/**
 * Default implementation of RssFetcher using HttpURLConnection.
 * Can be shared across multiple collectors for consistent network behavior.
 */
class HttpRssFetcher(
    private val connectTimeoutMs: Int = 10000,
    private val readTimeoutMs: Int = 10000,
    private val userAgent: String = "TrackerApp/1.0"
) : RssFetcher {

    companion object {
        private const val TAG = "HttpRssFetcher"
    }

    override fun fetch(url: String): String {
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
            // Re-throw NetworkException as-is (already logged above)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch RSS feed from $url: ${e.message}", e)
            throw NetworkException("Failed to fetch RSS feed: ${e.message}", e)
        } finally {
            connection?.disconnect()
        }
    }
}
