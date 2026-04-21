package com.andre.shakeflashlight

import android.content.Context
import android.content.SharedPreferences

object ShakePrefs {
    const val FILE = "shake_flashlight"

    const val KEY_DETECTION_ON = "detection_on"
    const val KEY_SENSITIVITY  = "sensitivity"
    const val KEY_GESTURE_MODE = "gesture_mode"

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun sensitivity(prefs: SharedPreferences): Int =
        prefs.getInt(KEY_SENSITIVITY, SensitivityProfile.DEFAULT_LEVEL)
            .coerceIn(0, SensitivityProfile.STEPS - 1)

    fun gestureMode(prefs: SharedPreferences): GestureMode =
        GestureMode.fromPref(prefs.getString(KEY_GESTURE_MODE, null))
}
