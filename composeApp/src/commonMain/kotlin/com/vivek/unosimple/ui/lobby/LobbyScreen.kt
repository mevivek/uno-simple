package com.vivek.unosimple.ui.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.GhostButton
import com.vivek.unosimple.ui.theme.LocalClayTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.multiplayer.PlayerSeat
import com.vivek.unosimple.ui.TestTags

/**
 * Lobby for local multiplayer. Pick how many players share the device, give
 * them names, start the round. No network — goes straight into the hotseat
 * game.
 *
 * Pre-fills names as "Player 1", "Player 2", etc. so "Start" is always one
 * tap away.
 */
@Composable
fun LobbyScreen(
    onStart: (seats: List<PlayerSeat>) -> Unit,
    onBack: () -> Unit,
) {
    var playerCount by remember { mutableStateOf(2) }
    val names = remember {
        mutableStateListOf("Player 1", "Player 2", "Player 3", "Player 4")
    }

    Surface(
        modifier = Modifier.fillMaxSize().testTag(TestTags.LOBBY_SCREEN),
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
                text = "PASS & PLAY",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Hand the device around after each turn.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "HOW MANY PLAYERS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (count in 2..4) {
                    CountChip(
                        count = count,
                        selected = playerCount == count,
                        onClick = { playerCount = count },
                        tag = "${TestTags.LOBBY_COUNT_CHIP_PREFIX}$count",
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            for (i in 0 until playerCount) {
                OutlinedTextField(
                    value = names[i],
                    onValueChange = { names[i] = it },
                    label = { Text("Player ${i + 1}", style = MaterialTheme.typography.labelMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${TestTags.LOBBY_NAME_FIELD_PREFIX}$i"),
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.weight(1f))

            ClayButton(
                onClick = {
                    val seats = (0 until playerCount).map { i ->
                        PlayerSeat(id = "p${i + 1}", displayName = names[i].ifBlank { "Player ${i + 1}" })
                    }
                    onStart(seats)
                },
                modifier = Modifier
                    .widthIn(min = 240.dp)
                    .testTag(TestTags.LOBBY_START_BUTTON),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 18.dp),
            ) {
                Text(
                    "START ROUND",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            }

            Spacer(Modifier.height(10.dp))

            GhostButton(
                onClick = onBack,
                modifier = Modifier.widthIn(min = 240.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
            ) {
                Text("BACK", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * Chunky rounded count selector (2 / 3 / 4). Selected = amber slab;
 * unselected = slate panel with a thin stroke. Replaces the Material
 * FilterChip which felt templated against the arcade language.
 */
@Composable
private fun CountChip(count: Int, selected: Boolean, onClick: () -> Unit, tag: String) {
    val stroke = LocalClayTokens.current.strokeColor
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else stroke,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
        )
    }
}
