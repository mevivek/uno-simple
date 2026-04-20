import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    // google-services is applied below — only when the user sets
    // firebase.enabled=true in gradle.properties after dropping
    // google-services.json into composeApp/.
}

// Apply the Firebase Google-Services Gradle plugin iff the user has
// configured Firebase per docs/FIREBASE_SETUP.md. Without this guard
// the plugin fails the build when `composeApp/google-services.json`
// is missing, which would block builds for anyone who hasn't set up
// Firebase yet.
if (providers.gradleProperty("firebase.enabled").map { it == "true" }.orNull == true) {
    apply(plugin = "com.google.gms.google-services")
}

kotlin {
    // Default KMP source set hierarchy — gives us intermediate iosMain
    // (parent of iosX64Main/iosArm64Main/iosSimulatorArm64Main) without
    // explicit dependsOn wiring. The iOS framework's MainViewController
    // lives in src/iosMain/kotlin via this template.
    applyDefaultHierarchyTemplate()

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "composeApp"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.navigation.compose)
                // Serialization is needed here (not just in :shared) because
                // the Wasm Firebase wrapper encodes/decodes GameState to JSON
                // directly on the Kotlin/Wasm side.
                implementation(libs.kotlinx.serialization.json)
            }
        }
        // Intermediate set that feeds Android + iOS + Desktop but NOT Wasm.
        // dev.gitlive:firebase-* doesn't publish Wasm variants; keeping these
        // deps out of commonMain lets the Wasm build stay green.
        val nonWasmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.firebase.common)
                implementation(libs.firebase.database)
                implementation(libs.firebase.auth)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
        val androidMain by getting {
            dependsOn(nonWasmMain)
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(compose.uiTooling)
            }
        }
        // applyDefaultHierarchyTemplate creates iosMain; hook it to nonWasm
        // so the three iOS leaf sets pick up Firebase via transitive deps.
        val iosMain by getting { dependsOn(nonWasmMain) }
        val desktopMain by getting {
            dependsOn(nonWasmMain)
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.desktop.uiTestJUnit4)
            }
        }
    }
}

android {
    namespace = "com.vivek.unosimple"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vivek.unosimple"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "com.vivek.unosimple.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "UNO Simple"
            packageVersion = "1.0.0"
        }
    }
}
