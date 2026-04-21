package com.andre.shakeflashlight

enum class GestureMode(val prefValue: String, val labelRes: Int) {
    DOUBLE_CHOP  ("double_chop",   R.string.gesture_double_chop),
    SINGLE_SHAKE ("single_shake",  R.string.gesture_single_shake),
    TRIPLE_SHAKE ("triple_shake",  R.string.gesture_triple_shake),
    WRIST_TWIST  ("wrist_twist",   R.string.gesture_wrist_twist),
    CUSTOM_MOTION("custom_motion", R.string.gesture_custom_motion);

    val usesGyroscope: Boolean get() = this == WRIST_TWIST

    companion object {
        val DEFAULT = DOUBLE_CHOP

        fun fromPref(value: String?): GestureMode =
            entries.firstOrNull { it.prefValue == value } ?: DEFAULT
    }
}
