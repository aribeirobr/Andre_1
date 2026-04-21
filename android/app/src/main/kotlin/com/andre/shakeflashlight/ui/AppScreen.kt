package com.andre.shakeflashlight.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppScreen(
    detectionEnabled: Boolean,
    hasFlash: Boolean,
    onToggleDetection: (Boolean) -> Unit,
    onTestFlashlight: () -> Unit
) {
    MaterialTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Shake Flashlight",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Double-chop the phone (two quick downward flicks) to toggle the flashlight on and off.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!hasFlash) {
                    Text(
                        text = "No back-camera flash detected — the app cannot run on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    label = if (detectionEnabled) "Detection running" else "Detection off"
                ) {
                    Switch(
                        checked = detectionEnabled,
                        onCheckedChange = onToggleDetection,
                        enabled = hasFlash
                    )
                }

                Button(
                    onClick = onTestFlashlight,
                    enabled = hasFlash
                ) {
                    Text("Test flashlight")
                }
            }
        }
    }
}

@Composable
private fun Row(label: String, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        content()
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
