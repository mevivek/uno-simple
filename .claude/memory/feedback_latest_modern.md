---
name: Keep everything latest and modern
description: Always use the latest stable versions of frameworks, libraries, language features, and modern idioms for this hobby KMP UNO project.
type: feedback
---

For the UNO Simple project, always prefer the latest stable versions of everything and use modern Kotlin/Compose idioms.

**Why:** User stated on 2026-04-19: "try to keep everything latest and modern." Hobby project with no legacy constraints — no reason to use old patterns. Also a learning opportunity.

**How to apply:**
- **Kotlin:** latest stable (2.1+ as of 2026-04-19). Bumped via `gradle/libs.versions.toml`. Use modern features where they genuinely help — sealed interfaces, data classes with `copy`, `Result`, context receivers (with care), structured concurrency, Flow, `buildString`/`buildList`, destructuring, smart casts, top-level functions.
- **Compose Multiplatform:** latest stable (1.7+). Material 3 (`androidx.compose.material3`), NOT Material 2.
- **AGP (Android Gradle Plugin):** latest stable that works with the current Kotlin + Compose combination.
- **Gradle:** latest stable (8.11+).
- **Libraries:** Use the current recommended library for each concern:
  - State: `MutableStateFlow` inside `androidx.lifecycle` multiplatform ViewModel. Not LiveData, not RxJava.
  - Models: Kotlin `data class` / `sealed interface` + `kotlinx.serialization` with `@Serializable`.
  - Async: `kotlinx.coroutines` with Flow.
  - Navigation: `androidx.navigation.compose` multiplatform.
- Before adding a library, check it's actively maintained (recent releases, working Multiplatform support where applicable).
- Don't use deprecated APIs (e.g. Material 2, `collectAsState()` without lifecycle variant).
- Null safety is assumed; never add `!!` or unjustified unsafe casts.

**What this does NOT mean:**
- Don't chase bleeding-edge alpha/beta/EAP releases. Stable only.
- Don't rewrite working code just because a newer pattern exists — applies to new code.

**Concrete current versions (2026-04-19):** Kotlin 2.1.0, Compose Multiplatform 1.7.3, AGP 8.7.3, Gradle 8.11.1. Bump as new stable versions release.
