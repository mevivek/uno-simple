package com.vivek.unosimple.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.engine.models.Card
import kotlin.math.roundToInt

/**
 * Controller for the "fly a card from its origin to the discard pile"
 * animation. Lives at the GameScreen level and is consumed by child
 * composables (hand cards, opponent tiles, the discard pile) via a
 * CompositionLocal.
 *
 * Motion flow:
 *   1. Hand cards + opponent tiles register their on-screen positions as
 *      they layout (via [rememberOriginPos]).
 *   2. The discard pile registers its landing position.
 *   3. When a play happens, the caller invokes [flyFromHand] or
 *      [flyFromOpponent] — the controller queues a [Flight] and the
 *      [CardFlightOverlay] renders a ghost card traveling the path.
 *   4. While a flight is active, [suppressTopDiscard] is true so the real
 *      discard doesn't flash into place before the ghost lands.
 */
class CardFlightController {
    internal val handPositions = mutableStateMapOf<Card, Offset>()
    internal val opponentPositions = mutableStateMapOf<String, Offset>()
    internal var discardPosition by mutableStateOf<Offset?>(null)
    internal val flights = mutableStateListOf<Flight>()

    /** True while any flight is airborne — discard pile hides its top card. */
    val suppressTopDiscard: Boolean get() = flights.isNotEmpty()

    fun setDiscardPos(offset: Offset) { discardPosition = offset }
    fun setHandPos(card: Card, offset: Offset) { handPositions[card] = offset }
    fun setOpponentPos(playerId: String, offset: Offset) { opponentPositions[playerId] = offset }

    /**
     * Animate [playedCard] flying from the human hand's last-known position
     * for that card to the discard pile. Face-up (the player sees it land).
     */
    fun flyFromHand(playedCard: Card) {
        val start = handPositions[playedCard] ?: return
        val end = discardPosition ?: return
        flights += Flight(card = playedCard, start = start, end = end, faceDown = false)
    }

    /**
     * Animate a face-down card from [opponentId]'s tile to the discard pile,
     * then flip to [playedCard] on arrival. We render face-down during the
     * arc to reinforce "the bot just played a mystery card", then the real
     * face is revealed when the pile un-suppresses.
     */
    fun flyFromOpponent(opponentId: String, playedCard: Card) {
        val start = opponentPositions[opponentId] ?: return
        val end = discardPosition ?: return
        flights += Flight(card = playedCard, start = start, end = end, faceDown = true)
    }

    internal fun remove(flight: Flight) { flights.remove(flight) }
}

@Immutable
internal data class Flight(
    val card: Card,
    val start: Offset,
    val end: Offset,
    val faceDown: Boolean,
    val id: Long = nextId++,
) {
    companion object { private var nextId: Long = 0 }
}

val LocalCardFlight = compositionLocalOf<CardFlightController?> { null }

/**
 * Modifier that reports this composable's root-relative position to the
 * flight controller under [key]. Used by both hand cards (key = the Card)
 * and opponent tiles (key = playerId).
 */
@Composable
internal fun Modifier.reportPosition(onPos: (Offset) -> Unit): Modifier {
    return this.onGloballyPositioned { coords: LayoutCoordinates ->
        val topLeft = coords.positionInRoot()
        val centerX = topLeft.x + coords.size.width / 2f
        val centerY = topLeft.y + coords.size.height / 2f
        onPos(Offset(centerX, centerY))
    }
}

/**
 * Full-screen overlay that renders every in-flight card ghost. Must sit
 * above every other GameScreen child (drawn last in the Box) so the
 * animation is always on top.
 *
 * Each flight animates start → end over [FLIGHT_DURATION_MS]ms with a
 * FastOutSlowInEasing, a mid-flight scale bump (card feels "picked up"),
 * and a gentle rotation ramp so it doesn't look rigid.
 */
@Composable
internal fun CardFlightOverlay(controller: CardFlightController) {
    Box(modifier = Modifier.fillMaxSize()) {
        for (flight in controller.flights.toList()) {
            FlyingCard(flight, onDone = { controller.remove(flight) })
        }
    }
}

@Composable
private fun FlyingCard(flight: Flight, onDone: () -> Unit) {
    val progress = remember(flight.id) { Animatable(0f) }
    LaunchedEffect(flight.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = FLIGHT_DURATION_MS, easing = FastOutSlowInEasing),
        )
        onDone()
    }
    val t = progress.value
    // Card width + height in px so we can offset the top-left to center on
    // the captured point.
    val density = LocalDensity.current
    val halfW = with(density) { CARD_WIDTH.toPx() / 2f }
    val halfH = with(density) { CARD_HEIGHT.toPx() / 2f }

    val x = flight.start.x + (flight.end.x - flight.start.x) * t - halfW
    val y = flight.start.y + (flight.end.y - flight.start.y) * t - halfH

    // A tiny parabolic arc so the card rises then settles — purely visual
    // polish. Peaks around the midpoint, max ~30dp lift.
    val archPx = with(density) { (-30).dp.toPx() }
    val arc = (4f * t * (1f - t)) * archPx

    // Scale: 1.0 → 1.15 → 1.0 so the ghost feels momentarily "grabbed".
    val scale = 1f + (0.15f * (1f - kotlin.math.abs(2f * t - 1f)))
    // Rotation: -6° → +2° over the flight, gives personality.
    val rotation = -6f + 8f * t

    // Shadow trail — three fading ghosts behind the flight path, each one
    // lagging further behind. Gives the card weight + motion blur without
    // a real blur shader (which Compose Wasm doesn't always honor).
    val trailPositions = listOf(0.08f, 0.14f, 0.22f)
    trailPositions.forEach { lag ->
        val tLag = (t - lag).coerceAtLeast(0f)
        if (tLag > 0f) {
            val xl = flight.start.x + (flight.end.x - flight.start.x) * tLag - halfW
            val yl = flight.start.y + (flight.end.y - flight.start.y) * tLag - halfH
            val arcL = (4f * tLag * (1f - tLag)) * archPx
            val alphaL = (1f - lag * 3f).coerceAtLeast(0f) * (1f - t * 0.3f).coerceAtLeast(0f) * 0.4f
            Box(
                modifier = Modifier
                    .offset { IntOffset(xl.roundToInt(), (yl + arcL).roundToInt()) }
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = scale * 0.96f
                        scaleY = scale * 0.96f
                        alpha = alphaL
                    },
            ) {
                CardView(
                    card = if (flight.faceDown) null else flight.card,
                    enabled = true,
                    onClick = null,
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), (y + arc).roundToInt()) }
            .graphicsLayer { rotationZ = rotation; scaleX = scale; scaleY = scale },
    ) {
        CardView(
            card = if (flight.faceDown) null else flight.card,
            enabled = true,
            onClick = null,
        )
    }
}

/**
 * Hand cards / opponent tiles read this to know whether to render at full
 * opacity or a lower value while their card is mid-flight. (We currently
 * don't dim anything because the hand has already removed the card from
 * the list by the time the flight starts — kept as a hook for future use.)
 */
private const val FLIGHT_DURATION_MS: Int = 380
