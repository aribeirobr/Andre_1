package com.andre.shakeflashlight

import kotlin.math.abs

/**
 * Fires on a quick wrist-twist around the axis pointing out of the screen
 * (gyroscope Z).  A twist must:
 *   1. sustain |ω_z| above [profile.twistThreshold] for at least
 *      [MIN_BURST_MS] milliseconds;
 *   2. integrate to at least [MIN_ROTATION_RAD] of total rotation.
 *
 * That rejects slow rotations like sliding the phone into a pocket while
 * still accepting a sharp wrist flip.
 */
class WristTwistDetector(
    private val profile: SensitivityProfile,
    private val onFire: () -> Unit
) : GestureRecognizer {

    private var burstStartMs = 0L
    private var lastSampleMs = 0L
    private var integratedRotation = 0f
    private var burstSign = 0
    private var cooldownUntilMs = 0L

    override fun onGyro(x: Float, y: Float, z: Float, nowMs: Long) {
        if (nowMs < cooldownUntilMs) {
            resetBurst()
            return
        }

        val sign = if (z >= 0f) 1 else -1
        val fast = abs(z) >= profile.twistThreshold

        if (!fast || (burstSign != 0 && sign != burstSign)) {
            if (burstSign != 0 && evaluateBurst(nowMs)) return
            resetBurst()
            if (fast) {
                burstSign = sign
                burstStartMs = nowMs
                lastSampleMs = nowMs
                integratedRotation = 0f
            }
            return
        }

        if (burstSign == 0) {
            burstSign = sign
            burstStartMs = nowMs
            lastSampleMs = nowMs
            integratedRotation = 0f
            return
        }

        val dt = (nowMs - lastSampleMs).coerceAtLeast(0L) / 1000f
        integratedRotation += abs(z) * dt
        lastSampleMs = nowMs

        evaluateBurst(nowMs)
    }

    private fun evaluateBurst(nowMs: Long): Boolean {
        val duration = nowMs - burstStartMs
        if (duration >= MIN_BURST_MS && integratedRotation >= MIN_ROTATION_RAD) {
            cooldownUntilMs = nowMs + POST_FIRE_COOLDOWN_MS
            resetBurst()
            onFire()
            return true
        }
        return false
    }

    private fun resetBurst() {
        burstSign = 0
        burstStartMs = 0L
        lastSampleMs = 0L
        integratedRotation = 0f
    }

    companion object {
        const val MIN_BURST_MS = 120L
        const val MIN_ROTATION_RAD = 1.5f
        const val POST_FIRE_COOLDOWN_MS = 800L
    }
}
