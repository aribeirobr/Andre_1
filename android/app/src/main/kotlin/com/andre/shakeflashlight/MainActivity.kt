package com.andre.shakeflashlight

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.andre.shakeflashlight.ui.AppScreen
import com.andre.shakeflashlight.ui.RecordingState

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var torch: TorchController
    private var activeRecorder: PatternRecorder? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) maybeRequestNotificationPermission()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* detection still works silently if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = ShakePrefs.get(this)
        torch = TorchController(this)

        setContent {
            var detectionOn by remember {
                mutableStateOf(prefs.getBoolean(ShakePrefs.KEY_DETECTION_ON, false))
            }
            var sensitivity by remember {
                mutableIntStateOf(ShakePrefs.sensitivity(prefs))
            }
            var gesture by remember {
                mutableStateOf(ShakePrefs.gestureMode(prefs))
            }
            var strictness by remember {
                mutableIntStateOf(ShakePrefs.strictness(prefs))
            }
            var pattern by remember {
                mutableStateOf(ShakePrefs.customPattern(prefs))
            }
            var recordingState: RecordingState by remember {
                mutableStateOf(RecordingState.Idle)
            }

            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        ShakePrefs.KEY_SENSITIVITY     -> sensitivity = ShakePrefs.sensitivity(prefs)
                        ShakePrefs.KEY_GESTURE_MODE    -> gesture = ShakePrefs.gestureMode(prefs)
                        ShakePrefs.KEY_DETECTION_ON    ->
                            detectionOn = prefs.getBoolean(ShakePrefs.KEY_DETECTION_ON, false)
                        ShakePrefs.KEY_CUSTOM_PATTERN  -> pattern = ShakePrefs.customPattern(prefs)
                        ShakePrefs.KEY_MATCH_STRICTNESS -> strictness = ShakePrefs.strictness(prefs)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            AppScreen(
                detectionEnabled = detectionOn,
                hasFlash = torch.hasFlash(),
                sensitivityLevel = sensitivity,
                gestureMode = gesture,
                strictnessLevel = strictness,
                hasCustomPattern = pattern != null,
                patternSummary = pattern?.let {
                    "${"%.1f".format(it.durationMs / 1000f)}s, peak ${"%.1f".format(it.peakMagnitude)} m/s²"
                },
                recordingState = recordingState,
                onToggleDetection = { enabled ->
                    detectionOn = enabled
                    prefs.edit().putBoolean(ShakePrefs.KEY_DETECTION_ON, enabled).apply()
                    if (enabled) startDetection() else stopDetection()
                },
                onSensitivityChange = { level ->
                    sensitivity = level
                    prefs.edit().putInt(ShakePrefs.KEY_SENSITIVITY, level).apply()
                },
                onGestureChange = { mode ->
                    gesture = mode
                    prefs.edit().putString(ShakePrefs.KEY_GESTURE_MODE, mode.prefValue).apply()
                },
                onStrictnessChange = { level ->
                    strictness = level
                    prefs.edit().putInt(ShakePrefs.KEY_MATCH_STRICTNESS, level).apply()
                },
                onTestFlashlight = { torch.toggle() },
                onStartRecording = {
                    if (recordingState != RecordingState.Idle) return@AppScreen
                    startRecording { state -> recordingState = state }
                },
                onCancelRecording = {
                    activeRecorder?.cancel()
                    activeRecorder = null
                    recordingState = RecordingState.Idle
                }
            )
        }

        ensurePermissions()
        if (prefs.getBoolean(ShakePrefs.KEY_DETECTION_ON, false)) startDetection()
    }

    override fun onDestroy() {
        activeRecorder?.cancel()
        activeRecorder = null
        torch.release()
        super.onDestroy()
    }

    private fun startRecording(onState: (RecordingState) -> Unit) {
        val recorder = PatternRecorder(
            context = this,
            callbacks = object : PatternRecorder.Callbacks {
                override fun onCountdownTick(secondsRemaining: Int) {
                    onState(
                        if (secondsRemaining <= 0)
                            RecordingState.Recording
                        else
                            RecordingState.Countdown(secondsRemaining)
                    )
                }
                override fun onRecordingStarted() {
                    onState(RecordingState.Recording)
                }
                override fun onRecorded(pattern: MotionPattern) {
                    prefs.edit()
                        .putString(ShakePrefs.KEY_CUSTOM_PATTERN, pattern.encode())
                        .apply()
                    onState(RecordingState.Saved(
                        durationMs = pattern.durationMs,
                        peakMagnitude = pattern.peakMagnitude
                    ))
                    activeRecorder = null
                }
                override fun onError(message: String) {
                    onState(RecordingState.Error(message))
                    activeRecorder = null
                }
            }
        )
        activeRecorder = recorder
        onState(RecordingState.Countdown(3))
        recorder.arm()
    }

    private fun ensurePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            maybeRequestNotificationPermission()
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startDetection() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, ShakeFlashlightService::class.java)
        )
    }

    private fun stopDetection() {
        stopService(Intent(this, ShakeFlashlightService::class.java))
    }
}
