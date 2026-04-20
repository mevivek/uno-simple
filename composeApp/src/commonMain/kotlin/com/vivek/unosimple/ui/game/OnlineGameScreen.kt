package com.vivek.unosimple.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.isWild
import com.vivek.unosimple.multiplayer.GameSyncService
import com.vivek.unosimple.multiplayer.PlayerSeat
import com.vivek.unosimple.viewmodel.OnlineGameViewModel

/**
 * Online multiplayer game surface. Each player runs this on their own
 * device; they always see their own hand (never anyone else's), so no
 * pass-device overlay. Host sees a "Start round" CTA in the pre-round
 * waiting state; guests see "Waiting for host…".
 *
 * Architecturally mirrors [HotseatGameScreen] but with `visibleHumanId =
 * vm.myId` and a waiting-room phase for when the host hasn't dealt yet.
 */
@Composable
fun OnlineGameScreen(
    vm: OnlineGameViewModel,
    isHost: Boolean,
    roomCode: String,
    onBackToHome: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val players by vm.sync.players.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        val s = state
        // Before the round starts, show a room lobby where everyone watches
        // the seat list populate. Only the host sees the Start button.
        if (s == null) {
            OnlineWaitingRoom(
                roomCode = roomCode,
                players = players,
                isHost = isHost,
                onStart = { vm.startRound(players, handSize = 7) },
                onBackToHome = onBackToHome,
            )
            return@Surface
        }

        var pendingWild: Card? by remember { mutableStateOf(null) }
        var unoDeclared: Boolean by remember { mutableStateOf(false) }

        val visibleHumanId = vm.myId

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                TopBar(state = s, humanId = visibleHumanId, onBackToHome = onBackToHome)
                Spacer(Modifier.height(16.dp))
                OpponentRow(state = s, humanId = visibleHumanId)
            }

            TableCenter(
                state = s,
                isHumanTurn = s.currentPlayer.id == visibleHumanId,
                onDraw = { vm.drawCard() },
            )

            HumanHand(
                state = s,
                humanId = visibleHumanId,
                unoDeclared = unoDeclared,
                onDeclareUno = { unoDeclared = true },
                onCardTap = { card ->
                    if (card.isWild) {
                        pendingWild = card
                    } else {
                        vm.playCard(card, chosenColor = null, declareUno = unoDeclared)
                        unoDeclared = false
                    }
                },
                onNewRound = {
                    if (isHost) vm.startRound(players, handSize = 7)
                },
            )
        }

        EffectSpotlight(state = s)
        RoundOverPodium(
            state = s,
            onNewRound = { if (isHost) vm.startRound(players, handSize = 7) },
        )

        pendingWild?.let { card ->
            WildColorPickerDialog(
                onCancel = { pendingWild = null },
                onPick = { color ->
                    vm.playCard(card, chosenColor = color, declareUno = unoDeclared)
                    unoDeclared = false
                    pendingWild = null
                },
            )
        }

        CelebrationOverlay(visible = s.isRoundOver)
    }
}

/**
 * Waiting room shown before the host starts the first round. Displays the
 * room code (so others can join) and the seat list as it fills in. The
 * host sees a Start button; guests just watch.
 */
@Composable
private fun OnlineWaitingRoom(
    roomCode: String,
    players: List<PlayerSeat>,
    isHost: Boolean,
    onStart: () -> Unit,
    onBackToHome: () -> Unit,
) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Big tap-to-copy chip showing the room code + copy icon.
        com.vivek.unosimple.ui.theme.ClaySurface(
            color = MaterialTheme.colorScheme.primary,
            cornerRadius = 24.dp,
            elevation = 12.dp,
        ) {
            Row(
                modifier = Modifier
                    .clickable {
                        com.vivek.unosimple.platform.Clipboard.writeText(roomCode)
                        copied = true
                    }
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    roomCode,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(16.dp))
                com.vivek.unosimple.ui.common.CopyIcon(
                    size = 24.dp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (copied) "Copied!" else "Tap to copy",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (copied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        // Seat list as avatar chips.
        if (players.isEmpty()) {
            Text(
                "Waiting for players…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                players.forEach { p ->
                    SeatChip(seat = p)
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        if (isHost) {
            com.vivek.unosimple.ui.theme.ClayButton(
                onClick = onStart,
                enabled = players.size in 2..10,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            ) {
                Text(
                    "Start round",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
            }
        } else {
            Text(
                "Waiting for host…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onBackToHome)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text("Leave", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SeatChip(seat: com.vivek.unosimple.multiplayer.PlayerSeat) {
    com.vivek.unosimple.ui.theme.ClaySurface(
        color = MaterialTheme.colorScheme.surface,
        cornerRadius = 20.dp,
        elevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.vivek.unosimple.ui.game.PlayerAvatar(
                id = seat.id,
                name = seat.displayName,
                size = 28.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                seat.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
