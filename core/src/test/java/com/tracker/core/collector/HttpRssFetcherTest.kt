package com.tracker.core.collector

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class HttpRssFetcherTest {

    @Test
    fun fetch_retriesRetryableHttpError_andReturnsSuccessfulBody() = runBlocking {
        TestHttpServer { socket, attempt ->
            if (attempt == 1) {
                socket.respond(status = 503)
            } else {
                socket.respond(body = "<rss>ok</rss>")
            }
        }.use { server ->
            val fetcher = HttpRssFetcher(maxRetries = 1, retryDelayMs = 1)

            val body = fetcher.fetch(server.url)

            assertEquals("<rss>ok</rss>", body)
            assertEquals(2, server.attempts)
        }
    }

    @Test
    fun fetch_retriesSocketTimeout_andReturnsSuccessfulBody() = runBlocking {
        TestHttpServer { socket, attempt ->
            if (attempt == 1) {
                Thread.sleep(250)
                try {
                    socket.respond(body = "too late")
                } catch (_: IOException) {
                    // Client timed out and closed the socket, which is expected in this test.
                }
            } else {
                socket.respond(body = "<rss>after retry</rss>")
            }
        }.use { server ->
            val fetcher = HttpRssFetcher(
                readTimeoutMs = 50,
                maxRetries = 1,
                retryDelayMs = 1
            )

            val body = fetcher.fetch(server.url)

            assertEquals("<rss>after retry</rss>", body)
            assertEquals(2, server.attempts)
        }
    }

    @Test
    fun fetch_propagatesCancellationDuringRetryDelay() {
        TestHttpServer { socket, _ ->
            socket.respond(status = 503)
        }.use { server ->
            val fetcher = HttpRssFetcher(maxRetries = 3, retryDelayMs = 5_000)

            assertThrows(TimeoutCancellationException::class.java) {
                runBlocking {
                    withTimeout(250) {
                        fetcher.fetch(server.url)
                    }
                }
            }

            assertEquals(1, server.attempts)
        }
    }

    private class TestHttpServer(
        private val handler: (Socket, Int) -> Unit
    ) : AutoCloseable {
        private val closed = AtomicBoolean(false)
        private val requestCount = AtomicInteger()
        private val executor: ExecutorService = Executors.newCachedThreadPool()
        private val serverSocket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))

        init {
            executor.execute {
                while (!closed.get()) {
                    try {
                        val socket = serverSocket.accept()
                        executor.execute {
                            socket.use {
                                it.consumeRequestHeaders()
                                handler(it, requestCount.incrementAndGet())
                            }
                        }
                    } catch (_: IOException) {
                        if (!closed.get()) throw AssertionError("Test HTTP server failed")
                    }
                }
            }
        }

        val url: String
            get() = "http://127.0.0.1:${serverSocket.localPort}/rss"

        val attempts: Int
            get() = requestCount.get()

        override fun close() {
            closed.set(true)
            serverSocket.close()
            executor.shutdownNow()
        }
    }
}

private fun Socket.consumeRequestHeaders() {
    val input = getInputStream().bufferedReader()
    while (true) {
        val line = input.readLine() ?: return
        if (line.isEmpty()) return
    }
}

private fun Socket.respond(
    status: Int = 200,
    body: String = ""
) {
    val bytes = body.toByteArray(Charsets.UTF_8)
    val reason = when (status) {
        200 -> "OK"
        503 -> "Service Unavailable"
        else -> "Status"
    }
    val headers = buildString {
        append("HTTP/1.1 $status $reason\r\n")
        append("Content-Length: ${bytes.size}\r\n")
        append("Connection: close\r\n")
        append("\r\n")
    }.toByteArray(Charsets.UTF_8)
    getOutputStream().use { output ->
        output.write(headers)
        output.write(bytes)
        output.flush()
    }
}
