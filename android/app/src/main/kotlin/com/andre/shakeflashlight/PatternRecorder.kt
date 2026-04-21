package com.andre.shakeflashlight

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper

/**
 * Captures linear-acceleration samples from the phone for the duration of a
 * recording session so the user can teach the app a custom motion.
 *
 * Lifecycle:
 *   - [arm] registers a sensor listener and starts a countdown.
 *   - After [countdownMs] a [Callbacks.onRecordingStarted] fires.
 *   - Samples are collected for [recordingMs] (or until a hard cap).
 *   - [Callbacks.onRecorded] delivers the resulting pattern, or
 *     [Callbacks.onError] fires if not enough motion was captured.
 *   - [cancel] aborts at any stage.
 */
class PatternRecorder(
    context: Context,
    private val callbacks: Callbacks,
    private val countdownMs: Long = 3_000L,
    private val recordingMs: Long = 2_000L,
) : SensorEventListener {

    interface Callbacks {
        fun onCountdownTick(secondsRemaining: Int)
        fun onRecordingStarted()
        fun onRecorded(pattern: MotionPattern)
        fun onError(message: String)
    }

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val usesGravitySensor = accelSensor?.type == Sensor.TYPE_LINEAR_ACCELERATION
    private val handler = Handler(Looper.getMainLooper())

    private enum class State { IDLE, COUNTDOWN, RECORDING, DONE }
    private var state = State.IDLE
    private val samples = ArrayList<FloatArray>(MotionPattern.MAX_SAMPLES)

    fun arm() {
        if (state != State.IDLE) return
        val sensor = accelSensor ?: run {
            callbacks.onError("No accelerometer on this device.")
            return
        }

        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)

        state = State.COUNTDOWN
        val totalSeconds = ((countdownMs + 999) / 1000).toInt()
        callbacks.onCountdownTick(totalSeconds)
        for (i in 1..totalSeconds) {
            handler.postDelayed(
                { if (state == State.COUNTDOWN) callbacks.onCountdownTick(totalSeconds - i) },
                i * 1000L
            )
        }

        handler.postDelayed({
            if (state != State.COUNTDOWN) return@postDelayed
            state = State.RECORDING
            samples.clear()
            callbacks.onRecordingStarted()
        }, countdownMs)

        handler.postDelayed({ finish() }, countdownMs + recordingMs)
    }

    fun cancel() {
        if (state == State.IDLE || state == State.DONE) return
        state = State.DONE
        handler.removeCallbacksAndMessages(null)
        sensorManager.unregisterListener(this)
        samples.clear()
    }

    private fun finish() {
        if (state != State.RECORDING) return
        state = State.DONE
        sensorManager.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)

        val trimmed = trimLeadingSilence(samples)
        if (trimmed.size < MotionPattern.MIN_SAMPLES) {
            callbacks.onError("Not enough motion captured. Try a bigger shake.")
            return
        }
        callbacks.onRecorded(MotionPattern(trimmed.toTypedArray()))
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (state != State.RECORDING) return
        if (samples.size >= MotionPattern.MAX_SAMPLES) return

        val (x, y, z) = if (usesGravitySensor) {
            Triple(event.values[0], event.values[1], event.values[2])
        } else {
            Triple(event.values[0], event.values[1], event.values[2] - GRAVITY)
        }
        samples.add(floatArrayOf(x, y, z))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun trimLeadingSilence(samples: List<FloatArray>): List<FloatArray> {
        val firstActive = samples.indexOfFirst {
            val m2 = it[0] * it[0] + it[1] * it[1] + it[2] * it[2]
            m2 > TRIM_FLOOR_SQ
        }
        return if (firstActive <= 0) samples else samples.drop(firstActive)
    }

    companion object {
        private const val GRAVITY = 9.81f
        private const val TRIM_FLOOR_SQ = 4f * 4f  // start once magnitude exceeds 4 m/s²
    }
}
