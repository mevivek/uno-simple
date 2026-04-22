package com.vivek.unosimple.haptics

/**
 * Android / iOS / Desktop default. Real AndroidHapticsService
 * (VibratorManager / HapticFeedbackConstants) + IosHapticsService
 * (UIImpactFeedbackGenerator) are tracked in TODO.md — both need
 * platform-specific code that's easiest to test on real devices.
 */
actual fun createHapticsService(): HapticsService = NoHapticsService()
