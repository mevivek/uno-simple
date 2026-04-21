package com.vivek.unosimple.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.GhostButton

/**
 * Full-screen pause modal. The caller gates visibility; this composable
 * just renders the panel with four options:
 *
 *   RESUME — dismisses and unfreezes the bot loop
 *   RULES  — routes to the Rules screen
 *   SETTINGS — routes to the Settings screen
 *   QUIT TO HOME — clears the round and goes back to Home
 *
 * Panel sits on top of a dark scrim so the game state behind it is hidden
 * but still partly visible (anti-peek).
 */
@Composable
internal fun PauseOverlay(
    onResume: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .testTag("pause_overlay"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 280.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "PAUSED",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))

            ClayButton(
                onClick = onResume,
                modifier = Modifier.fillMaxWidth().testTag("pause.resume"),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            ) {
                Text(
                    "RESUME",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            }
            GhostButton(
                onClick = onOpenRules,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
            ) { Text("HOW TO PLAY", style = MaterialTheme.typography.titleMedium) }
            GhostButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
            ) { Text("SETTINGS", style = MaterialTheme.typography.titleMedium) }
            GhostButton(
                onClick = onQuit,
                modifier = Modifier.fillMaxWidth().testTag("pause.quit"),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
            ) {
                Text(
                    "QUIT TO HOME",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
