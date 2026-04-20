# iOS setup

Building and running UNO Simple on iOS requires a Mac. Writing the code runs fine on Windows/Linux — we have `iosMain` source sets and the Compose Multiplatform `iosX64 / iosArm64 / iosSimulatorArm64` targets configured — but the Kotlin/Native compiler, `xcodebuild`, and the iOS simulator only exist on macOS.

## Option A — Borrow / own a Mac

1. Clone the repo on the Mac.
2. Install **Xcode 15+** and its command-line tools (`xcode-select --install`).
3. Install **Kotlin Multiplatform Mobile plugin** for IntelliJ / Android Studio — it bundles the KMP-aware Xcode project generation and simulator launcher. Not strictly required but makes iteration fast.
4. From the project root:
   ```bash
   ./gradlew :composeApp:iosSimulatorArm64Test     # unit tests on simulator
   ./gradlew :composeApp:compileKotlinIosArm64     # device-target compile
   ```
5. For a runnable app, you also need an **Xcode project** that embeds the
   shared framework. The JetBrains Compose Multiplatform template
   ([`compose-multiplatform-template`](https://github.com/JetBrains/compose-multiplatform-template))
   ships a ready-made `iosApp/` directory with the right settings —
   copy that into this repo as `iosApp/` and adjust the Framework path
   to point at our `composeApp` framework output.

## Option B — Codemagic (no Mac required)

Codemagic's free tier offers 500 build minutes/month on macOS VMs — enough for a hobby-scale iOS workflow.

1. Sign up at <https://codemagic.io/>.
2. Add this GitHub repo (once you push).
3. Pick "Kotlin Multiplatform" as the workflow template.
4. Configure the build: Xcode project path = `iosApp/iosApp.xcodeproj`, Scheme = `iosApp`, Output = `.ipa`.
5. Builds run on push; artifacts download to your machine. Sideload via Xcode or TestFlight.

## Option C — TestFlight (after Option A or B)

For personal devices (yours and friends'):
1. Enroll in the Apple Developer Program ($99/year).
2. Generate a distribution certificate + provisioning profile.
3. Upload build to App Store Connect, distribute via TestFlight to up to 100 internal testers.

The hobby project ADR-008 specifies we'd rename before ever submitting to the App Store (UNO trademark). TestFlight internal distribution doesn't go through App Review, so the current name works there.

## What's already wired up for iOS

- `composeApp/src/iosMain/kotlin/com/vivek/unosimple/ios/MainViewController.kt` exposes `MainViewController()` returning a `ComposeUIViewController { App() }`. Swift-side code calls that to embed the Compose UI in a `UIViewController`.
- `composeApp/build.gradle.kts` declares the three iOS targets (x64, arm64, simulator arm64) with `baseName = "composeApp"` and `isStatic = true`. Running `./gradlew :composeApp:linkPodDebugFrameworkIosArm64` produces `composeApp.framework`.
- `gradle.properties` has `kotlin.native.ignoreDisabledTargets=true` so the iOS targets don't block builds on Windows — they're just skipped with a warning.

## What's missing (because it needs a Mac)

- `iosApp/iosApp.xcodeproj` — the Xcode wrapper project that embeds the Compose framework into a runnable `.app`. Must be created on macOS via Xcode. The reference structure to mirror is JetBrains' `compose-multiplatform-template/iosApp/`.
- App icons / launch screen assets.
- iOS-specific permissions / capabilities (none needed yet — no camera, no network, no file access).
- Real haptic implementation via `UIImpactFeedbackGenerator` in a future `IosHapticsService`.
- Real audio implementation via `AVAudioPlayer` in a future `IosAudioService`.
