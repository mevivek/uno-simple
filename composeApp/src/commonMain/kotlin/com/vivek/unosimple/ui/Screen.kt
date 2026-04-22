package com.vivek.unosimple.ui

/**
 * Top-level UI destinations. Kept as a tiny sealed hierarchy for now; a proper
 * `androidx.navigation.compose` graph will slot in when we grow to enough
 * screens to justify it (settings, rules, stats, post-round).
 */
sealed interface Screen {
    data object Splash : Screen
    data object Onboarding : Screen
    data object Home : Screen
    data object Settings : Screen
    data object Profile : Screen
    data object AvatarPicker : Screen
    data object Rules : Screen
    data object About : Screen
    data object Stats : Screen
    data object Achievements : Screen
    data object Friends : Screen
    data object Admin : Screen
    data object Lobby : Screen
    data object OnlineLobby : Screen

    /** @property botCount number of AI opponents (1..3). Human is always player 0. */
    data class Game(val botCount: Int) : Screen

    /** Local-multiplayer hotseat game with 2-4 humans on a single device. */
    data class Hotseat(val seats: List<com.vivek.unosimple.multiplayer.PlayerSeat>) : Screen

    /**
     * Online multiplayer via Firebase. [isHost] means this client generated
     * the [roomCode]; [displayName] is what opponents see on the table.
     */
    data class Online(
        val displayName: String,
        val roomCode: String,
        val isHost: Boolean,
    ) : Screen
}
