package com.vivek.unosimple.haptics

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App-wide [HapticsService] available to any composable. Provided at the
 * root in `App()` via `CompositionLocalProvider`. Defaults to
 * [NoHapticsService] so isolated previews + unit tests don't need wiring.
 *
 * Call-sites:
 *   val haptics = LocalHaptics.current
 *   haptics.emit(HapticPattern.BUMP)
 */
val LocalHaptics = staticCompositionLocalOf<HapticsService> { NoHapticsService() }
