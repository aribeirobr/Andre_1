package com.andre.shakeflashlight

data class SensitivityProfile(
    val chopThreshold: Float,
    val settleThreshold: Float,
    val maxInterChopMs: Long,
    val twistThreshold: Float
) {
    companion object {
        const val STEPS = 5
        const val DEFAULT_LEVEL = 2

        private val TABLE = arrayOf(
            SensitivityProfile(chopThreshold =  8f, settleThreshold = 2.5f, maxInterChopMs = 900, twistThreshold = 4f),
            SensitivityProfile(chopThreshold = 11f, settleThreshold = 3f,   maxInterChopMs = 850, twistThreshold = 5f),
            SensitivityProfile(chopThreshold = 14f, settleThreshold = 4f,   maxInterChopMs = 800, twistThreshold = 6f),
            SensitivityProfile(chopThreshold = 17f, settleThreshold = 5f,   maxInterChopMs = 750, twistThreshold = 7f),
            SensitivityProfile(chopThreshold = 20f, settleThreshold = 6f,   maxInterChopMs = 700, twistThreshold = 8f),
        )

        fun forLevel(level: Int): SensitivityProfile =
            TABLE[level.coerceIn(0, STEPS - 1)]
    }
}
