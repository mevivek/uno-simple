package com.vivek.unosimple.ui.game

import androidx.compose.ui.graphics.Color

/**
 * Canonical list of bot characters. Each persona pairs a display name with
 * a distinct illustrated face drawn by [PlayerAvatar] — bots no longer
 * appear as anonymous "Bot 1 / Bot 2 / Bot 3" discs.
 *
 * Mapping from bot id ("bot1", "bot2", …) to persona is deterministic:
 * index `1` → `BOTS[0]`, index `2` → `BOTS[1]`, …, wrapping if there are
 * ever more bots than personas. [GameViewModel.startGame] uses [personaFor]
 * to pick the seat label at round start.
 *
 * Character design is intentionally low-detail — two ears + two eyes + a
 * small mouth drawn on a colored disc — so it renders fast on Skia/Wasm
 * without asset files.
 */
internal data class BotPersona(
    val name: String,
    val faceColor: Color,
    val earColor: Color,
    val earShape: EarShape,
    val eyeStyle: EyeStyle,
    val mouth: Mouth,
    val blush: Boolean,
    val accessory: Accessory,
)

internal enum class EarShape { ROUND, POINTY, ANTENNA, NONE }
internal enum class EyeStyle { ROUND, DOT, WINK, SLEEPY }
internal enum class Mouth { SMILE, SMIRK, O, LINE }
internal enum class Accessory { NONE, BOLT, HEART_CHEEKS, SPOT }

internal val BOTS: List<BotPersona> = listOf(
    BotPersona(
        name = "Mochi",
        faceColor = Color(0xFFFFE4CF),          // cream cat
        earColor = Color(0xFFFFB3C1),
        earShape = EarShape.POINTY,
        eyeStyle = EyeStyle.WINK,
        mouth = Mouth.SMILE,
        blush = true,
        accessory = Accessory.NONE,
    ),
    BotPersona(
        name = "Max",
        faceColor = Color(0xFFE7B77A),          // tan dog
        earColor = Color(0xFF8E5B2A),
        earShape = EarShape.ROUND,
        eyeStyle = EyeStyle.DOT,
        mouth = Mouth.SMIRK,
        blush = false,
        accessory = Accessory.SPOT,
    ),
    BotPersona(
        name = "Kira",
        faceColor = Color(0xFFCBB8E8),          // purple alien
        earColor = Color(0xFF8C6BBF),
        earShape = EarShape.ANTENNA,
        eyeStyle = EyeStyle.ROUND,
        mouth = Mouth.O,
        blush = true,
        accessory = Accessory.NONE,
    ),
    BotPersona(
        name = "Juno",
        faceColor = Color(0xFFFFD48A),          // marigold robot
        earColor = Color(0xFFE89425),
        earShape = EarShape.ANTENNA,
        eyeStyle = EyeStyle.ROUND,
        mouth = Mouth.LINE,
        blush = false,
        accessory = Accessory.BOLT,
    ),
    BotPersona(
        name = "Pip",
        faceColor = Color(0xFF9ED9A6),          // green frog
        earColor = Color(0xFF6CBF7E),
        earShape = EarShape.ROUND,
        eyeStyle = EyeStyle.ROUND,
        mouth = Mouth.SMILE,
        blush = false,
        accessory = Accessory.NONE,
    ),
    BotPersona(
        name = "Milo",
        faceColor = Color(0xFFFF9F6B),          // orange fox
        earColor = Color(0xFFD36A3B),
        earShape = EarShape.POINTY,
        eyeStyle = EyeStyle.SLEEPY,
        mouth = Mouth.SMIRK,
        blush = false,
        accessory = Accessory.NONE,
    ),
    BotPersona(
        name = "Luna",
        faceColor = Color(0xFFC4E6FB),          // blue bunny
        earColor = Color(0xFF9ED1F0),
        earShape = EarShape.POINTY,
        eyeStyle = EyeStyle.DOT,
        mouth = Mouth.SMILE,
        blush = true,
        accessory = Accessory.NONE,
    ),
    BotPersona(
        name = "Bea",
        faceColor = Color(0xFFFFB5A7),          // coral bear
        earColor = Color(0xFFE5826F),
        earShape = EarShape.ROUND,
        eyeStyle = EyeStyle.ROUND,
        mouth = Mouth.SMILE,
        blush = true,
        accessory = Accessory.HEART_CHEEKS,
    ),
    BotPersona(
        name = "Zeph",
        faceColor = Color(0xFFDCCFF2),          // lavender ghost
        earColor = Color(0xFFB59EDB),
        earShape = EarShape.NONE,
        eyeStyle = EyeStyle.SLEEPY,
        mouth = Mouth.O,
        blush = false,
        accessory = Accessory.NONE,
    ),
    // Hidden 10th persona — "Geet". Only shows up in the avatar picker
    // when the Geet easter egg is unlocked (user enters her name in
    // onboarding / profile). A small private surprise.
    BotPersona(
        name = "Geet",
        faceColor = Color(0xFFFFD2E4),          // soft pink
        earColor = Color(0xFFE5596C),           // coral-pink bow
        earShape = EarShape.ROUND,
        eyeStyle = EyeStyle.WINK,
        mouth = Mouth.SMILE,
        blush = true,
        accessory = Accessory.HEART_CHEEKS,
    ),
)

/** Case-insensitive check for the "Geet" easter egg in the user's display name. */
internal fun isGeetName(name: String): Boolean {
    val n = name.trim().lowercase()
    return n == "geet" || n == "gitanjali"
}

/**
 * Deterministic mapping from a bot seat id to a [BotPersona]. IDs follow
 * the pattern `bot1`, `bot2`, … produced by [GameViewModel.startGame]; the
 * trailing number picks the persona. Unknown ids fall back to [BOTS]`[0]`.
 */
internal fun personaFor(botId: String): BotPersona {
    val n = botId.removePrefix("bot").toIntOrNull() ?: return BOTS[0]
    val idx = ((n - 1) % BOTS.size + BOTS.size) % BOTS.size
    return BOTS[idx]
}

/** True if [id] maps to a bot persona (i.e. "bot1", "bot2", …). */
internal fun isBotId(id: String): Boolean = id.startsWith("bot") &&
    id.removePrefix("bot").toIntOrNull() != null
