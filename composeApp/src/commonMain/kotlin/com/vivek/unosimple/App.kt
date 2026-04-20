package com.vivek.unosimple

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.vivek.unosimple.audio.LocalAudio
import com.vivek.unosimple.audio.createAudioService
import com.vivek.unosimple.multiplayer.PlayerSeat
import com.vivek.unosimple.multiplayer.createFirebaseSyncService
import com.vivek.unosimple.multiplayer.firebaseSupported
import com.vivek.unosimple.settings.SettingsRepository
import com.vivek.unosimple.ui.Screen
import com.vivek.unosimple.ui.adaptive.ProvideWindowSizeClass
import com.vivek.unosimple.ui.game.GameScreen
import com.vivek.unosimple.ui.game.HotseatGameScreen
import com.vivek.unosimple.ui.game.OnlineGameScreen
import com.vivek.unosimple.ui.home.HomeScreen
import com.vivek.unosimple.ui.lobby.LobbyScreen
import com.vivek.unosimple.ui.lobby.OnlineLobbyScreen
import com.vivek.unosimple.ui.settings.SettingsScreen
import com.vivek.unosimple.ui.theme.UnoTheme
import com.vivek.unosimple.viewmodel.GameViewModel
import com.vivek.unosimple.viewmodel.HotseatGameViewModel
import com.vivek.unosimple.viewmodel.OnlineGameViewModel

/**
 * Minimal [ViewModelStoreOwner] for non-Android Compose Multiplatform
 * targets (Wasm / Desktop / iOS). Android's Activity / Fragment provide an
 * owner via view-tree lifecycle, but there is no analogous thing outside
 * the Android world — `viewModel()` throws "No ViewModelStoreOwner was
 * provided via LocalViewModelStoreOwner" without this shim.
 */
private class AppViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
    fun dispose() = viewModelStore.clear()
}

/**
 * Fallback shown on Wasm when the user enters the Online lobby —
 * dev.gitlive:firebase-* doesn't publish Wasm variants, so Online
 * multiplayer is Android / iOS / Desktop only for now. Native-Wasm
 * Firebase support will swap this out with the real game screen.
 */
@Composable
private fun OnlineNotSupportedScreen(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Online multiplayer isn't available on web yet.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "The Firebase Kotlin/Wasm variant doesn't exist yet. Use the Android or Desktop build to play online.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp),
            )
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.padding(top = 24.dp),
            ) { Text("Back") }
        }
    }
}

@Composable
fun App() {
    UnoTheme {
        // Install a ViewModelStoreOwner for the duration of the app. On
        // Android this is unused (Activity/Fragment already provide one);
        // on Wasm/Desktop/iOS it's the only owner that exists.
        val owner = remember { AppViewModelStoreOwner() }
        DisposableEffect(owner) { onDispose { owner.dispose() } }

        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
            // Root adaptive provider. Screens read the current size class via
            // `LocalWindowSizeClass.current` or `currentWindowSizeClass()`. For
            // now every screen renders the Compact layout regardless — the
            // branching skeleton is in place so Medium/Expanded refinements
            // can be added without churning the app shell. See
            // `.claude/memory/feedback_adaptive_ui.md` for the full rule.
            ProvideWindowSizeClass {
                // Single app-wide settings store. In-memory for now; swap for
                // a persistent impl (DataStore / NSUserDefaults / localStorage)
                // when we add the platform stubs.
                val settings = remember { SettingsRepository() }
                val settingsState by settings.state.collectAsState()
                // App-wide audio service (Wasm = synthesized Web Audio SFX;
                // other targets = silent until real asset-backed impls ship).
                // Muted state tracks the user's sound-effects toggle.
                val audio = remember { createAudioService() }
                LaunchedEffect(settingsState.soundEnabled) {
                    audio.setMuted(!settingsState.soundEnabled)
                }
                // Profile (local UUID + display name), shared across online
                // lobby + profile-edit screen. Survives reloads via localStorage.
                val profile = remember { com.vivek.unosimple.profile.ProfileRepository() }

                // Sealed-class navigation. Swap for androidx.navigation.compose
                // once we grow past a handful of screens. Check storage for an
                // in-progress solo game — if present, land directly on the
                // game screen so a page reload resumes instead of dropping
                // back to Home. `botCount` is a rough placeholder; GameScreen
                // reads the real count from the saved state.
                var screen: Screen by remember {
                    val initial: Screen = if (GameViewModel.hasSavedSession()) {
                        Screen.Game(botCount = 2)
                    } else {
                        Screen.Home
                    }
                    mutableStateOf(initial)
                }

                CompositionLocalProvider(
                    com.vivek.unosimple.ui.theme.LocalReducedMotion provides settingsState.reducedMotion,
                    LocalAudio provides audio,
                ) {
                com.vivek.unosimple.ui.adaptive.AdaptiveScreenContainer {
                when (val s = screen) {
                    Screen.Home -> HomeScreen(
                        onStartGame = { botCount -> screen = Screen.Game(botCount) },
                        onOpenSettings = { screen = Screen.Settings },
                        onOpenLobby = { screen = Screen.Lobby },
                        onOpenOnlineLobby = { screen = Screen.OnlineLobby },
                        firebaseAvailable = firebaseSupported,
                    )
                    Screen.Settings -> SettingsScreen(
                        settings = settings,
                        onOpenProfile = { screen = Screen.Profile },
                        onDone = { screen = Screen.Home },
                    )
                    Screen.Profile -> com.vivek.unosimple.ui.profile.ProfileScreen(
                        profile = profile,
                        onBack = { screen = Screen.Settings },
                    )
                    Screen.Lobby -> LobbyScreen(
                        onStart = { seats -> screen = Screen.Hotseat(seats) },
                        onBack = { screen = Screen.Home },
                    )
                    Screen.OnlineLobby -> OnlineLobbyScreen(
                        profileName = profile.profile.value.displayName,
                        onCreateRoom = { name, code ->
                            // If the user edited the name in the lobby, push
                            // it back into the profile so it sticks next time.
                            if (name != profile.profile.value.displayName) profile.setDisplayName(name)
                            screen = Screen.Online(displayName = name, roomCode = code, isHost = true)
                        },
                        onJoinRoom = { name, code ->
                            if (name != profile.profile.value.displayName) profile.setDisplayName(name)
                            screen = Screen.Online(displayName = name, roomCode = code, isHost = false)
                        },
                        onBack = { screen = Screen.Home },
                    )
                    is Screen.Game -> GameScreen(
                        botCount = s.botCount,
                        onBackToHome = { screen = Screen.Home },
                    )
                    is Screen.Hotseat -> {
                        val vm = remember(s.seats) { HotseatGameViewModel(seats = s.seats) }
                        HotseatGameScreen(
                            vm = vm,
                            onBackToHome = { screen = Screen.Home },
                        )
                    }
                    is Screen.Online -> {
                        // One VM per room code. The anonymous-auth uid would
                        // normally become the client id; for v1 we use a
                        // session-random id derived from the display name +
                        // a short nonce. Swap to real auth when we wire up
                        // Firebase.auth anonymous sign-in.
                        val sync = remember(s.roomCode, s.displayName) {
                            // Use the stable profile UID as the online client id
                            // so the same device shows up with the same id across
                            // rooms / reconnects. Falls back to a random suffix if
                            // profile somehow isn't initialized.
                            val myId = profile.profile.value.uid
                            createFirebaseSyncService(myId = myId, roomCode = s.roomCode)
                        }
                        if (sync == null) {
                            OnlineNotSupportedScreen(onBack = { screen = Screen.Home })
                        } else {
                            val vm = remember(sync) { OnlineGameViewModel(sync) }
                            // Both host and guest append themselves to the
                            // seat list. The round is only dealt when the
                            // host taps "Start round" in the waiting room.
                            LaunchedEffect(sync, s.displayName) {
                                sync.joinSeat(
                                    PlayerSeat(id = sync.myId, displayName = s.displayName)
                                )
                            }
                            OnlineGameScreen(
                                vm = vm,
                                isHost = s.isHost,
                                roomCode = s.roomCode,
                                onBackToHome = { screen = Screen.Home },
                            )
                        }
                    }
                }
                } // close AdaptiveScreenContainer
                } // close LocalReducedMotion provider
            }
        }
    }
}
