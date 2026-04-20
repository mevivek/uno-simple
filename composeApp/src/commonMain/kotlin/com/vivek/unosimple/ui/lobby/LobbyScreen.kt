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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
                text = "Local multiplayer",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Pass the device after each turn.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(28.dp))

            Text("Players", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (count in 2..4) {
                    FilterChip(
                        selected = playerCount == count,
                        onClick = { playerCount = count },
                        label = { Text("$count") },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(),
                        modifier = Modifier.testTag("${TestTags.LOBBY_COUNT_CHIP_PREFIX}$count"),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            for (i in 0 until playerCount) {
                OutlinedTextField(
                    value = names[i],
                    onValueChange = { names[i] = it },
                    label = { Text("Player ${i + 1}") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${TestTags.LOBBY_NAME_FIELD_PREFIX}$i"),
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val seats = (0 until playerCount).map { i ->
                        PlayerSeat(id = "p${i + 1}", displayName = names[i].ifBlank { "Player ${i + 1}" })
                    }
                    onStart(seats)
                },
                modifier = Modifier
                    .widthIn(min = 220.dp)
                    .testTag(TestTags.LOBBY_START_BUTTON),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text("Start round", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.widthIn(min = 220.dp),
                shape = RoundedCornerShape(50),
            ) { Text("Back") }
        }
    }
}
