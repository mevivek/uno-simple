package com.vivek.unosimple.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
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
import com.vivek.unosimple.ui.theme.noiseBackground
import kotlinx.coroutines.launch

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
    val connectionState by vm.sync.connectionState.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

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
                connectionState = connectionState,
                myId = vm.myId,
            )
            return@Surface
        }

        var pendingWild: Card? by remember { mutableStateOf(null) }
        var unoDeclared: Boolean by remember { mutableStateOf(false) }

        val visibleHumanId = vm.myId
        val audio = com.vivek.unosimple.audio.LocalAudio.current
        val flight = remember { CardFlightController() }
        val flash = com.vivek.unosimple.ui.theme.rememberFlashController()

        // Watch for opponent plays + action-card hits (same logic as solo).
        var lastDiscardSize by remember { mutableStateOf<Int?>(null) }
        var lastActorId by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(s.discardPile.size) {
            val size = s.discardPile.size
            val prevSize = lastDiscardSize
            val prevActor = lastActorId
            lastDiscardSize = size
            lastActorId = s.currentPlayer.id
            if (prevSize != null && size > prevSize && prevActor != null && prevActor != visibleHumanId) {
                flight.flyFromOpponent(prevActor, s.topDiscard)
                audio.play(com.vivek.unosimple.audio.SoundEffect.CARD_PLAY)
            }
            if (prevSize != null && size > prevSize) {
                val top = s.topDiscard
                when (top) {
                    is com.vivek.unosimple.engine.models.WildDrawFourCard -> {
                        scope.launch { flash.flash(androidx.compose.ui.graphics.Color.White, 0.8f, 280) }
                        audio.play(com.vivek.unosimple.audio.SoundEffect.ERROR)
                    }
                    is com.vivek.unosimple.engine.models.DrawTwoCard -> {
                        scope.launch { flash.flash(androidx.compose.ui.graphics.Color(0xFFFF5168), 0.35f, 320) }
                    }
                    else -> Unit
                }
            }
        }

        var lastRoundOver by remember { mutableStateOf(false) }
        LaunchedEffect(s.isRoundOver) {
            val now = s.isRoundOver
            if (now && !lastRoundOver) audio.play(com.vivek.unosimple.audio.SoundEffect.WIN)
            lastRoundOver = now
        }

        androidx.compose.runtime.CompositionLocalProvider(LocalCardFlight provides flight) {
        // Red UNO felt — same gradient + noise texture as solo GameScreen.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        0f to androidx.compose.ui.graphics.Color(0xFFF37A45),
                        0.35f to androidx.compose.ui.graphics.Color(0xFFD7301C),
                        1f to androidx.compose.ui.graphics.Color(0xFF7A0F0A),
                        radius = 1200f,
                    )
                )
                .noiseBackground(
                    base = androidx.compose.ui.graphics.Color.Transparent,
                    speckleColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.03f),
                    density = 0.35f,
                ),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "UNO",
                    style = MaterialTheme.typography.displayLarge,
                    color = androidx.compose.ui.graphics.Color(0x22FFFFFF),
                    fontWeight = FontWeight.Black,
                    fontSize = androidx.compose.ui.unit.TextUnit(140f, androidx.compose.ui.unit.TextUnitType.Sp),
                )
            }
            BigTableDirectionArrow(direction = s.direction)

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
                onDraw = {
                    audio.play(com.vivek.unosimple.audio.SoundEffect.CARD_DEAL)
                    vm.drawCard()
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
                        flight.flyFromHand(card)
                        audio.play(com.vivek.unosimple.audio.SoundEffect.CARD_PLAY)
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

        // Connection badge overlay — top-right corner, always visible so
        // a mid-game drop is obvious.
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 10.dp, end = 10.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            com.vivek.unosimple.ui.common.ConnectionBadge(state = connectionState)
        }

        // Emote feed — incoming reactions from every player in the room
        // render as transient chat bubbles stacked near the top.
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 50.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            EmoteFeed(events = vm.sync.emoteEvents, players = players)
        }

        // Self-emote corner — right-middle, broadcasts to the room.
        Box(
            modifier = Modifier.fillMaxSize().padding(end = 10.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            EmoteCorner(onBroadcast = { reaction ->
                scope.launch { vm.sync.broadcastEmote(reaction.name) }
            })
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

        CelebrationOverlay(visible = s.isRoundOver)

        // Card-flight + flash overlays layered over the felt.
        CardFlightOverlay(controller = flight)
        com.vivek.unosimple.ui.theme.FlashOverlay(controller = flash)

        // CALL UNO disc — same logic as solo.
        val canDeclareUno = s.currentPlayer.id == visibleHumanId &&
            s.players.find { it.id == visibleHumanId }?.handSize == 2 &&
            !s.isRoundOver
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 18.dp, bottom = 130.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            CallUnoDisc(
                visible = canDeclareUno,
                declared = unoDeclared,
                onTap = {
                    unoDeclared = true
                    audio.play(com.vivek.unosimple.audio.SoundEffect.UNO_CALL)
                },
            )
        }
        } // close red-felt Box
        } // close CompositionLocalProvider(LocalCardFlight)
    }
}

/**
 * Waiting room shown before the host starts the first round. Displays the
 * room code in big digit boxes, the seat list populating in real-time,
 * and a context-aware CTA (host = Start round, guest = "waiting" pulse).
 * A live connection badge in the top-right signals sync health.
 */
@Composable
private fun OnlineWaitingRoom(
    roomCode: String,
    players: List<PlayerSeat>,
    isHost: Boolean,
    onStart: () -> Unit,
    onBackToHome: () -> Unit,
    connectionState: com.vivek.unosimple.multiplayer.ConnectionState =
        com.vivek.unosimple.multiplayer.ConnectionState.Connected,
    myId: String = "",
) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 60.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                "ROOM CODE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(10.dp))

            // Big digit boxes — one per character in the room code. Click
            // anywhere on the row to copy to clipboard.
            Row(
                modifier = Modifier.clickable {
                    com.vivek.unosimple.platform.Clipboard.writeText(roomCode)
                    copied = true
                },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                roomCode.forEach { c ->
                    CodeDigitBox(digit = c.toString())
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.clickable {
                    com.vivek.unosimple.platform.Clipboard.writeText(roomCode)
                    copied = true
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.vivek.unosimple.ui.common.CopyIcon(
                    size = 14.dp,
                    color = if (copied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (copied) "COPIED!" else "TAP TO COPY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = if (copied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(32.dp))

            // Seat-count header + seat list.
            Text(
                "${players.size} OF 4 SEATS FILLED",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(14.dp))
            if (players.isEmpty()) {
                Text(
                    "Nobody here yet \u2014 share the code above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    players.forEach { p -> SeatChip(seat = p, isMe = p.id == myId) }
                }
            }

            Spacer(Modifier.height(40.dp))

            if (isHost) {
                val enoughPlayers = players.size >= 2
                com.vivek.unosimple.ui.theme.ClayButton(
                    onClick = onStart,
                    enabled = enoughPlayers,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    contentPadding = PaddingValues(horizontal = 40.dp, vertical = 18.dp),
                ) {
                    Text(
                        if (enoughPlayers) "START ROUND" else "NEED 1 MORE PLAYER",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
            } else {
                // Pulsing "waiting for host" text so the guest knows the
                // screen is live, not frozen.
                val alpha by rememberInfiniteTransition(label = "guest-wait").animateFloat(
                    initialValue = 0.55f,
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = androidx.compose.animation.core.tween(
                            durationMillis = 900,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing,
                        ),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                    ),
                    label = "guest-wait-alpha",
                )
                Text(
                    "WAITING FOR HOST\u2026",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                )
            }

            Spacer(Modifier.height(20.dp))
            com.vivek.unosimple.ui.theme.GhostButton(
                onClick = onBackToHome,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("LEAVE ROOM", style = MaterialTheme.typography.titleSmall)
            }
        }

        // Connection badge — top-right corner, always visible.
        com.vivek.unosimple.ui.common.ConnectionBadge(
            state = connectionState,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        )
    }
}

/**
 * A single big digit box (or character slot) in the room-code hero. Panel-
 * styled with the code character centered; sized big enough to read across
 * a room when someone is asking the host to share the code.
 */
@Composable
private fun CodeDigitBox(digit: String) {
    com.vivek.unosimple.ui.theme.ClaySurface(
        color = MaterialTheme.colorScheme.primary,
        cornerRadius = 14.dp,
        elevation = 10.dp,
    ) {
        Box(
            modifier = Modifier.size(width = 58.dp, height = 76.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                digit,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun SeatChip(seat: com.vivek.unosimple.multiplayer.PlayerSeat, isMe: Boolean = false) {
    com.vivek.unosimple.ui.theme.ClaySurface(
        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        cornerRadius = 20.dp,
        elevation = if (isMe) 8.dp else 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerAvatar(id = seat.id, name = seat.displayName, size = 28.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                seat.displayName + if (isMe) " (YOU)" else "",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
