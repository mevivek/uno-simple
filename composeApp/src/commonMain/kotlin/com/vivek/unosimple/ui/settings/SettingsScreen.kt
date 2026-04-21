package com.vivek.unosimple.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.vivek.unosimple.ui.theme.ClaySurface
import com.vivek.unosimple.ui.theme.GhostButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.settings.SettingsRepository
import com.vivek.unosimple.ui.TestTags
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    onOpenProfile: () -> Unit = {},
    onOpenRules: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onDone: () -> Unit,
) {
    val current by settings.state.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize().testTag(TestTags.SETTINGS_SCREEN),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(32.dp))

            // Feedback group — sound, haptics, reduced motion in one dark panel.
            SettingsGroup(title = "FEEDBACK") {
                ToggleRow(
                    label = "Sound effects",
                    checked = current.soundEnabled,
                    onChange = settings::setSoundEnabled,
                    switchTag = TestTags.SETTINGS_SOUND_TOGGLE,
                )
                Divider()
                ToggleRow(
                    label = "Haptics",
                    checked = current.hapticsEnabled,
                    onChange = settings::setHapticsEnabled,
                    switchTag = TestTags.SETTINGS_HAPTICS_TOGGLE,
                )
                Divider()
                ToggleRow(
                    label = "Reduced motion",
                    checked = current.reducedMotion,
                    onChange = settings::setReducedMotion,
                    switchTag = "settings.reduced_motion",
                )
            }

            Spacer(Modifier.height(16.dp))

            SettingsGroup(title = "TIMING") {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Animation speed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = formatSpeed(current.animationSpeedMultiplier),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = current.animationSpeedMultiplier,
                        onValueChange = settings::setAnimationSpeed,
                        valueRange = 0.25f..2.0f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.SETTINGS_SPEED_SLIDER),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsGroup(title = "ACCOUNT") {
                LinkRow(label = "Profile", chevronText = "Edit \u203A", onClick = onOpenProfile)
            }

            Spacer(Modifier.height(16.dp))

            SettingsGroup(title = "INFO") {
                LinkRow(label = "How to play", chevronText = "Rules \u203A", onClick = onOpenRules)
                Divider()
                LinkRow(label = "About", chevronText = "v${com.vivek.unosimple.BuildInfo.BUILD_STAMP} \u203A", onClick = onOpenAbout)
            }

            Spacer(Modifier.weight(1f))

            GhostButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.SETTINGS_BACK_BUTTON),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    "BACK",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    switchTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.testTag(switchTag),
        )
    }
}

/**
 * Grouped settings section — label + dark panel containing the rows.
 * Matches the iOS / Android settings convention of "SECTION HEADER" above
 * a raised card.
 */
@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        ClaySurface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            cornerRadius = 14.dp,
            elevation = 6.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun Divider() {
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        thickness = 1.dp,
    )
}

/**
 * Tappable row with a label on the left and a small chevron / hint on the
 * right. Used for Profile + How-to-play + About in the settings list.
 */
@Composable
private fun LinkRow(label: String, chevronText: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            chevronText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatSpeed(multiplier: Float): String {
    val rounded = (multiplier * 100).roundToInt() / 100f
    return when {
        rounded < 0.95f -> "${rounded}x — faster"
        rounded > 1.05f -> "${rounded}x — slower"
        else -> "${rounded}x — default"
    }
}
