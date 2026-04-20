plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    // Declared apply=false; composeApp applies it conditionally when
    // firebase.enabled=true in gradle.properties (see composeApp/build.gradle.kts).
    alias(libs.plugins.google.services) apply false
}
