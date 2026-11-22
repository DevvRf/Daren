package com.gamespace.app

import java.util.concurrent.atomic.AtomicBoolean

enum class Trend { UP, DOWN, FLAT, UNKNOWN }

data class PingReading(
    val ms: Long?,
    val quality: PingMonitor.PingQuality,
    val trend: Trend
)

/**
 * Runs real ping measurements on a dedicated background thread so network
 * I/O never blocks the UI. Call start() once, read latest() anytime from
 * the main thread (it's just a volatile field read, safe and cheap).
 */
class PingTracker {

    @Volatile
    private var latestReading: PingReading = PingReading(null, PingMonitor.PingQuality.UNKNOWN, Trend.UNKNOWN)

    private var previousMs: Long? = null
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null

    fun start(intervalMs: Long = 2500L) {
        if (running.get()) return
        running.set(true)
        thread = Thread {
            while (running.get()) {
                val result = PingMonitor.measure()
                val trend = when {
                    result.ms == null || previousMs == null -> Trend.UNKNOWN
                    result.ms < previousMs!! -> Trend.DOWN
                    result.ms > previousMs!! -> Trend.UP
                    else -> Trend.FLAT
                }
                previousMs = result.ms
                latestReading = PingReading(result.ms, result.quality, trend)
                try {
                    Thread.sleep(intervalMs)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.apply { isDaemon = true; start() }
    }

    fun stop() {
        running.set(false)
        thread?.interrupt()
        thread = null
    }

    fun latest(): PingReading = latestReading
}
