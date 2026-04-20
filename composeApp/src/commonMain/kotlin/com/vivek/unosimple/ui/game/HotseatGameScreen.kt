package com.vivek.unosimple.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.LocalClayTokens
import com.vivek.unosimple.ui.theme.noiseBackground
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

        // Same dark felt as solo GameScreen so the mode shift doesn't feel
        // like a different app.
        val bg = MaterialTheme.colorScheme.background
        val spotlight = LocalClayTokens.current.tableAccent
        Box(
            modifier = Modifier
                .fillMaxSize()
                .noiseBackground(base = bg, density = 0.45f)
                .background(
                    Brush.radialGradient(
                        0f to spotlight.copy(alpha = 0.55f),
                        0.45f to Color.Transparent,
                        1f to Color.Transparent,
                        radius = 700f,
                    )
                ),
        ) {

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
        // the transition is instant. Entrance + exit animate so the
        // pass-the-device moment feels ceremonial, not abrupt.
        val isHandoff = phase is HotseatGameViewModel.Phase.Handoff
        AnimatedVisibility(
            visible = isHandoff,
            enter = fadeIn(tween(200)) + slideInVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                initialOffsetY = { it / 3 },
            ),
            exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { -it / 3 }),
        ) {
            val nextName = (phase as? HotseatGameViewModel.Phase.Handoff)?.nextSeat?.displayName ?: ""
            PassDeviceOverlay(
                nextPlayerName = nextName,
                onReady = vm::acknowledgeHandoff,
            )
        }

        CelebrationOverlay(visible = s.isRoundOver)
        } // close felt Box
    }
}

@Composable
private fun PassDeviceOverlay(
    nextPlayerName: String,
    onReady: () -> Unit,
) {
    val spotlight = LocalClayTokens.current.tableAccent
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.radialGradient(
                    0f to spotlight.copy(alpha = 0.5f),
                    0.5f to Color.Transparent,
                    1f to Color.Transparent,
                    radius = 600f,
                    center = Offset(600f, 800f),
                )
            )
            .testTag(TestTags.HOTSEAT_PASS_OVERLAY),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "PASS THE DEVICE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = nextPlayerName,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "is up next",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(20.dp))
            ClayButton(
                onClick = onReady,
                modifier = Modifier.testTag(TestTags.HOTSEAT_READY_BUTTON),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 18.dp),
            ) {
                Text(
                    "I'M READY",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
