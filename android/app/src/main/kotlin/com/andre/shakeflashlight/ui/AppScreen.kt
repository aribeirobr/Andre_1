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
import androidx.compose.material3.OutlinedButton
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
import com.andre.shakeflashlight.ShakePrefs

sealed class RecordingState {
    data object Idle : RecordingState()
    data class Countdown(val secondsRemaining: Int) : RecordingState()
    data object Recording : RecordingState()
    data class Saved(val durationMs: Int, val peakMagnitude: Float) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

@Composable
fun AppScreen(
    detectionEnabled: Boolean,
    hasFlash: Boolean,
    sensitivityLevel: Int,
    gestureMode: GestureMode,
    strictnessLevel: Int,
    hasCustomPattern: Boolean,
    patternSummary: String?,
    recordingState: RecordingState,
    onToggleDetection: (Boolean) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onGestureChange: (GestureMode) -> Unit,
    onStrictnessChange: (Int) -> Unit,
    onTestFlashlight: () -> Unit,
    onStartRecording: () -> Unit,
    onCancelRecording: () -> Unit
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
                    enabled = hasFlash &&
                        (detectionEnabled || gestureReady(gestureMode, hasCustomPattern)),
                    onCheckedChange = onToggleDetection
                )
                if (gestureMode == GestureMode.CUSTOM_MOTION && !hasCustomPattern) {
                    Text(
                        text = stringResource(R.string.custom_pattern_required_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

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

                if (gestureMode == GestureMode.CUSTOM_MOTION) {
                    Spacer(Modifier.height(8.dp))
                    CustomPatternSection(
                        hasPattern = hasCustomPattern,
                        patternSummary = patternSummary,
                        strictness = strictnessLevel,
                        recordingState = recordingState,
                        enabled = hasFlash,
                        onStartRecording = onStartRecording,
                        onCancelRecording = onCancelRecording,
                        onStrictnessChange = onStrictnessChange
                    )
                }
            }
        }
    }
}

private fun gestureReady(mode: GestureMode, hasCustomPattern: Boolean): Boolean =
    mode != GestureMode.CUSTOM_MOTION || hasCustomPattern

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

@Composable
private fun CustomPatternSection(
    hasPattern: Boolean,
    patternSummary: String?,
    strictness: Int,
    recordingState: RecordingState,
    enabled: Boolean,
    onStartRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onStrictnessChange: (Int) -> Unit
) {
    Text(
        text = stringResource(R.string.custom_pattern_title),
        style = MaterialTheme.typography.titleMedium
    )

    when (recordingState) {
        RecordingState.Idle, is RecordingState.Saved, is RecordingState.Error -> {
            val statusText = when {
                recordingState is RecordingState.Saved ->
                    stringResource(
                        R.string.custom_pattern_saved,
                        recordingState.durationMs / 1000f,
                        recordingState.peakMagnitude
                    )
                recordingState is RecordingState.Error -> recordingState.message
                hasPattern && patternSummary != null ->
                    stringResource(R.string.custom_pattern_ready, patternSummary)
                else -> stringResource(R.string.custom_pattern_none)
            }
            val statusColor =
                if (recordingState is RecordingState.Error)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor
            )
            Button(onClick = onStartRecording, enabled = enabled) {
                Text(
                    stringResource(
                        if (hasPattern) R.string.custom_pattern_rerecord
                        else R.string.custom_pattern_record
                    )
                )
            }
        }

        is RecordingState.Countdown -> {
            Text(
                text = stringResource(
                    R.string.custom_pattern_countdown,
                    recordingState.secondsRemaining
                ),
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedButton(onClick = onCancelRecording) {
                Text(stringResource(R.string.custom_pattern_cancel))
            }
        }

        RecordingState.Recording -> {
            Text(
                text = stringResource(R.string.custom_pattern_recording),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedButton(onClick = onCancelRecording) {
                Text(stringResource(R.string.custom_pattern_cancel))
            }
        }
    }

    Spacer(Modifier.height(4.dp))

    val strictnessLabels = stringArrayResource(R.array.strictness_labels)
    val strictnessClamped = strictness.coerceIn(0, ShakePrefs.STRICTNESS_STEPS - 1)

    Text(
        text = stringResource(R.string.strictness_title),
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        text = strictnessLabels[strictnessClamped],
        style = MaterialTheme.typography.bodyLarge
    )
    Slider(
        value = strictnessClamped.toFloat(),
        onValueChange = {
            onStrictnessChange(it.toInt().coerceIn(0, ShakePrefs.STRICTNESS_STEPS - 1))
        },
        valueRange = 0f..(ShakePrefs.STRICTNESS_STEPS - 1).toFloat(),
        steps = ShakePrefs.STRICTNESS_STEPS - 2,
        enabled = enabled && hasPattern,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = stringResource(R.string.strictness_hint),
        style = MaterialTheme.typography.bodySmall
    )
}
