package com.vivek.unosimple.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Whether the current user has asked for reduced motion. Decorative effects
 * (confetti, home backdrop bob, card-entry stagger, legal-card glow pulse)
 * should short-circuit when this is true. Functional feedback motion
 * (opponent shake, discard land, UNO button pulse while the declaration
 * window is open) still runs — the point is to remove eye-candy, not cues.
 *
 * Provided at the app root from [com.vivek.unosimple.settings.SettingsRepository].
 * Default `false` so unit tests and isolated previews don't need to set it.
 */
val LocalReducedMotion = compositionLocalOf { false }
