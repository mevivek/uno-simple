package com.vivek.unosimple.engine.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CardColor { RED, YELLOW, GREEN, BLUE }

/**
 * A classic Mattel UNO card. `color` is null only for wild cards at rest —
 * once a wild is played, the chosen color lives on [com.vivek.unosimple.engine.models.GameState.activeColor],
 * not on the card instance.
 */
@Serializable
sealed interface Card {
    val color: CardColor?
}

@Serializable
@SerialName("number")
data class NumberCard(override val color: CardColor, val number: Int) : Card {
    init {
        require(number in 0..9) { "UNO number cards go 0..9, got $number" }
    }
}

@Serializable
@SerialName("skip")
data class SkipCard(override val color: CardColor) : Card

@Serializable
@SerialName("reverse")
data class ReverseCard(override val color: CardColor) : Card

@Serializable
@SerialName("drawTwo")
data class DrawTwoCard(override val color: CardColor) : Card

@Serializable
@SerialName("wild")
data object WildCard : Card {
    override val color: CardColor? get() = null
}

@Serializable
@SerialName("wildDrawFour")
data object WildDrawFourCard : Card {
    override val color: CardColor? get() = null
}

/** True if this card has no intrinsic color (Wild / Wild Draw Four). */
val Card.isWild: Boolean
    get() = this is WildCard || this is WildDrawFourCard

/**
 * Classic UNO scoring per Mattel rules: numbered cards = face value,
 * action cards (Skip / Reverse / Draw Two) = 20, wild cards = 50.
 */
val Card.scoreValue: Int
    get() = when (this) {
        is NumberCard -> number
        is SkipCard, is ReverseCard, is DrawTwoCard -> 20
        WildCard, WildDrawFourCard -> 50
    }
