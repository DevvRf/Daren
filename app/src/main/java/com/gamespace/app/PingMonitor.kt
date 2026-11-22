package com.gamespace.app

import java.net.InetSocketAddress
import java.net.Socket

/**
 * Real network latency measurement — opens an actual TCP connection to a
 * reliable public host and times how long the handshake takes. This is a
 * genuine round-trip measurement, not a simulated/random number.
 */
object PingMonitor {

    enum class PingQuality { GOOD, OK, BAD, UNKNOWN }

    data class PingResult(val ms: Long?, val quality: PingQuality)

    private const val HOST = "8.8.8.8"
    private const val PORT = 53
    private const val TIMEOUT_MS = 2000

    /** Blocking call — run this off the main thread. */
    fun measure(): PingResult {
        return try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(HOST, PORT), TIMEOUT_MS)
            }
            val elapsed = System.currentTimeMillis() - start
            PingResult(elapsed, qualityFor(elapsed))
        } catch (e: Exception) {
            PingResult(null, PingQuality.UNKNOWN)
        }
    }

    private fun qualityFor(ms: Long): PingQuality = when {
        ms < 80 -> PingQuality.GOOD
        ms < 180 -> PingQuality.OK
        else -> PingQuality.BAD
    }
}
