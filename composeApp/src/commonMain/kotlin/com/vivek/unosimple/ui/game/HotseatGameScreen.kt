package com.vivek.unosimple.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.isWild
import com.vivek.unosimple.ui.TestTags
import com.vivek.unosimple.viewmodel.HotseatGameViewModel

/**
 * Local multiplayer game surface. Wraps the same game widgets the solo
 * GameScreen uses (TopBar / OpponentRow / TableCenter / HumanHand), but
 * driven by [HotseatGameViewModel] so multiple humans take turns on one
 * device.
 *
 * The core UX addition is the [PassDeviceOverlay] — a full-screen opaque
 * prompt shown between turns so each player can't peek at the next player's
 * hand. The underlying game state keeps advancing normally; the overlay
 * just gates when the hand becomes visible.
 */
@Composable
fun HotseatGameScreen(
    vm: HotseatGameViewModel,
    onBackToHome: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val phase by vm.phase.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize().testTag(TestTags.HOTSEAT_SCREEN),
        color = MaterialTheme.colorScheme.background,
    ) {
        val s = state ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Dealing…", style = MaterialTheme.typography.titleMedium)
            }
            return@Surface
        }

        // The currently-visible seat depends on phase:
        // - Handoff: the game surface is hidden behind the overlay, so the
        //   visible-seat choice doesn't matter, but we still render behind
        //   so the transition doesn't flash.
        // - PlayerTurn: current player's hand shows.
        // - RoundOver: we show the current player's view (winner's hand is
        //   empty, but we keep state stable).
        val visibleHumanId = s.currentPlayer.id

        // Mirror the UNO flag pattern in single-player: consume on play.
        var unoDeclared: Boolean by remember { mutableStateOf(false) }
        var pendingWild: Card? by remember { mutableStateOf(null) }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                TopBar(
                    state = s,
                    humanId = visibleHumanId,
                    onBackToHome = onBackToHome,
                )
                Spacer(Modifier.height(16.dp))
                OpponentRow(state = s, humanId = visibleHumanId)
            }

            TableCenter(
                state = s,
                isHumanTurn = phase is HotseatGameViewModel.Phase.PlayerTurn,
                onDraw = {
                    vm.drawCard()
                    unoDeclared = false
                },
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
                onNewRound = { vm.startNewRound() },
            )
        }

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

        EffectSpotlight(state = s)
        RoundOverPodium(state = s, onNewRound = { vm.startNewRound() })

        // Handoff overlay sits above the game. The game below keeps
        // collecting state but is hidden, so when the player taps Ready
        // the transition is instant.
        when (val p = phase) {
            is HotseatGameViewModel.Phase.Handoff -> PassDeviceOverlay(
                nextPlayerName = p.nextSeat.displayName,
                onReady = vm::acknowledgeHandoff,
            )
            is HotseatGameViewModel.Phase.PlayerTurn, HotseatGameViewModel.Phase.RoundOver -> {
                // no overlay
            }
        }

        CelebrationOverlay(visible = s.isRoundOver)
    }
}

@Composable
private fun PassDeviceOverlay(
    nextPlayerName: String,
    onReady: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(TestTags.HOTSEAT_PASS_OVERLAY),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Pass the device",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = nextPlayerName,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onReady,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(),
                modifier = Modifier.testTag(TestTags.HOTSEAT_READY_BUTTON),
            ) {
                Text(
                    "Ready",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
