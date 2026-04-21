package com.andre.shakeflashlight

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat

class ShakeFlashlightService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private var gyroSensor: Sensor? = null
    private lateinit var torch: TorchController
    private lateinit var prefs: SharedPreferences

    private var recognizer: GestureRecognizer = NullRecognizer
    private var currentMode: GestureMode = GestureMode.DEFAULT

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            ShakePrefs.KEY_SENSITIVITY,
            ShakePrefs.KEY_GESTURE_MODE,
            ShakePrefs.KEY_CUSTOM_PATTERN,
            ShakePrefs.KEY_MATCH_STRICTNESS -> rebuildRecognizer()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        torch = TorchController(this)
        prefs = ShakePrefs.get(this)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        rebuildRecognizer()
        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        torch.turnOff()
        torch.release()
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val nowMs = SystemClock.elapsedRealtime()
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION ->
                recognizer.onAccel(event.values[0], event.values[1], event.values[2], nowMs)
            Sensor.TYPE_ACCELEROMETER ->
                recognizer.onAccel(
                    event.values[0],
                    event.values[1],
                    event.values[2] - GRAVITY,
                    nowMs
                )
            Sensor.TYPE_GYROSCOPE ->
                recognizer.onGyro(event.values[0], event.values[1], event.values[2], nowMs)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun rebuildRecognizer() {
        sensorManager.unregisterListener(this)

        val mode = ShakePrefs.gestureMode(prefs)
        val profile = SensitivityProfile.forLevel(ShakePrefs.sensitivity(prefs))
        val fire = { torch.toggle(); Unit }

        recognizer = when (mode) {
            GestureMode.DOUBLE_CHOP   -> DoubleChopDetector(profile, fire)
            GestureMode.SINGLE_SHAKE  -> SingleShakeDetector(profile, fire)
            GestureMode.TRIPLE_SHAKE  -> TripleShakeDetector(profile, fire)
            GestureMode.WRIST_TWIST   -> WristTwistDetector(profile, fire)
            GestureMode.CUSTOM_MOTION -> buildCustomDetector(fire) ?: NullRecognizer
        }
        currentMode = mode

        accelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        if (mode.usesGyroscope) {
            gyroSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    private fun startInForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setOngoing(true)
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildCustomDetector(fire: () -> Unit): CustomPatternDetector? {
        val pattern = ShakePrefs.customPattern(prefs) ?: return null
        val strictness = ShakePrefs.strictness(prefs)
        val threshold = ShakePrefs.strictnessThreshold(strictness)
        return CustomPatternDetector(pattern, threshold, fire)
    }

    private object NullRecognizer : GestureRecognizer

    companion object {
        private const val CHANNEL_ID = "shake_detector"
        private const val NOTIF_ID = 101
        private const val GRAVITY = 9.81f
    }
}
