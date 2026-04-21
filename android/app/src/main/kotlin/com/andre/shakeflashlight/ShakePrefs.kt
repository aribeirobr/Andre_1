package com.andre.shakeflashlight

import android.content.Context
import android.content.SharedPreferences

object ShakePrefs {
    const val FILE = "shake_flashlight"

    const val KEY_DETECTION_ON    = "detection_on"
    const val KEY_SENSITIVITY     = "sensitivity"
    const val KEY_GESTURE_MODE    = "gesture_mode"
    const val KEY_CUSTOM_PATTERN  = "custom_pattern_v1"
    const val KEY_MATCH_STRICTNESS = "match_strictness"

    const val STRICTNESS_STEPS = 5
    const val DEFAULT_STRICTNESS = 2

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun sensitivity(prefs: SharedPreferences): Int =
        prefs.getInt(KEY_SENSITIVITY, SensitivityProfile.DEFAULT_LEVEL)
            .coerceIn(0, SensitivityProfile.STEPS - 1)

    fun gestureMode(prefs: SharedPreferences): GestureMode =
        GestureMode.fromPref(prefs.getString(KEY_GESTURE_MODE, null))

    fun customPattern(prefs: SharedPreferences): MotionPattern? =
        MotionPattern.decode(prefs.getString(KEY_CUSTOM_PATTERN, null))

    fun strictness(prefs: SharedPreferences): Int =
        prefs.getInt(KEY_MATCH_STRICTNESS, DEFAULT_STRICTNESS)
            .coerceIn(0, STRICTNESS_STEPS - 1)

    /** DTW cost threshold per strictness level (normalized by path length). */
    fun strictnessThreshold(level: Int): Float = when (level.coerceIn(0, 4)) {
        0 -> 6.0f   // Very loose
        1 -> 4.5f   // Loose
        2 -> 3.0f   // Medium (default)
        3 -> 2.0f   // Strict
        else -> 1.25f  // Very strict
    }
}
