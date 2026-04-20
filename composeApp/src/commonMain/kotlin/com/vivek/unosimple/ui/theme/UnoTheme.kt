package com.vivek.unosimple.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.Font
import uno_simple.composeapp.generated.resources.Bungee_Regular
import uno_simple.composeapp.generated.resources.Inter_Regular
import uno_simple.composeapp.generated.resources.Res

/**
 * Premium dark "arcade card-game" theme (Phase 1 of the 2026-04-20 revamp).
 * Replaces the former claymorph/cream palette. Saturated-on-dark, with a
 * Bungee chunky display face + Inter for body. Rich amber + coral + neon
 * accents read adult and game-native rather than "kids educational".
 *
 * Legacy API names (`ClaySurface`, `ClayButton`, `LocalClayTokens`,
 * `UnoClayTokens`) are preserved deliberately — many call-sites reference
 * them, and the new dark visuals slot in under the same names so the rest
 * of the app picks up the look automatically. A later phase may rename
 * these to `Panel` / `PrimaryButton` once the churn is tolerable.
 */
@Composable
fun UnoTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalClayTokens provides ArcadeTokens) {
        MaterialTheme(
            colorScheme = UnoDarkColorScheme,
            typography = unoTypography(),
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// Palette — dark arcade. Pinned to the north-star in the revamp plan.
// ---------------------------------------------------------------------------

internal val Obsidian: Color = Color(0xFF0B0F17)       // base background
internal val Midnight: Color = Color(0xFF141A27)       // card/surface base
internal val Slate: Color = Color(0xFF1D2435)          // elevated surfaces
internal val StrokeSoft: Color = Color(0xFF2A3347)     // subtle dividers + borders
internal val Ink: Color = Color(0xFFF5F0E3)            // primary text — warm bone
internal val InkMuted: Color = Color(0xFF9BA3B4)       // secondary text
internal val Amber: Color = Color(0xFFFFB53B)          // primary accent / gold CTA
internal val AmberDeep: Color = Color(0xFFE8931F)
internal val Coral: Color = Color(0xFFFF5168)          // danger / wild-four / penalty
internal val Neon: Color = Color(0xFF2EE89B)           // legal-play / UNO / go
internal val Sky: Color = Color(0xFF49B6FF)            // information / info CTAs
internal val Violet: Color = Color(0xFFA06BFF)         // wilds / magic
internal val Crimson: Color = Color(0xFFB2263A)        // error

private val UnoDarkColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Obsidian,
    primaryContainer = AmberDeep,
    onPrimaryContainer = Obsidian,

    secondary = Sky,
    onSecondary = Obsidian,
    secondaryContainer = Color(0xFF0E3A5E),
    onSecondaryContainer = Ink,

    tertiary = Neon,
    onTertiary = Obsidian,
    tertiaryContainer = Color(0xFF0F3D2A),
    onTertiaryContainer = Ink,

    background = Obsidian,
    onBackground = Ink,
    surface = Midnight,
    onSurface = Ink,
    surfaceVariant = Slate,
    onSurfaceVariant = InkMuted,

    outline = StrokeSoft,
    outlineVariant = Color(0xFF1F2639),

    error = Crimson,
    onError = Ink,
)

// ---------------------------------------------------------------------------
// Typography — Bungee for display/headlines/cards; Inter for body.
// ---------------------------------------------------------------------------

@Composable
private fun bungeeFamily(): FontFamily = FontFamily(
    Font(Res.font.Bungee_Regular, weight = FontWeight.Normal),
    Font(Res.font.Bungee_Regular, weight = FontWeight.Bold),
    Font(Res.font.Bungee_Regular, weight = FontWeight.Black),
)

@Composable
private fun interFamily(): FontFamily = FontFamily(
    Font(Res.font.Inter_Regular, weight = FontWeight.Normal),
    Font(Res.font.Inter_Regular, weight = FontWeight.Medium),
    Font(Res.font.Inter_Regular, weight = FontWeight.SemiBold),
    Font(Res.font.Inter_Regular, weight = FontWeight.Bold),
    Font(Res.font.Inter_Regular, weight = FontWeight.ExtraBold),
    Font(Res.font.Inter_Regular, weight = FontWeight.Black),
)

@Composable
private fun unoTypography(): Typography {
    val display = bungeeFamily()
    val body = interFamily()
    val base = Typography()
    return base.copy(
        // Display + headlines — chunky arcade face.
        displayLarge = base.displayLarge.copy(fontFamily = display, fontWeight = FontWeight.Black),
        displayMedium = base.displayMedium.copy(fontFamily = display, fontWeight = FontWeight.Black),
        displaySmall = base.displaySmall.copy(fontFamily = display, fontWeight = FontWeight.Black),
        headlineLarge = base.headlineLarge.copy(fontFamily = display, fontWeight = FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = display, fontWeight = FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = display, fontWeight = FontWeight.ExtraBold),
        // Titles — also display face; smaller scale.
        titleLarge = base.titleLarge.copy(fontFamily = display, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = body, fontWeight = FontWeight.Black),
        titleSmall = base.titleSmall.copy(fontFamily = body, fontWeight = FontWeight.Bold),
        // Body + labels — clean sans for utility content.
        bodyLarge = base.bodyLarge.copy(fontFamily = body, fontWeight = FontWeight.Normal),
        bodyMedium = base.bodyMedium.copy(fontFamily = body, fontWeight = FontWeight.Normal),
        bodySmall = base.bodySmall.copy(fontFamily = body, fontWeight = FontWeight.Normal),
        labelLarge = base.labelLarge.copy(fontFamily = body, fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.copy(fontFamily = body, fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(fontFamily = body, fontWeight = FontWeight.Medium),
    )
}

// ---------------------------------------------------------------------------
// Tokens — naming kept for compatibility with existing call-sites. Semantics
// flipped to the new dark-arcade vocabulary.
// ---------------------------------------------------------------------------

@Immutable
data class UnoClayTokens(
    /** Dark drop-shadow color. Still called "shadowColor" for backwards API. */
    val shadowColor: Color,
    /** Cool-white rim highlight painted along the top edge of elevated surfaces. */
    val highlightColor: Color,
    /** Default corner radius for surfaces — tightened from 20dp → 12dp. */
    val cornerRadiusDp: Int,
    /** Default elevation / shadow spread. */
    val elevationDp: Int,
    /** Hero-background accent — the warm glow behind the table / home hero. */
    val tableAccent: Color,
    /** Color of thin strokes on panels and dividers. */
    val strokeColor: Color,
)

internal val ArcadeTokens: UnoClayTokens = UnoClayTokens(
    shadowColor = Color(0xAA000000),
    highlightColor = Color(0x33FFFFFF),
    cornerRadiusDp = 14,
    elevationDp = 10,
    tableAccent = Color(0xFF1A2236),
    strokeColor = StrokeSoft,
)

val LocalClayTokens = staticCompositionLocalOf { ArcadeTokens }

/** Convenience: default corner radius as Dp. */
val UnoClayTokens.corner get() = cornerRadiusDp.dp

/** Convenience: default elevation as Dp. */
val UnoClayTokens.elevation get() = elevationDp.dp
