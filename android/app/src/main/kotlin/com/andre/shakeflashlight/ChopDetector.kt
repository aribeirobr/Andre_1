package com.andre.shakeflashlight

import android.os.SystemClock
import kotlin.math.sqrt

/**
 * Detects a Motorola-style "double chop" from linear-acceleration samples.
 *
 * A chop = a brief acceleration spike whose magnitude crosses [CHOP_THRESHOLD]
 * and then settles below [SETTLE_THRESHOLD] inside [MAX_CHOP_DURATION_MS].
 *
 * A double chop = two chops, separated by at least [MIN_INTER_CHOP_MS] and at
 * most [MAX_INTER_CHOP_MS].  After firing, further samples are ignored for
 * [POST_FIRE_COOLDOWN_MS] so a single gesture can only toggle once.
 */
class ChopDetector(private val onDoubleChop: () -> Unit) {

    private var inChop = false
    private var chopStartMs = 0L
    private var lastChopEndMs = 0L
    private var cooldownUntilMs = 0L

    fun onSample(x: Float, y: Float, z: Float, nowMs: Long = SystemClock.elapsedRealtime()) {
        if (nowMs < cooldownUntilMs) return

        val magnitude = sqrt(x * x + y * y + z * z)

        if (!inChop) {
            if (magnitude >= CHOP_THRESHOLD) {
                inChop = true
                chopStartMs = nowMs
            }
            return
        }

        val chopDuration = nowMs - chopStartMs
        if (chopDuration > MAX_CHOP_DURATION_MS) {
            inChop = false
            return
        }

        if (magnitude <= SETTLE_THRESHOLD) {
            inChop = false
            val gap = nowMs - lastChopEndMs
            if (lastChopEndMs != 0L && gap in MIN_INTER_CHOP_MS..MAX_INTER_CHOP_MS) {
                lastChopEndMs = 0L
                cooldownUntilMs = nowMs + POST_FIRE_COOLDOWN_MS
                onDoubleChop()
            } else {
                lastChopEndMs = nowMs
            }
        }
    }

    companion object {
        const val CHOP_THRESHOLD = 14f
        const val SETTLE_THRESHOLD = 4f
        const val MAX_CHOP_DURATION_MS = 250L
        const val MIN_INTER_CHOP_MS = 120L
        const val MAX_INTER_CHOP_MS = 800L
        const val POST_FIRE_COOLDOWN_MS = 600L
    }
}
