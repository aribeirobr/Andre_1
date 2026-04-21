package com.andre.shakeflashlight.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.andre.shakeflashlight.GestureMode
import com.andre.shakeflashlight.R
import com.andre.shakeflashlight.SensitivityProfile

@Composable
fun AppScreen(
    detectionEnabled: Boolean,
    hasFlash: Boolean,
    sensitivityLevel: Int,
    gestureMode: GestureMode,
    onToggleDetection: (Boolean) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onGestureChange: (GestureMode) -> Unit,
    onTestFlashlight: () -> Unit
) {
    MaterialTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = stringResource(R.string.intro_blurb),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!hasFlash) {
                    Text(
                        text = stringResource(R.string.no_flash_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(8.dp))

                DetectionSwitchRow(
                    checked = detectionEnabled,
                    enabled = hasFlash,
                    onCheckedChange = onToggleDetection
                )

                Button(onClick = onTestFlashlight, enabled = hasFlash) {
                    Text(stringResource(R.string.test_flashlight))
                }

                Spacer(Modifier.height(8.dp))

                SensitivitySection(
                    level = sensitivityLevel,
                    enabled = hasFlash,
                    onLevelChange = onSensitivityChange
                )

                Spacer(Modifier.height(8.dp))

                GestureSection(
                    selected = gestureMode,
                    enabled = hasFlash,
                    onSelect = onGestureChange
                )
            }
        }
    }
}

@Composable
private fun DetectionSwitchRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Text(
            text = stringResource(
                if (checked) R.string.detection_running else R.string.detection_off
            ),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SensitivitySection(
    level: Int,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit
) {
    val labels = stringArrayResource(R.array.sensitivity_labels)
    val clamped = level.coerceIn(0, SensitivityProfile.STEPS - 1)

    Text(
        text = stringResource(R.string.sensitivity_title),
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        text = labels[clamped],
        style = MaterialTheme.typography.bodyLarge
    )
    Slider(
        value = clamped.toFloat(),
        onValueChange = { onLevelChange(it.toInt().coerceIn(0, SensitivityProfile.STEPS - 1)) },
        valueRange = 0f..(SensitivityProfile.STEPS - 1).toFloat(),
        steps = SensitivityProfile.STEPS - 2,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = stringResource(R.string.sensitivity_hint),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun GestureSection(
    selected: GestureMode,
    enabled: Boolean,
    onSelect: (GestureMode) -> Unit
) {
    Text(
        text = stringResource(R.string.gesture_title),
        style = MaterialTheme.typography.titleMedium
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GestureMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = mode == selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(mode) }
                    )
            ) {
                RadioButton(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    enabled = enabled
                )
                Text(
                    text = stringResource(mode.labelRes),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
