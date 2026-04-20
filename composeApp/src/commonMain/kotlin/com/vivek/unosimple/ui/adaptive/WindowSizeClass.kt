package com.vivek.unosimple.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Coarse three-bucket screen-size classification matching the Material 3
 * breakpoints. Driven from `BoxWithConstraints` at the app root so every
 * screen can branch layouts as forms factor changes — required from Phase 3
 * onward per `.claude/memory/feedback_adaptive_ui.md`.
 */
enum class WindowSizeClass {
    /** < 600 dp wide — phones in portrait, narrow phone landscapes. */
    Compact,

    /** 600–840 dp wide — foldables, small tablets, phone landscape. */
    Medium,

    /** ≥ 840 dp wide — tablets, desktop, large web windows. */
    Expanded,
}

/**
 * CompositionLocal holding the current [WindowSizeClass]. Read via
 * `LocalWindowSizeClass.current` inside any composable.
 */
val LocalWindowSizeClass = compositionLocalOf { WindowSizeClass.Compact }

/**
 * Convenience accessor so callers don't have to import `LocalWindowSizeClass`.
 */
@Composable
@ReadOnlyComposable
fun currentWindowSizeClass(): WindowSizeClass = LocalWindowSizeClass.current

/**
 * Wraps [content] with a provider that computes the current [WindowSizeClass]
 * from the available width. Place once at the root of the app so nested
 * screens can call [currentWindowSizeClass] without re-measuring.
 */
@Composable
fun ProvideWindowSizeClass(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sizeClass = when {
            maxWidth < 600.dp -> WindowSizeClass.Compact
            maxWidth < 840.dp -> WindowSizeClass.Medium
            else -> WindowSizeClass.Expanded
        }
        CompositionLocalProvider(LocalWindowSizeClass provides sizeClass) {
            content()
        }
    }
}

/**
 * Wraps [content] in a centered container whose width is capped on wider
 * screens, so a phone-first layout doesn't stretch awkwardly on tablets or
 * desktop browser windows.
 *
 * - Compact: fills the available width (no change).
 * - Medium: centered, capped at 640 dp.
 * - Expanded: centered, capped at 820 dp.
 */
@Composable
fun AdaptiveScreenContainer(content: @Composable () -> Unit) {
    val sc = LocalWindowSizeClass.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter,
    ) {
        val innerModifier = when (sc) {
            WindowSizeClass.Compact -> Modifier.fillMaxSize()
            WindowSizeClass.Medium -> Modifier.fillMaxSize().widthIn(max = 640.dp)
            WindowSizeClass.Expanded -> Modifier.fillMaxSize().widthIn(max = 820.dp)
        }
        Box(modifier = innerModifier) { content() }
    }
}
