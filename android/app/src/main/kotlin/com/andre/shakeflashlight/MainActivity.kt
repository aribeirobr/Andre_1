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

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var torch: TorchController

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

            // Keep UI state in sync with prefs even when changed elsewhere (unlikely,
            // but defensive — the service also writes nothing, but this is cheap).
            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        ShakePrefs.KEY_SENSITIVITY  -> sensitivity = ShakePrefs.sensitivity(prefs)
                        ShakePrefs.KEY_GESTURE_MODE -> gesture = ShakePrefs.gestureMode(prefs)
                        ShakePrefs.KEY_DETECTION_ON ->
                            detectionOn = prefs.getBoolean(ShakePrefs.KEY_DETECTION_ON, false)
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
                onTestFlashlight = { torch.toggle() }
            )
        }

        ensurePermissions()
        if (prefs.getBoolean(ShakePrefs.KEY_DETECTION_ON, false)) startDetection()
    }

    override fun onDestroy() {
        torch.release()
        super.onDestroy()
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
