# KMP setup (Windows)

You (the human) run this once per machine. Everything else in the project is automated from Claude sessions via Gradle.

## What you need

- **JDK 17+** — comes bundled with Android Studio as the JetBrains Runtime (JBR). You already have it if you have Android Studio installed.
- **Android SDK** — comes with Android Studio.
- **Git** — for version control and cloning the repo.
- (Optional, for iOS builds) **macOS with Xcode** — not required for Phases 0–4. We develop on Windows.

## 1. Verify prerequisites

Open PowerShell and run:

```powershell
# Java (JDK)
$env:JAVA_HOME
"$env:JAVA_HOME\bin\java.exe" -version  # should print 17+

# Android SDK
Test-Path "$env:LOCALAPPDATA\Android\Sdk"  # True

# adb
adb --version

# Git
git --version
```

For this project we're already confirmed: JDK 21 via Android Studio's JBR at `C:\Users\meviv\AppData\Local\Programs\Android Studio\jbr`, Android SDK at `C:\Users\meviv\AppData\Local\Android\Sdk`.

## 2. Point Gradle at the Android SDK

Create `local.properties` at the project root (git-ignored):

```properties
sdk.dir=C:\\Users\\meviv\\AppData\\Local\\Android\\Sdk
```

Already done for this repo.

## 3. First build

From the project root:

```bash
./gradlew :shared:desktopTest
```

First run downloads Gradle 8.11.1 (~150 MB) and all plugins (~a few hundred MB). Expect 3-5 minutes on a fast connection, then subsequent builds are cached and much faster.

If that passes, you've got a working toolchain.

## 4. Running the app

| Target | Command | Notes |
|---|---|---|
| Desktop (JVM) | `./gradlew :composeApp:run` | Opens a native window. Best for fast iteration. |
| Web (Wasm) | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` | Serves at http://localhost:8080. |
| Android | `./gradlew :composeApp:installDebug` | Requires an emulator or connected device. |
| iOS | (deferred) | Requires Mac. |

## 5. Android emulator

If you want to test on Android:
1. Open Android Studio → Device Manager → Create a device (Pixel 7 / API 34 works well).
2. Start it.
3. Run `adb devices` — should list your emulator.
4. `./gradlew :composeApp:installDebug` — installs and launches.

## 6. Running tests

```bash
./gradlew :shared:allTests                      # engine tests, all targets
./gradlew :shared:desktopTest                   # engine tests, JVM only (fastest)
./gradlew :composeApp:desktopTest               # Compose UI tests (JVM)
./gradlew :composeApp:connectedAndroidTest      # Android instrumented tests (needs emulator)
```

## 7. Updating toolchain later

- **Kotlin/Compose/AGP versions:** edit `gradle/libs.versions.toml`.
- **Gradle itself:** edit `gradle/wrapper/gradle-wrapper.properties` (bump `distributionUrl`).
- **Android SDK:** via Android Studio → SDK Manager.

## Troubleshooting

- **"JAVA_HOME is not set"** — set it in PowerShell via `$env:JAVA_HOME="C:\Users\meviv\AppData\Local\Programs\Android Studio\jbr"` or add to user env vars permanently.
- **Gradle wrapper fails to download** — check network, retry, or run `./gradlew --refresh-dependencies`.
- **"SDK location not found"** — `local.properties` missing or wrong path.
- **Wasm build hangs on "npm install"** — first run only; takes ~2 minutes. Be patient.
- **iOS targets fail on Windows** — expected. `kotlin.native.ignoreDisabledTargets=true` hides the warning.
