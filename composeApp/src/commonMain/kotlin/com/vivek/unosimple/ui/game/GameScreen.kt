package com.vivek.unosimple.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivek.unosimple.engine.models.DrawTwoCard
import com.vivek.unosimple.engine.models.ReverseCard
import com.vivek.unosimple.engine.models.SkipCard
import com.vivek.unosimple.engine.models.WildDrawFourCard
import com.vivek.unosimple.ui.theme.FlashOverlay
import com.vivek.unosimple.ui.theme.LocalClayTokens
import com.vivek.unosimple.ui.theme.noiseBackground
import com.vivek.unosimple.ui.theme.rememberFlashController
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import com.vivek.unosimple.ui.TestTags
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vivek.unosimple.audio.LocalAudio
import com.vivek.unosimple.audio.SoundEffect
import com.vivek.unosimple.engine.Rules
import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.models.PlayDirection
import com.vivek.unosimple.engine.models.Player
import com.vivek.unosimple.engine.models.isWild
import com.vivek.unosimple.ui.common.MenuIcon
import com.vivek.unosimple.ui.theme.LocalReducedMotion
import com.vivek.unosimple.viewmodel.GameViewModel
import kotlin.math.PI

/**
 * The live game screen. Phase 3 step B+C: shows opponents, draw + discard
 * piles, active color, turn indicator, and the human player's hand. Tap
 * interactions are wired up so the human can play their cards — bot turns
 * currently only advance when the human touches something (dedicated bot
 * scheduling arrives in step E).
 */
@Composable
fun GameScreen(
    botCount: Int,
    onBackToHome: () -> Unit,
    onOpenRules: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    // The default factory on non-Android targets can't instantiate our
    // ViewModel (UnsupportedOperationException) — provide an explicit
    // factory so lifecycle-viewmodel-compose works on Wasm / Desktop. Tests
    // override this parameter to inject a pre-configured ViewModel.
    vm: GameViewModel = viewModel(factory = GameViewModel.Factory),
) {
    val state by vm.state.collectAsState()

    // Fire startGame once on entry. Try to resume a saved session first so
    // a page reload doesn't wipe the active round; fall through to a fresh
    // deal if nothing is stored.
    LaunchedEffect(botCount) {
        if (vm.state.value == null) {
            if (vm.restoreSavedSession() == null) vm.startGame(botCount)
        }
    }

    // When the human taps a wild we open a color picker before dispatching
    // the play; for a non-wild we play immediately. pendingWild holds the
    // card awaiting a color choice.
    var pendingWild: Card? by remember { mutableStateOf(null) }

    // UNO declaration: the user must tap the UNO button BEFORE playing their
    // second-to-last card, else the engine applies a 2-card penalty. We
    // consume the flag on each play (set back to false) so declaring on
    // one turn doesn't carry into a later one.
    var unoDeclared: Boolean by remember { mutableStateOf(false) }

    // The UI only shows for human-vs-bots mode (humanPlayerId is non-null).
    // All-bots mode uses GameViewModel headlessly in integration tests.
    val humanId = vm.humanPlayerId ?: "p1"

    // Shared flight controller — hand cards, opponent tiles, and the discard
    // pile register their positions here; human plays and bot plays route
    // their played card through the resulting fly-to-pile animation.
    val flight = remember { CardFlightController() }

    val audio = LocalAudio.current
    val flash = rememberFlashController()
    val scope = rememberCoroutineScope()
    var pauseOpen by remember { mutableStateOf(false) }

    // Watch for opponent plays: whenever the discard size grows and the
    // player who just moved wasn't the human, fire a flight from that
    // opponent's tile to the pile.
    var lastDiscardSize by remember { mutableStateOf<Int?>(null) }
    var lastActorId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state?.discardPile?.size) {
        val s = state ?: return@LaunchedEffect
        val size = s.discardPile.size
        val prevSize = lastDiscardSize
        val prevActor = lastActorId
        lastDiscardSize = size
        // Capture the NEXT actor's id for the next round of detection; the
        // "who just played" is whoever was current BEFORE this change, which
        // we saved as prevActor.
        lastActorId = s.currentPlayer.id
        if (prevSize != null && size > prevSize && prevActor != null && prevActor != humanId) {
            val topCard = s.topDiscard
            flight.flyFromOpponent(prevActor, topCard)
            audio.play(SoundEffect.CARD_PLAY)
        }
        // Bolt-flash + error beep on ANY Wild+4 landing (the bomb play). A
        // +2 stack earns a lighter coral tinge; number/skip/reverse get no
        // screen flash because they happen every turn.
        if (prevSize != null && size > prevSize) {
            val top = s.topDiscard
            when (top) {
                is WildDrawFourCard -> {
                    scope.launch { flash.flash(Color.White, peakAlpha = 0.8f, durationMs = 280) }
                    audio.play(SoundEffect.ERROR)
                }
                is DrawTwoCard -> {
                    scope.launch { flash.flash(Color(0xFFFF5168), peakAlpha = 0.35f, durationMs = 320) }
                }
                else -> Unit
            }
        }
    }

    // Play the WIN fanfare exactly once when the round flips to over.
    var lastRoundOver by remember { mutableStateOf(false) }
    LaunchedEffect(state?.isRoundOver) {
        val now = state?.isRoundOver == true
        if (now && !lastRoundOver) audio.play(SoundEffect.WIN)
        lastRoundOver = now
    }

    CompositionLocalProvider(LocalCardFlight provides flight) {
    Surface(
        modifier = Modifier.fillMaxSize().testTag(TestTags.GAME_SCREEN),
        color = MaterialTheme.colorScheme.background,
    ) {
        val s = state
        if (s == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Dealing…", style = MaterialTheme.typography.titleMedium)
            }
            return@Surface
        }

        // Red UNO "felt" — warm red gradient with a brighter spotlight at the
        // center of the table (where the discard pile sits), and a darker
        // vignette at the edges. Matches the iOS / Mattel UNO aesthetic.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0f to Color(0xFFF37A45),   // bright orange-red at center
                        0.35f to Color(0xFFD7301C), // mid red
                        1f to Color(0xFF7A0F0A),    // deep maroon at edges
                        radius = 1200f,
                    )
                )
                .noiseBackground(
                    base = Color.Transparent,
                    speckleColor = Color.White.copy(alpha = 0.03f),
                    density = 0.35f,
                ),
        ) {
            // Giant "UNO" wordmark embossed behind the discard pile. Low-
            // alpha so it reads as table decoration, not live info.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "UNO",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color(0x22FFFFFF),
                    fontWeight = FontWeight.Black,
                    fontSize = 140.sp,
                )
            }
            // Giant curved amber direction arrow wrapping the discard pile.
            BigTableDirectionArrow(direction = s.direction)
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top row: header + opponents
            Column {
                TopBar(
                    state = s,
                    humanId = humanId,
                    onBackToHome = {
                        // Menu tap opens pause overlay rather than quitting
                        // straight. Quit is a button inside the overlay.
                        vm.pause()
                        pauseOpen = true
                    },
                )
                Spacer(Modifier.height(16.dp))
                OpponentRow(state = s, humanId = humanId)
            }

            // Middle: draw + discard piles
            TableCenter(
                state = s,
                isHumanTurn = s.currentPlayer.id == humanId,
                onDraw = {
                    audio.play(SoundEffect.CARD_DEAL)
                    vm.drawCard()
                },
            )

            // Bottom: UNO button (when applicable) + human's hand
            HumanHand(
                state = s,
                humanId = humanId,
                unoDeclared = unoDeclared,
                onDeclareUno = {
                    unoDeclared = true
                    audio.play(SoundEffect.UNO_CALL)
                },
                onCardTap = { card ->
                    if (card.isWild) {
                        pendingWild = card
                    } else {
                        flight.flyFromHand(card)
                        audio.play(SoundEffect.CARD_PLAY)
                        vm.playCard(card, declareUno = unoDeclared)
                        unoDeclared = false // consume the declaration
                    }
                },
                onNewRound = { vm.startGame(botCount) },
            )
        }

        pendingWild?.let { card ->
            WildColorPickerDialog(
                onCancel = { pendingWild = null },
                onPick = { color ->
                    flight.flyFromHand(card)
                    audio.play(SoundEffect.CARD_PLAY)
                    vm.playCard(card, color, declareUno = unoDeclared)
                    unoDeclared = false
                    pendingWild = null
                },
            )
        }

        EffectSpotlight(state = s)
        CelebrationOverlay(visible = s.isRoundOver)
        RoundOverPodium(state = s, onNewRound = { vm.startGame(botCount) })
        CardFlightOverlay(controller = flight)
        FlashOverlay(controller = flash)

        if (pauseOpen) {
            PauseOverlay(
                onResume = {
                    pauseOpen = false
                    vm.resume()
                },
                onOpenRules = {
                    // Pause stays active while viewing rules; resume happens
                    // when the user lands back on the game and taps Resume.
                    onOpenRules()
                },
                onOpenSettings = {
                    onOpenSettings()
                },
                onQuit = {
                    vm.clear()
                    pauseOpen = false
                    onBackToHome()
                },
            )
        }

        // Floating CALL UNO disc anchored bottom-right, above every game
        // layer. Only actionable while the human has exactly 2 cards on
        // their own turn and the round is still live.
        val canDeclareUno = s.currentPlayer.id == humanId &&
            s.players.find { it.id == humanId }?.handSize == 2 &&
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
                    audio.play(SoundEffect.UNO_CALL)
                },
            )
        }
        // Emote corner — small reaction button pinned to the middle of the
        // left edge, in the dead space between the opponent row and the
        // human hand. Keeps it away from the card fan entirely.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            EmoteCorner()
        }
        } // close radial-felt Box
    }
    } // close CompositionLocalProvider
}

@Composable
internal fun WildColorPickerDialog(
    onCancel: () -> Unit,
    onPick: (CardColor) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Choose a color") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                for (c in CardColor.entries) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardColorToCompose(c)),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(
                            onClick = { onPick(c) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = c.name.first().toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

@Composable
internal fun TopBar(
    state: GameState,
    humanId: String,
    onBackToHome: () -> Unit,
) {
    val isHumanTurn = state.currentPlayer.id == humanId
    val header = when {
        state.isRoundOver -> {
            val winner = state.players.find { it.id == state.winnerId }
            if (winner != null) {
                // Round delta = the score the winner just collected. This is
                // the sum of the losers' remaining hand values — same number
                // the engine already added to winner.score this round.
                val delta = state.players
                    .filter { it.id != state.winnerId }
                    .sumOf { it.handScore }
                "${winner.name} won! +$delta"
            } else {
                "Round over"
            }
        }
        isHumanTurn -> "Your turn"
        else -> "${state.currentPlayer.name}'s turn"
    }

    val human = state.players.find { it.id == humanId }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onBackToHome)
                .testTag(TestTags.GAME_MENU_BUTTON),
            contentAlignment = Alignment.Center,
        ) {
            MenuIcon(size = 20.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = header,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .testTag(TestTags.GAME_TURN_HEADER),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (isHumanTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (human != null) {
                ScoreChip(score = human.score)
                Spacer(Modifier.width(8.dp))
            }
            DirectionIndicator(direction = state.direction)
        }
    }
}

/**
 * Small chip at top-right showing the human player's cumulative score. Pops
 * briefly whenever the score increments so a point gain is noticeable.
 */
@Composable
internal fun ScoreChip(score: Int) {
    var lastScore by remember { mutableStateOf(score) }
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(score) {
        if (score > lastScore) {
            scale.animateTo(1.25f, tween(140))
            scale.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
        }
        lastScore = score
    }
    Box(
        modifier = Modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$score",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * Circular arrow indicating current turn direction. Canvas-drawn rather
 * than relying on Unicode `↻` / `↺` which fall back to placeholder boxes
 * in Skia's default Wasm font.
 */
@Composable
internal fun DirectionIndicator(direction: PlayDirection) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(28.dp)) {
        val stroke = 2.5.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f - stroke * 1.5f
        // Draw a 3/4 arc + arrowhead. For counter-clockwise, flip the
        // arrowhead position horizontally.
        val isClockwise = direction == PlayDirection.CLOCKWISE
        // Arc sweeps 270 degrees, leaving a gap at the right (12 o'clock)
        // for the arrowhead.
        val startAngle = if (isClockwise) -120f else -60f
        val sweep = if (isClockwise) 270f else -270f
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        // Arrowhead at the "end" of the arc.
        val headAngleDeg = if (isClockwise) startAngle + sweep else startAngle + sweep
        val headAngleRad = (headAngleDeg * PI / 180).toFloat()
        val tipX = cx + r * kotlin.math.cos(headAngleRad)
        val tipY = cy + r * kotlin.math.sin(headAngleRad)
        val headLen = r * 0.55f
        // Tangent direction at the tip determines arrowhead orientation.
        val tangentDeg = if (isClockwise) headAngleDeg + 90f else headAngleDeg - 90f
        val tangentRad = (tangentDeg * PI / 180).toFloat()
        val baseX = tipX - headLen * kotlin.math.cos(tangentRad)
        val baseY = tipY - headLen * kotlin.math.sin(tangentRad)
        val perpDeg = tangentDeg + 90f
        val perpRad = (perpDeg * PI / 180).toFloat()
        val sideOffsetX = (headLen * 0.35f) * kotlin.math.cos(perpRad)
        val sideOffsetY = (headLen * 0.35f) * kotlin.math.sin(perpRad)
        drawLine(
            color = color,
            start = Offset(tipX, tipY),
            end = Offset(baseX + sideOffsetX, baseY + sideOffsetY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(tipX, tipY),
            end = Offset(baseX - sideOffsetX, baseY - sideOffsetY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun OpponentRow(state: GameState, humanId: String) {
    val opponents = state.players.filter { it.id != humanId }
    val currentId = state.currentPlayer.id
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
    ) {
        for (opp in opponents) {
            OpponentCard(player = opp, isCurrent = opp.id == currentId)
        }
    }
}

@Composable
internal fun OpponentCard(player: Player, isCurrent: Boolean) {
    // Hand-size change feedback:
    //   +1  → a brief "+1" badge pops above the tile (regular draw)
    //   +2+ → cascade shake (they ate a +2 / +4 stack)
    var lastHandSize by remember { mutableStateOf(player.handSize) }
    val shakeX = remember { androidx.compose.animation.core.Animatable(0f) }
    var drewBadgeVisible by remember { mutableStateOf(false) }
    val tileScale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(player.handSize) {
        val prev = lastHandSize
        lastHandSize = player.handSize
        val delta = player.handSize - prev
        when {
            delta == 1 -> {
                drewBadgeVisible = true
                tileScale.animateTo(1.05f, tween(120, easing = FastOutSlowInEasing))
                tileScale.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                delay(900)
                drewBadgeVisible = false
            }
            delta > 1 -> {
                val amplitudes = floatArrayOf(6f, -5f, 4f, -3f, 2f, 0f)
                for (a in amplitudes) {
                    shakeX.animateTo(a, tween(durationMillis = 60, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    val flightCtrl = LocalCardFlight.current
    val activeRing = MaterialTheme.colorScheme.primary
    val inactiveStroke = LocalClayTokens.current.strokeColor
    val tileBg = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .offset(x = shakeX.value.dp)
            .padding(top = 18.dp) // leave room for the thinking bubble above
            .reportPosition { pos -> flightCtrl?.setOpponentPos(player.id, pos) },
    ) {
        Column(
            modifier = Modifier
                .scale(tileScale.value)
                .clip(RoundedCornerShape(16.dp))
                .background(tileBg)
                .drawBehind {
                    // Active-opponent rim: neon-amber stroke around the tile
                    // so it's unmistakable whose turn it is. Inactive tiles
                    // get a faint slate stroke so the silhouette still reads.
                    val stroke = if (isCurrent) 2.5.dp.toPx() else 1.dp.toPx()
                    val ringColor = if (isCurrent) activeRing else inactiveStroke
                    drawRoundRect(
                        color = ringColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style = Stroke(width = stroke),
                    )
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Portrait frame: avatar centered under the name; bigger than
            // the old 44dp so the persona reads at a glance.
            PlayerAvatar(id = player.id, name = player.name, size = 52.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = player.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
            )
            // UNO! pill — when they're down to one card. Neon green so it
            // telegraphs "last card" danger rather than hides as small text.
            if (player.hasCalledUno) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.tertiary)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Text(
                        "UNO!",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            FannedOpponentHand(handSize = player.handSize)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${player.handSize}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black,
                )
                AnimatedVisibility(
                    visible = drewBadgeVisible,
                    enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.4f, animationSpec = tween(180)),
                    exit = fadeOut(tween(220)),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "+1",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                }
            }
        }
        if (isCurrent) {
            Box(
                modifier = Modifier.align(Alignment.TopCenter),
            ) { ThinkingBubble() }
        }
    }
}

@Composable
internal fun TableCenter(
    state: GameState,
    isHumanTurn: Boolean,
    onDraw: () -> Unit,
) {
    // Discard pile (where played cards "collect") sits at the visual center
    // of the row, so the eye naturally goes to the current top card during
    // play. The draw pile (where you pick a fresh card) is offset to the
    // far left with padding between — it's still reachable but doesn't
    // compete with the discard for attention.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Discard — absolute center.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FannedDiscardPile(state = state)
            Spacer(Modifier.height(6.dp))
            ActiveColorSwatch(state.activeColor)
        }
        // Draw pile — left edge of the table row.
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StackedDrawPile(
                remaining = state.drawPile.size,
                enabled = isHumanTurn && !state.isRoundOver,
                onDraw = onDraw,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Draw (${state.drawPile.size})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Stack of 3 face-down cards with gentle offsets. Top card is tappable and
 * shows a subtle bounce on hover/press via the underlying CardView.
 */
@Composable
internal fun StackedDrawPile(
    remaining: Int,
    enabled: Boolean,
    onDraw: () -> Unit,
) {
    // Deeper piles show more back-cards; once the deck runs low we fade them.
    val bottomAlpha = (remaining / 20f).coerceIn(0.2f, 1f)
    val midAlpha = (remaining / 10f).coerceIn(0.4f, 1f)
    Box(
        modifier = Modifier.size(width = CARD_WIDTH + 6.dp, height = CARD_HEIGHT + 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (remaining >= 3) {
            CardView(
                card = null,
                modifier = Modifier
                    .offset(x = 5.dp, y = 5.dp)
                    .alpha(bottomAlpha),
                enabled = true,
                onClick = null,
            )
        }
        if (remaining >= 2) {
            CardView(
                card = null,
                modifier = Modifier
                    .offset(x = 2.5.dp, y = 2.5.dp)
                    .alpha(midAlpha),
                enabled = true,
                onClick = null,
            )
        }
        CardView(
            card = null,
            enabled = enabled,
            onClick = if (enabled) onDraw else null,
        )
    }
}

/**
 * Fanned discard pile — the three most-recent discards layered with slight
 * rotation offsets so the pile feels like real cards on a table.
 *
 * When a new card lands (discard size grows) the new top bounces in with a
 * scale animation; the previously-top card slides back into the fan.
 */
@Composable
internal fun FannedDiscardPile(state: GameState) {
    val pile = state.discardPile
    val top = pile.lastOrNull() ?: return
    val prev = if (pile.size >= 2) pile[pile.size - 2] else null
    val prev2 = if (pile.size >= 3) pile[pile.size - 3] else null
    val flightCtrl = LocalCardFlight.current
    val hideTop = flightCtrl?.suppressTopDiscard == true

    Box(
        modifier = Modifier
            .size(width = CARD_WIDTH + 40.dp, height = CARD_HEIGHT + 14.dp)
            .reportPosition { pos -> flightCtrl?.setDiscardPos(pos) },
        contentAlignment = Alignment.Center,
    ) {
        if (prev2 != null) {
            Box(modifier = Modifier
                .offset(x = (-14).dp, y = 3.dp)
                .rotate(-10f)
                .alpha(0.55f)
            ) { CardView(card = prev2, enabled = true, onClick = null) }
        }
        if (prev != null) {
            Box(modifier = Modifier
                .offset(x = (-6).dp, y = 1.dp)
                .rotate(-4f)
                .alpha(0.80f)
            ) { CardView(card = prev, enabled = true, onClick = null) }
        }
        if (!hideTop) {
            key(pile.size) {
                var landed by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { landed = true }
                val landScale by animateFloatAsState(
                    targetValue = if (landed) 1f else 0.4f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    label = "discard-land",
                )
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (-2).dp)
                        .rotate(4f)
                        .scale(landScale),
                ) { CardView(card = top, enabled = true, onClick = null) }
            }
        }
    }
}

/**
 * Brief full-screen label that flashes up when a Skip / Reverse / +2 / +4
 * lands on the discard. Fades out after ~900ms. Pure visual sugar — the
 * engine has already applied the effect by the time the label shows.
 */
@Composable
internal fun EffectSpotlight(state: GameState) {
    var message by remember { mutableStateOf<String?>(null) }
    var accent by remember { mutableStateOf(Color.Transparent) }
    var lastSize by remember { mutableStateOf(state.discardPile.size) }
    val wildAccent = MaterialTheme.colorScheme.primary

    LaunchedEffect(state.discardPile.size) {
        val prevSize = lastSize
        lastSize = state.discardPile.size
        if (state.discardPile.size <= prevSize) return@LaunchedEffect
        val top = state.topDiscard
        val (label, color) = when (top) {
            is SkipCard -> "SKIP!" to cardColorToCompose(top.color)
            is ReverseCard -> "REVERSE!" to cardColorToCompose(top.color)
            is DrawTwoCard -> "+2!" to cardColorToCompose(top.color)
            WildDrawFourCard -> "+4!" to wildAccent
            else -> null to Color.Transparent
        }
        if (label != null) {
            message = label
            accent = color
            delay(900)
            if (message == label) message = null
        }
    }

    val visible = message != null
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.5f, animationSpec = tween(200)),
            exit = fadeOut(tween(220)) + scaleOut(targetScale = 1.3f, animationSpec = tween(260)),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(accent.copy(alpha = 0.92f))
                    .padding(horizontal = 40.dp, vertical = 22.dp),
            ) {
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
internal fun ActiveColorSwatch(color: CardColor) {
    // Just the color dot — the name was redundant noise.
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(cardColorToCompose(color)),
    )
}

/**
 * Prominent "YOUR TURN" callout above the hand. Pulses gently so it catches
 * the eye when focus shifts back to the player after a bot plays. Only
 * shown while it is in fact the human's turn.
 */
@Composable
internal fun YourTurnBanner() {
    val pulse by rememberInfiniteTransition(label = "your-turn").animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "your-turn-scale",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulse)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "YOUR TURN",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * Soft pulsing glow behind a playable card. Draws a translucent primary-color
 * rounded rect slightly larger than the card, pulsing in opacity so legal
 * plays catch the eye without being visually loud.
 */
@Composable
internal fun LegalGlow(content: @Composable () -> Unit) {
    if (LocalReducedMotion.current) {
        // Static, subtle outline instead of a pulsing halo.
        val glow = MaterialTheme.colorScheme.primary
        Box(modifier = Modifier.drawBehind {
            drawRoundRect(
                color = glow.copy(alpha = 0.3f),
                topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
            )
        }) { content() }
        return
    }
    val pulse by rememberInfiniteTransition(label = "legal-glow").animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "legal-glow-alpha",
    )
    val glowColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.drawBehind {
            drawRoundRect(
                color = glowColor.copy(alpha = pulse * 0.45f),
                topLeft = Offset(-3.dp.toPx(), -3.dp.toPx()),
                size = Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(17.dp.toPx()),
            )
        },
    ) { content() }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun HumanHand(
    state: GameState,
    humanId: String,
    unoDeclared: Boolean,
    onDeclareUno: () -> Unit,
    onCardTap: (Card) -> Unit,
    onNewRound: () -> Unit,
) {
    val human = state.players.find { it.id == humanId } ?: return
    val isHumanTurn = state.currentPlayer.id == humanId
    val legalSet = if (isHumanTurn) {
        Rules.legalPlaysFor(state, human.hand).toSet()
    } else {
        emptySet()
    }

    // Track hand-size growth so we can flash a "+1" on the human's own pill
    // when they (or a +2/+4 penalty aimed at them) pushes a card into hand.
    var humanLastHandSize by remember { mutableStateOf(human.handSize) }
    var humanDrewVisible by remember { mutableStateOf(false) }
    LaunchedEffect(human.handSize) {
        val delta = human.handSize - humanLastHandSize
        humanLastHandSize = human.handSize
        if (delta >= 1) {
            humanDrewVisible = true
            delay(900)
            humanDrewVisible = false
        }
    }

    // Show the UNO button whenever the human holds exactly two cards AND
    // it's their turn. Before that you can't declare (too early); after
    // they play, the indicator moves with them.
    val canDeclareUno = isHumanTurn && human.handSize == 2 && !state.isRoundOver

    Column(modifier = Modifier.fillMaxWidth()) {
        // The banner was too loud; the TopBar "Your turn" text + legal-card
        // glow already signal turn ownership. Keeping this block empty so
        // future subtle indicators (e.g. a thin top-line accent) have a
        // natural insertion point.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Small pill showing hand size — "Your hand (N)" label was too
            // wordy; the number alone inside a rounded badge reads faster.
            // Flashes a "+1" chip next to it when a card lands in hand.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${human.handSize}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AnimatedVisibility(
                    visible = humanDrewVisible,
                    enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.4f, animationSpec = tween(180)),
                    exit = fadeOut(tween(220)),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "+1",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                }
            }
            // Inline UNO button removed — the floating CallUnoDisc bottom-
            // right of the screen is the declaration CTA now. `canDeclareUno`
            // signaling stays here so we can one-day nudge the hand-size
            // pill when declaration is available.
        }
        // FlowRow wraps cards onto a second row when they overflow. Within a
        // row each card fans with a subtle rotation + y-lift (more for cards
        // further from the row center) so the hand reads as a held arc
        // rather than a flat strip.
        val n = human.hand.size
        val mid = (n - 1) / 2f
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val flightCtrl = LocalCardFlight.current
            for ((index, card) in human.hand.withIndex()) {
                val isLegal = card in legalSet
                val offsetFromMid = (index - mid)
                val rotationDeg = (offsetFromMid * 3.5f).coerceIn(-18f, 18f)
                val yLiftDp = (offsetFromMid * offsetFromMid * 1.4f).coerceAtMost(14f)
                AnimatedCardEntry(index = index) {
                    Box(
                        modifier = Modifier
                            .offset(y = yLiftDp.dp)
                            .rotate(rotationDeg)
                            .reportPosition { pos ->
                                flightCtrl?.setHandPos(card, pos)
                            },
                    ) {
                        if (isLegal) {
                            LegalGlow {
                                CardView(
                                    card = card,
                                    enabled = true,
                                    onClick = { onCardTap(card) },
                                )
                            }
                        } else {
                            CardView(card = card, enabled = false, onClick = null)
                        }
                    }
                }
            }
        }
        if (!state.isRoundOver && !isHumanTurn) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Waiting for ${state.currentPlayer.name}…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        if (state.isRoundOver) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onNewRound,
                colors = ButtonDefaults.buttonColors(),
            ) { Text("New round") }
        }
    }
}
