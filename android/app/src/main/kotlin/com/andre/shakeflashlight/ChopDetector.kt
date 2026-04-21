package com.andre.shakeflashlight

import kotlin.math.sqrt

/**
 * Common callback contract for every gesture implementation.  Detectors
 * implement only the sensor callback they care about and leave the other as
 * the no-op default.
 */
interface GestureRecognizer {
    fun onAccel(x: Float, y: Float, z: Float, nowMs: Long) = Unit
    fun onGyro(x: Float, y: Float, z: Float, nowMs: Long) = Unit
}

/**
 * Low-level spike detector shared by every shake-style gesture.  Every time
 * linear-acceleration magnitude crosses [profile.chopThreshold] and then
 * falls back below [profile.settleThreshold] within [MAX_CHOP_DURATION_MS],
 * a "spike" completes and [onSpike] is called with its end timestamp.
 */
class ShakeSpikeDetector(
    private val profile: SensitivityProfile,
    private val onSpike: (Long) -> Unit
) {
    private var inSpike = false
    private var spikeStartMs = 0L

    fun onSample(x: Float, y: Float, z: Float, nowMs: Long) {
        val magnitude = sqrt(x * x + y * y + z * z)

        if (!inSpike) {
            if (magnitude >= profile.chopThreshold) {
                inSpike = true
                spikeStartMs = nowMs
            }
            return
        }

        if (nowMs - spikeStartMs > MAX_CHOP_DURATION_MS) {
            inSpike = false
            return
        }

        if (magnitude <= profile.settleThreshold) {
            inSpike = false
            onSpike(nowMs)
        }
    }

    companion object {
        const val MAX_CHOP_DURATION_MS = 250L
    }
}

/** Two chops within `profile.maxInterChopMs` toggle the torch. */
class DoubleChopDetector(
    private val profile: SensitivityProfile,
    private val onFire: () -> Unit
) : GestureRecognizer {

    private var lastSpikeMs = 0L
    private var cooldownUntilMs = 0L

    private val spikes = ShakeSpikeDetector(profile) { spikeEndMs ->
        if (spikeEndMs < cooldownUntilMs) return@ShakeSpikeDetector

        val gap = spikeEndMs - lastSpikeMs
        if (lastSpikeMs != 0L && gap in MIN_INTER_CHOP_MS..profile.maxInterChopMs) {
            lastSpikeMs = 0L
            cooldownUntilMs = spikeEndMs + POST_FIRE_COOLDOWN_MS
            onFire()
        } else {
            lastSpikeMs = spikeEndMs
        }
    }

    override fun onAccel(x: Float, y: Float, z: Float, nowMs: Long) {
        if (nowMs >= cooldownUntilMs) spikes.onSample(x, y, z, nowMs)
    }

    companion object {
        const val MIN_INTER_CHOP_MS = 120L
        const val POST_FIRE_COOLDOWN_MS = 600L
    }
}

/** A single strong shake toggles the torch.  Long cooldown guards against
 *  double-firing from one wobble. */
class SingleShakeDetector(
    profile: SensitivityProfile,
    private val onFire: () -> Unit
) : GestureRecognizer {

    private var cooldownUntilMs = 0L

    private val spikes = ShakeSpikeDetector(profile) { spikeEndMs ->
        if (spikeEndMs >= cooldownUntilMs) {
            cooldownUntilMs = spikeEndMs + POST_FIRE_COOLDOWN_MS
            onFire()
        }
    }

    override fun onAccel(x: Float, y: Float, z: Float, nowMs: Long) {
        if (nowMs >= cooldownUntilMs) spikes.onSample(x, y, z, nowMs)
    }

    companion object {
        const val POST_FIRE_COOLDOWN_MS = 1000L
    }
}

/** Three chops within [MAX_TOTAL_MS], each 100..500 ms apart, toggle the
 *  torch.  Much harder to trigger by accident. */
class TripleShakeDetector(
    profile: SensitivityProfile,
    private val onFire: () -> Unit
) : GestureRecognizer {

    private val timestamps = ArrayDeque<Long>(3)
    private var cooldownUntilMs = 0L

    private val spikes = ShakeSpikeDetector(profile) { spikeEndMs ->
        if (spikeEndMs < cooldownUntilMs) return@ShakeSpikeDetector

        if (timestamps.isNotEmpty()) {
            val gap = spikeEndMs - timestamps.last()
            if (gap !in MIN_GAP_MS..MAX_GAP_MS) {
                timestamps.clear()
            }
        }
        timestamps.addLast(spikeEndMs)
        while (timestamps.size > 3) timestamps.removeFirst()

        if (timestamps.size == 3 &&
            timestamps.last() - timestamps.first() <= MAX_TOTAL_MS) {
            timestamps.clear()
            cooldownUntilMs = spikeEndMs + POST_FIRE_COOLDOWN_MS
            onFire()
        }
    }

    override fun onAccel(x: Float, y: Float, z: Float, nowMs: Long) {
        if (nowMs >= cooldownUntilMs) spikes.onSample(x, y, z, nowMs)
    }

    companion object {
        const val MIN_GAP_MS = 100L
        const val MAX_GAP_MS = 500L
        const val MAX_TOTAL_MS = 1500L
        const val POST_FIRE_COOLDOWN_MS = 800L
    }
}
