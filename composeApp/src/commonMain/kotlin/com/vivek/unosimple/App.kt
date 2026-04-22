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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
fun App(
    /**
     * Optional override for the starting screen. Production passes `null`
     * (→ Splash → routing). Tests pass `Screen.Home` to skip the 1.4-second
     * splash animation so click-through assertions land immediately.
     */
    overrideInitialScreen: Screen? = null,
) {
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
                // Shared history + achievements repos so every GameViewModel
                // writes into the same stores.
                val history = remember { com.vivek.unosimple.persistence.HistoryRepository() }
                val achievements = remember { com.vivek.unosimple.persistence.AchievementRepository() }
                // Friends service is per-profile; re-create when the
                // profile UID/name/avatar changes.
                val profileState by profile.profile.collectAsState()
                // Mirror the local profile into the Firebase users registry
                // on every change — triggers once right after onboarding
                // completes, and again on any Profile / AvatarPicker save.
                // Only fires when the user has actually set their name
                // (skips the initial "Player" placeholder).
                LaunchedEffect(
                    profileState.uid,
                    profileState.displayName,
                    profileState.avatarId,
                    profileState.hasSeenTutorial,
                ) {
                    if (profileState.hasSeenTutorial && profileState.displayName.isNotBlank()) {
                        com.vivek.unosimple.profile.writeUserProfileMirror(
                            uid = profileState.uid,
                            displayName = profileState.displayName,
                            avatarId = profileState.avatarId,
                        )
                    }
                }
                val friendsService = remember(profileState.uid) {
                    com.vivek.unosimple.friends.createFriendsService(
                        myUid = profileState.uid,
                        myDisplayName = profileState.displayName,
                        myAvatarId = profileState.avatarId,
                    )
                }
                val gameVmFactory = remember(history, achievements) {
                    viewModelFactory {
                        initializer {
                            GameViewModel(history = history, achievements = achievements)
                        }
                    }
                }

                // Sealed-class navigation. Swap for androidx.navigation.compose
                // once we grow past a handful of screens. Check storage for an
                // in-progress solo game — if present, land directly on the
                // game screen so a page reload resumes instead of dropping
                // back to Home. `botCount` is a rough placeholder; GameScreen
                // reads the real count from the saved state.
                var screen: Screen by remember {
                    // Cold-launch routing:
                    //   - Tests pass overrideInitialScreen = Screen.Home
                    //   - Wasm: if the URL path is /admin, deep-link there
                    //   - Otherwise → Splash → Onboarding (first run) / Home
                    val initial: Screen = overrideInitialScreen
                        ?: if (com.vivek.unosimple.platform.currentUrlPath().trim('/').equals("admin", ignoreCase = true)) {
                            Screen.Admin
                        } else Screen.Splash
                    mutableStateOf<Screen>(initial)
                }
                // Online session auto-resume — if a page reload lands while
                // the user was in an online room, read the stashed
                // {displayName, roomCode, isHost} and route back in.
                val onlineResume = remember { com.vivek.unosimple.persistence.OnlineSessionStore.read() }
                // Back-stack — push on forward nav, pop on back. Keeps
                // "back" context-correct (e.g. Settings → Rules → back
                // returns to Settings, not Home) without hardcoding a
                // return target in every screen's onBack callback.
                val backStack = remember { mutableListOf<Screen>() }
                val pushTo: (Screen) -> Unit = { next ->
                    backStack.add(screen)
                    screen = next
                }
                val goBack: (fallback: Screen) -> Unit = { fallback ->
                    screen = if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) else fallback
                }

                CompositionLocalProvider(
                    com.vivek.unosimple.ui.theme.LocalReducedMotion provides settingsState.reducedMotion,
                    LocalAudio provides audio,
                ) {
                com.vivek.unosimple.ui.adaptive.AdaptiveScreenContainer {
                when (val s = screen) {
                    Screen.Splash -> com.vivek.unosimple.ui.onboarding.SplashScreen(
                        onDone = {
                            // After splash: online-room resume first, then
                            // saved solo round, then first-run onboarding,
                            // then Home.
                            screen = when {
                                onlineResume != null -> Screen.Online(
                                    displayName = onlineResume.displayName,
                                    roomCode = onlineResume.roomCode,
                                    isHost = onlineResume.isHost,
                                )
                                GameViewModel.hasSavedSession() -> Screen.Game(botCount = 2)
                                !profile.profile.value.hasSeenTutorial -> Screen.Onboarding
                                else -> Screen.Home
                            }
                        },
                    )
                    Screen.Onboarding -> com.vivek.unosimple.ui.onboarding.OnboardingScreen(
                        profile = profile,
                        onDone = {
                            // Fresh start after onboarding — don't let back
                            // return to the one-time onboarding flow.
                            backStack.clear()
                            screen = Screen.Home
                        },
                    )
                    Screen.AvatarPicker -> com.vivek.unosimple.ui.profile.AvatarPickerScreen(
                        profile = profile,
                        onBack = { goBack(Screen.Profile) },
                    )
                    Screen.Home -> HomeScreen(
                        onStartGame = { botCount -> pushTo(Screen.Game(botCount)) },
                        onOpenSettings = { pushTo(Screen.Settings) },
                        onOpenLobby = { pushTo(Screen.Lobby) },
                        onOpenOnlineLobby = { pushTo(Screen.OnlineLobby) },
                        firebaseAvailable = firebaseSupported,
                        currentName = profileState.displayName,
                    )
                    Screen.Settings -> SettingsScreen(
                        settings = settings,
                        onOpenProfile = { pushTo(Screen.Profile) },
                        onOpenRules = { pushTo(Screen.Rules) },
                        onOpenAbout = { pushTo(Screen.About) },
                        onDone = { goBack(Screen.Home) },
                    )
                    Screen.Profile -> com.vivek.unosimple.ui.profile.ProfileScreen(
                        profile = profile,
                        onBack = { goBack(Screen.Settings) },
                        onPickAvatar = { pushTo(Screen.AvatarPicker) },
                        onOpenStats = { pushTo(Screen.Stats) },
                        onOpenFriends = { pushTo(Screen.Friends) },
                    )
                    Screen.Lobby -> LobbyScreen(
                        onStart = { seats ->
                            backStack.clear()
                            screen = Screen.Hotseat(seats)
                        },
                        onBack = { goBack(Screen.Home) },
                    )
                    Screen.OnlineLobby -> OnlineLobbyScreen(
                        profileName = profile.profile.value.displayName,
                        onCreateRoom = { name, code ->
                            if (name != profile.profile.value.displayName) profile.setDisplayName(name)
                            com.vivek.unosimple.persistence.OnlineSessionStore.write(
                                com.vivek.unosimple.persistence.OnlineSessionInfo(name, code, true)
                            )
                            backStack.clear()
                            screen = Screen.Online(displayName = name, roomCode = code, isHost = true)
                        },
                        onJoinRoom = { name, code ->
                            if (name != profile.profile.value.displayName) profile.setDisplayName(name)
                            com.vivek.unosimple.persistence.OnlineSessionStore.write(
                                com.vivek.unosimple.persistence.OnlineSessionInfo(name, code, false)
                            )
                            backStack.clear()
                            screen = Screen.Online(displayName = name, roomCode = code, isHost = false)
                        },
                        onBack = { goBack(Screen.Home) },
                    )
                    is Screen.Game -> GameScreen(
                        botCount = s.botCount,
                        onBackToHome = {
                            backStack.clear()
                            screen = Screen.Home
                        },
                        onOpenRules = { pushTo(Screen.Rules) },
                        onOpenSettings = { pushTo(Screen.Settings) },
                        vm = viewModel(factory = gameVmFactory),
                    )
                    Screen.Rules -> com.vivek.unosimple.ui.rules.RulesScreen(
                        onBack = { goBack(Screen.Home) },
                    )
                    Screen.About -> com.vivek.unosimple.ui.about.AboutScreen(
                        onBack = { goBack(Screen.Settings) },
                    )
                    Screen.Stats -> com.vivek.unosimple.ui.stats.StatsScreen(
                        history = history,
                        humanId = profile.profile.value.uid,
                        onBack = { goBack(Screen.Profile) },
                        onOpenAchievements = { pushTo(Screen.Achievements) },
                    )
                    Screen.Achievements -> com.vivek.unosimple.ui.stats.AchievementsScreen(
                        achievements = achievements,
                        onBack = { goBack(Screen.Stats) },
                    )
                    Screen.Friends -> com.vivek.unosimple.ui.friends.FriendsScreen(
                        service = friendsService,
                        myUid = profileState.uid,
                        onBack = { goBack(Screen.Profile) },
                    )
                    Screen.Admin -> com.vivek.unosimple.ui.admin.AdminScreen(
                        profile = profile,
                        history = history,
                        achievements = achievements,
                        onBack = {
                            backStack.clear()
                            screen = Screen.Home
                        },
                    )
                    is Screen.Hotseat -> {
                        val vm = remember(s.seats) { HotseatGameViewModel(seats = s.seats) }
                        HotseatGameScreen(
                            vm = vm,
                            onBackToHome = {
                                backStack.clear()
                                screen = Screen.Home
                            },
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
                            OnlineNotSupportedScreen(onBack = { goBack(Screen.Home) })
                        } else {
                            val vm = remember(sync) { OnlineGameViewModel(sync) }
                            // Both host and guest append themselves to the
                            // seat list. The round is only dealt when the
                            // host taps "Start round" in the waiting room.
                            LaunchedEffect(sync, s.displayName) {
                                sync.joinSeat(
                                    PlayerSeat(
                                        id = sync.myId,
                                        displayName = s.displayName,
                                        avatarId = profile.profile.value.avatarId,
                                    )
                                )
                            }
                            OnlineGameScreen(
                                vm = vm,
                                isHost = s.isHost,
                                roomCode = s.roomCode,
                                onBackToHome = {
                                    com.vivek.unosimple.persistence.OnlineSessionStore.clear()
                                    backStack.clear()
                                    screen = Screen.Home
                                },
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
