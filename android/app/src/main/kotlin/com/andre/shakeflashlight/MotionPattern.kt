package com.andre.shakeflashlight

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * A recorded motion: a sequence of linear-acceleration samples (x, y, z,
 * m/s²) at ~50 Hz.  Stored in prefs as Base64-encoded little-endian floats.
 */
class MotionPattern(val samples: Array<FloatArray>) {

    val size: Int get() = samples.size

    /** Peak linear-acceleration magnitude over the whole recording. */
    val peakMagnitude: Float by lazy {
        var peak = 0f
        for (s in samples) {
            val m = sqrt(s[0] * s[0] + s[1] * s[1] + s[2] * s[2])
            if (m > peak) peak = m
        }
        peak
    }

    /** Duration in milliseconds assuming [SAMPLE_PERIOD_MS] between samples. */
    val durationMs: Int get() = samples.size * SAMPLE_PERIOD_MS

    fun encode(): String {
        val buf = ByteBuffer.allocate(samples.size * 12).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) {
            buf.putFloat(s[0]); buf.putFloat(s[1]); buf.putFloat(s[2])
        }
        return Base64.encodeToString(buf.array(), Base64.NO_WRAP)
    }

    companion object {
        /** Nominal sample period (SENSOR_DELAY_GAME is ~20 ms on most phones). */
        const val SAMPLE_PERIOD_MS = 20

        const val MIN_SAMPLES = 25   // ~0.5 s, reject accidental taps
        const val MAX_SAMPLES = 175  // ~3.5 s, hard cap

        fun decode(encoded: String?): MotionPattern? {
            if (encoded.isNullOrEmpty()) return null
            return try {
                val bytes = Base64.decode(encoded, Base64.NO_WRAP)
                val n = bytes.size / 12
                if (n < MIN_SAMPLES) return null
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                MotionPattern(Array(n) {
                    floatArrayOf(buf.getFloat(), buf.getFloat(), buf.getFloat())
                })
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
