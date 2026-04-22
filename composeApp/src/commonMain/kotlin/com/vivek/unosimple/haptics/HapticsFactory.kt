package com.vivek.unosimple.haptics

/**
 * Per-platform factory for the concrete [HapticsService].
 *
 * - **Android** — `VibratorManager` / `HapticFeedbackConstants` (deferred;
 *   needs platform code).
 * - **iOS** — `UIImpactFeedbackGenerator` (deferred; needs Mac).
 * - **Desktop / Wasm** — no haptics APIs; returns [NoHapticsService].
 *
 * Keeping the scaffolding here lets call-sites wire `LocalHaptics.current
 * .emit(...)` now; when real impls drop in, behavior lights up
 * automatically without touching the game screens.
 */
expect fun createHapticsService(): HapticsService
