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
import androidx.compose.runtime.getValue
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
    ) { /* Detection still works even if denied — the notification is just silent. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("shake_flashlight", MODE_PRIVATE)
        torch = TorchController(this)

        setContent {
            var detectionOn by remember { mutableStateOf(prefs.getBoolean(KEY_DETECTION_ON, false)) }
            AppScreen(
                detectionEnabled = detectionOn,
                hasFlash = torch.hasFlash(),
                onToggleDetection = { enabled ->
                    detectionOn = enabled
                    prefs.edit().putBoolean(KEY_DETECTION_ON, enabled).apply()
                    if (enabled) startDetection() else stopDetection()
                },
                onTestFlashlight = { torch.toggle() }
            )
        }

        ensurePermissions()
        if (prefs.getBoolean(KEY_DETECTION_ON, false)) startDetection()
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
        val intent = Intent(this, ShakeFlashlightService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopDetection() {
        stopService(Intent(this, ShakeFlashlightService::class.java))
    }

    companion object {
        private const val KEY_DETECTION_ON = "detection_on"
    }
}
