package io.github.neeschal.rodinessential.backend

import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Authenticated local transports to `rodin_essentiald`, ported from the
 * native host (`runtime/host-rust/src/backend_bridge.rs`).
 *
 * 1. Direct abstract Unix socket `rodin_essentiald_v15`; the app accepts the
 *    server only when `SO_PEERCRED` reports UID 0.
 * 2. Privileged loopback `127.0.0.1:732`, used only after the app proves it
 *    cannot bind the neighbouring low port `:731` from its own SELinux/UID
 *    sandbox. Any ambiguous probe result fails closed.
 * 3. Daemon-initiated reverse socket `rodin_essential_app_v15`; this process
 *    binds, the daemon connects and must present UID 0.
 *
 * Every command uses a fresh connection and the daemon closes the stream
 * after responding, so reads run to EOF exactly like the native bridge.
 */
object RodinDaemonTransport {
    const val RESPONSE_TIMEOUT_MS = 3_000
    const val PALETTE_TIMEOUT_MS = 75_000
    const val APP_CONTROLS_TIMEOUT_MS = 20_000
    const val APP_CONTROLS_READ_LIMIT = 786_432

    private const val SOCKET_NAME = "rodin_essentiald_v15"
    private const val REVERSE_SOCKET_NAME = "rodin_essential_app_v15"
    private const val LOOPBACK_PORT = 732
    private const val LOOPBACK_PRIVILEGE_PROBE_PORT = 731

    /** 0 unknown, 1 direct, 2 reverse, 3 loopback. */
    @Volatile
    private var ipcTransport = 0

    private class Connection(
        private val closeable: Closeable,
        val input: InputStream,
        val output: OutputStream,
    ) {
        fun setTimeout(milliseconds: Int) {
            when (closeable) {
                is LocalSocket -> closeable.soTimeout = milliseconds
                is Socket -> closeable.soTimeout = milliseconds
            }
        }

        fun close() {
            try {
                closeable.close()
            } catch (_: IOException) {
            }
        }
    }

    @Throws(IOException::class)
    fun request(command: String, timeoutMs: Int = RESPONSE_TIMEOUT_MS): String {
        val connection = connect(timeoutMs)
        try {
            connection.setTimeout(timeoutMs)
            connection.output.write(command.toByteArray(StandardCharsets.UTF_8))
            connection.output.flush()
            val response = String(readAll(connection.input, 0), StandardCharsets.UTF_8).trim()
            if (response.startsWith("OK ")) return response.removePrefix("OK ")
            throw IOException(response)
        } finally {
            connection.close()
        }
    }

    /**
     * Bounded framed exchange (`@<len>\n<command>`) used by Per-App Controls
     * and the memory/storage subsystem clocks only.
     */
    @Throws(IOException::class)
    fun framedRequest(command: String, timeoutMs: Int = APP_CONTROLS_TIMEOUT_MS): String {
        val connection = connect(timeoutMs)
        try {
            connection.setTimeout(timeoutMs)
            val payload = "@${command.length}\n$command"
            connection.output.write(payload.toByteArray(StandardCharsets.UTF_8))
            connection.output.flush()
            val response = String(
                readAll(connection.input, APP_CONTROLS_READ_LIMIT + 1),
                StandardCharsets.UTF_8,
            )
            if (response.length > APP_CONTROLS_READ_LIMIT) throw IOException("profile reply too large")
            val trimmed = response.trim()
            if (trimmed.startsWith("OK ")) return trimmed.removePrefix("OK ")
            throw IOException(trimmed)
        } finally {
            connection.close()
        }
    }

    /**
     * Publishes the authenticated reverse listener early, like the native
     * worker does while the interface starts, so the daemon can connect
     * before the first dashboard frame.
     */
    fun primeReverseListener() {
        if (reverseListener != null) return
        synchronized(this) {
            if (reverseListener != null) return
            val listener = try {
                LocalServerSocket(REVERSE_SOCKET_NAME)
            } catch (_: IOException) {
                return
            }
            reverseListener = listener
            Thread({ reverseAcceptLoop(listener) }, "rodin-reverse-ipc").apply {
                isDaemon = true
                start()
            }
        }
    }

    @Volatile
    private var reverseListener: LocalServerSocket? = null
    private val reverseIncoming = LinkedBlockingQueue<LocalSocket>(4)

    private fun reverseAcceptLoop(listener: LocalServerSocket) {
        while (true) {
            val socket = try {
                listener.accept()
            } catch (_: IOException) {
                Thread.sleep(100)
                continue
            }
            if (peerUid(socket) != 0) {
                try {
                    socket.close()
                } catch (_: IOException) {
                }
                continue
            }
            if (!reverseIncoming.offer(socket)) {
                // The native bridge uses a capacity-one rendezvous; shedding an
                // extra daemon-initiated connection is safe, the daemon retries.
                try {
                    socket.close()
                } catch (_: IOException) {
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun connect(timeoutMs: Int): Connection {
        if (ipcTransport == 2) {
            return try {
                configureUnix(takeReverseConnection())
            } catch (reverseError: IOException) {
                // A root-manager policy or daemon restart can invalidate a
                // transport that worked earlier. Do not pin this process
                // Offline until it is relaunched.
                ipcTransport = 0
                try {
                    val socket = connectLoopback()
                    ipcTransport = 3
                    socket
                } catch (loopbackError: IOException) {
                    throw IOException("$reverseError; $loopbackError")
                }
            }
        }
        if (ipcTransport == 3) {
            return try {
                connectLoopback()
            } catch (loopbackError: IOException) {
                ipcTransport = 0
                try {
                    configureUnix(takeReverseConnection())
                } catch (reverseError: IOException) {
                    throw IOException("$loopbackError; $reverseError")
                }
            }
        }

        val direct = try {
            connectDirect()
        } catch (error: IOException) {
            return fallback(error.message ?: "direct connect failed", timeoutMs)
        }
        ipcTransport = 1
        return configureUnix(direct)
    }

    @Throws(IOException::class)
    private fun connectDirect(): LocalSocket {
        val socket = LocalSocket()
        try {
            socket.connect(
                LocalSocketAddress(SOCKET_NAME, LocalSocketAddress.Namespace.ABSTRACT),
            )
        } catch (error: IOException) {
            try {
                socket.close()
            } catch (_: IOException) {
            }
            throw IOException("direct connect: ${error.message}")
        }
        if (peerUid(socket) != 0) {
            try {
                socket.close()
            } catch (_: IOException) {
            }
            throw IOException("direct daemon peer is not UID 0")
        }
        return socket
    }

    @Throws(IOException::class)
    private fun fallback(directError: String, timeoutMs: Int): Connection {
        return try {
            val socket = connectLoopback()
            ipcTransport = 3
            socket
        } catch (loopbackError: IOException) {
            try {
                val socket = configureUnix(takeReverseConnection())
                ipcTransport = 2
                socket
            } catch (reverseError: IOException) {
                throw IOException("$directError; $loopbackError; $reverseError")
            }
        }
    }

    @Throws(IOException::class)
    private fun connectLoopback(): Connection {
        // Prove the boundary from the app's real SELinux/UID context instead
        // of reading a proc sysctl that some production ROMs hide. A normal
        // app must receive EACCES when it attempts to bind a neighbouring low
        // port. If it can bind, or the result is ambiguous, fail closed.
        val probe = ServerSocket()
        try {
            probe.bind(
                InetSocketAddress(InetAddress.getByName("127.0.0.1"), LOOPBACK_PRIVILEGE_PROBE_PORT),
            )
            throw IOException("ordinary app context can bind the loopback privilege probe")
        } catch (error: java.net.BindException) {
            val message = error.message?.lowercase().orEmpty()
            if (!message.contains("permission") && !message.contains("denied")) {
                throw IOException("unable to prove the loopback privilege boundary: ${error.message}")
            }
        } catch (error: SecurityException) {
            throw IOException("unable to prove the loopback privilege boundary: ${error.message}")
        } finally {
            try {
                probe.close()
            } catch (_: IOException) {
            }
        }

        val socket = Socket()
        try {
            socket.connect(
                InetSocketAddress(InetAddress.getByName("127.0.0.1"), LOOPBACK_PORT),
                400,
            )
            socket.tcpNoDelay = true
        } catch (error: IOException) {
            try {
                socket.close()
            } catch (_: IOException) {
            }
            throw IOException("privileged loopback: ${error.message}")
        }
        return Connection(socket, socket.getInputStream(), socket.getOutputStream())
    }

    private fun configureUnix(socket: LocalSocket): Connection {
        // Super Touch's vendor first-frame transaction may wait up to two
        // seconds for the panel pipeline. Keep the UI asynchronous but allow
        // that verified HAL transaction to finish instead of reporting a
        // false timeout.
        socket.soTimeout = RESPONSE_TIMEOUT_MS
        return Connection(socket, socket.getInputStream(), socket.getOutputStream())
    }

    @Throws(IOException::class)
    private fun takeReverseConnection(): LocalSocket {
        primeReverseListener()
        return reverseIncoming.poll(3, TimeUnit.SECONDS)
            ?: throw IOException("reverse IPC: timed out")
    }

    private fun peerUid(socket: LocalSocket): Int = try {
        socket.peerCredentials.uid
    } catch (_: IOException) {
        -1
    }

    @Throws(IOException::class)
    private fun readAll(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            if (limit > 0 && output.size() > limit) {
                throw IOException("profile reply too large")
            }
        }
        return output.toByteArray()
    }
}
