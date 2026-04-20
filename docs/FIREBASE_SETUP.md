# Firebase setup for UNO Simple multiplayer

Turning `FirebaseSyncService` from a stub into a working multiplayer backend. Estimated time: 30–60 minutes the first time.

## 1. Create the Firebase project

1. Go to <https://console.firebase.google.com/>.
2. "Add project" → name it `uno-simple` (or anything).
3. Disable Google Analytics when offered — not needed.
4. Wait for provisioning (~1 min).

## 2. Enable Realtime Database

1. In the Firebase console sidebar, **Build → Realtime Database → Create Database**.
2. Location: pick the nearest region.
3. Start in **test mode** (locked to authenticated users; we'll enable anonymous auth next).
4. The DB URL appears at the top — looks like `https://uno-simple-default-rtdb.firebaseio.com`. Copy it.

## 3. Enable Anonymous Authentication

1. **Build → Authentication → Get started**.
2. Sign-in method → **Anonymous → Enable → Save**.
3. (Google / Apple sign-in is optional for linking accounts across devices — enable later if desired, matching ADR-005.)

## 4. Register apps

### Android

1. Firebase console → Project settings → **Your apps → Add app → Android**.
2. Android package name: `com.vivek.unosimple`. Register.
3. Download `google-services.json`.
4. Copy to: `composeApp/google-services.json`
5. This file is **not** committed to git — it contains your project config. Add it to `.gitignore` if not already excluded.

### iOS

1. Firebase console → Project settings → **Your apps → Add app → iOS**.
2. iOS bundle ID: `com.vivek.unosimple` (or whatever your `iosApp/iosApp.xcodeproj` uses).
3. Download `GoogleService-Info.plist`.
4. Copy to: `iosApp/iosApp/GoogleService-Info.plist` (add to Xcode's "Copy Bundle Resources" build phase).
5. Requires a Mac — see [IOS_SETUP.md](IOS_SETUP.md).

### Web (Wasm)

1. Firebase console → Project settings → **Your apps → Add app → Web (`</>`)**.
2. Nickname: `uno-simple-web`.
3. Copy the `firebaseConfig` object that Firebase shows — it has `apiKey`, `authDomain`, `databaseURL`, etc.
4. Paste it into `composeApp/src/wasmJsMain/resources/firebase-config.js`:
   ```javascript
   export const firebaseConfig = {
     apiKey: "...",
     authDomain: "...",
     databaseURL: "...",
     projectId: "...",
     storageBucket: "...",
     messagingSenderId: "...",
     appId: "...",
   };
   ```

## 5. Add Firebase dependencies to the Gradle build

### 5a. Root `build.gradle.kts`

```kotlin
plugins {
    // ... existing lines ...
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

### 5b. `composeApp/build.gradle.kts`

**Plugins block** (top of file):
```kotlin
plugins {
    // ... existing plugin lines ...
    id("com.google.gms.google-services")
}
```

**commonMain dependencies**:
```kotlin
val commonMain by getting {
    dependencies {
        // ... existing ...
        implementation(libs.firebase.database)
        implementation(libs.firebase.auth)
        implementation(libs.firebase.common)
    }
}
```

Uncomment the library aliases in `gradle/libs.versions.toml` (they're already declared as `firebase-database`, `firebase-auth`, `firebase-common`).

### 5c. Android app module config

Add at the top of `composeApp/build.gradle.kts` after existing plugins:
```kotlin
apply(plugin = "com.google.gms.google-services")
```

## 6. Replace the FirebaseSyncService stub

Open `composeApp/src/commonMain/kotlin/com/vivek/unosimple/multiplayer/FirebaseSyncService.kt`. Each method has a KDoc with the exact `dev.gitlive:firebase-database` code to paste. The stub returns `SubmitResult.NotConnected` today; once you replace the bodies, the service does real Firebase calls.

You'll also need:
- An `init { }` block that subscribes to `database.reference("rooms/$roomCode/state").valueEvents` and pushes decoded `GameState` into `_state`.
- A coroutine scope for that subscription (pass one in via the constructor, or use `GlobalScope.launch` if you prefer — not ideal, but acceptable for a hobby project).

## 7. Wire Firebase initialization at app startup

### Android

In `MainActivity.onCreate()`:
```kotlin
FirebaseApp.initializeApp(this)
```
(The google-services plugin usually does this automatically, but it's safe to be explicit.)

### iOS

In `iosApp`'s AppDelegate:
```swift
FirebaseApp.configure()
```

### Web (Wasm)

In `index.html`, before `composeApp.js`:
```html
<script type="module">
  import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.7.0/firebase-app.js';
  import { firebaseConfig } from './firebase-config.js';
  window.firebaseApp = initializeApp(firebaseConfig);
</script>
```

## 8. Secure your database

By default, Realtime Database allows read/write to any authenticated user. For a hobby project that's fine. If concerned about abuse, add rules in the Firebase console:

```json
{
  "rules": {
    "rooms": {
      "$roomCode": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

## 9. Cost expectations

Firebase free tier (Spark plan):
- Realtime Database: 10 GB/month downloaded, 1 GB stored — way more than a hobby game will ever use.
- Authentication: anonymous auth is free, unlimited.
- No card required for Spark plan.

## 10. Ready to play

- Build + run: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` (or android / desktop).
- Open two browsers → each creates / joins the same room code → play a round across "devices."

## What this doesn't get you (yet)

- UI for the lobby / join-by-code flow. The `FirebaseSyncService` is ready, but the `LobbyScreen` composable isn't. Plan: add one screen that (a) creates a room (random 4-char code) or (b) joins an existing code, then navigates to `GameScreen` with the `FirebaseSyncService` wired up.
- Reconnection / out-of-order action handling. For a turn-based game with a small number of actions per minute, Firebase's default ordering + latency compensation is sufficient. Polish later if needed.
- Anti-cheat. Client-authoritative model means a determined cheater could push invalid state; for a hobby project with friends that's fine.
