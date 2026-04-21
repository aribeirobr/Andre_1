package com.andre.shakeflashlight

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Matches live accelerometer data against a recorded [MotionPattern] using
 * Dynamic Time Warping with a Sakoe-Chiba band.
 *
 * Matching runs every [MATCH_STRIDE] samples once the rolling window has
 * enough data *and* shows recent activity (peak magnitude over the window
 * above [ACTIVITY_FLOOR] m/s²).  When a match completes below
 * [costThreshold], [onFire] is called and the detector enters a
 * [POST_FIRE_COOLDOWN_MS] cooldown.
 */
class CustomPatternDetector(
    private val pattern: MotionPattern,
    private val costThreshold: Float,
    private val onFire: () -> Unit
) : GestureRecognizer {

    private val windowSize = (pattern.size * WINDOW_OVERFLOW_FACTOR).toInt()
    private val ring = Array(windowSize) { FloatArray(3) }
    private var ringFilled = 0
    private var ringHead = 0
    private var samplesSinceMatch = 0
    private var cooldownUntilMs = 0L

    override fun onAccel(x: Float, y: Float, z: Float, nowMs: Long) {
        ring[ringHead][0] = x
        ring[ringHead][1] = y
        ring[ringHead][2] = z
        ringHead = (ringHead + 1) % windowSize
        if (ringFilled < windowSize) ringFilled++

        if (nowMs < cooldownUntilMs) {
            samplesSinceMatch = 0
            return
        }
        if (ringFilled < pattern.size) return

        samplesSinceMatch++
        if (samplesSinceMatch < MATCH_STRIDE) return
        samplesSinceMatch = 0

        if (!hasRecentActivity()) return

        val window = linearizeWindow()
        val cost = dtwCost(pattern.samples, window)
        if (cost <= costThreshold) {
            cooldownUntilMs = nowMs + POST_FIRE_COOLDOWN_MS
            onFire()
        }
    }

    private fun linearizeWindow(): Array<FloatArray> {
        val out = Array(ringFilled) { FloatArray(3) }
        val start = if (ringFilled < windowSize) 0 else ringHead
        for (i in 0 until ringFilled) {
            val src = ring[(start + i) % windowSize]
            out[i][0] = src[0]; out[i][1] = src[1]; out[i][2] = src[2]
        }
        return out
    }

    private fun hasRecentActivity(): Boolean {
        var peak = 0f
        val start = if (ringFilled < windowSize) 0 else ringHead
        for (i in 0 until ringFilled) {
            val s = ring[(start + i) % windowSize]
            val m = sqrt(s[0] * s[0] + s[1] * s[1] + s[2] * s[2])
            if (m > peak) peak = m
        }
        return peak >= ACTIVITY_FLOOR
    }

    companion object {
        private const val WINDOW_OVERFLOW_FACTOR = 1.4
        private const val MATCH_STRIDE = 5           // re-evaluate every ~100 ms
        private const val ACTIVITY_FLOOR = 5f        // m/s²
        private const val POST_FIRE_COOLDOWN_MS = 1200L
        private const val BAND_WIDTH = 15            // Sakoe-Chiba band (samples)

        fun dtwCost(a: Array<FloatArray>, b: Array<FloatArray>): Float {
            val m = a.size
            val n = b.size
            if (m == 0 || n == 0) return Float.MAX_VALUE

            val band = max(BAND_WIDTH, kotlin.math.abs(m - n) + 2)
            var prev = FloatArray(n + 1) { Float.MAX_VALUE }
            var curr = FloatArray(n + 1) { Float.MAX_VALUE }
            prev[0] = 0f

            for (i in 1..m) {
                for (j in 0..n) curr[j] = Float.MAX_VALUE
                val jStart = max(1, i - band)
                val jEnd = min(n, i + band)
                for (j in jStart..jEnd) {
                    val d = dist(a[i - 1], b[j - 1])
                    val best = min(prev[j - 1], min(prev[j], curr[j - 1]))
                    if (best < Float.MAX_VALUE) curr[j] = d + best
                }
                val tmp = prev; prev = curr; curr = tmp
            }
            val total = prev[n]
            return if (total == Float.MAX_VALUE) total else total / (m + n).toFloat()
        }

        private fun dist(a: FloatArray, b: FloatArray): Float {
            val dx = a[0] - b[0]
            val dy = a[1] - b[1]
            val dz = a[2] - b[2]
            return sqrt(dx * dx + dy * dy + dz * dz)
        }
    }
}
