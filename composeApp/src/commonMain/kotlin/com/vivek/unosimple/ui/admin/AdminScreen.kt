package com.vivek.unosimple.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.persistence.AchievementRepository
import com.vivek.unosimple.persistence.HistoryRepository
import com.vivek.unosimple.persistence.SessionStore
import com.vivek.unosimple.profile.ProfileRepository
import com.vivek.unosimple.ui.common.BackIcon
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.ClaySurface
import com.vivek.unosimple.ui.theme.GhostButton

/**
 * Admin / reset panel. Reached via the URL path `/admin` on Wasm, or via
 * direct navigation elsewhere. Meant for the project owner — shows
 * destructive reset buttons for every local data store plus the current
 * profile summary. No real multi-user "admin" since the app is personal-
 * device only; "admin" here means "manage this device's data".
 */
@Composable
fun AdminScreen(
    profile: ProfileRepository,
    history: HistoryRepository,
    achievements: AchievementRepository,
    onBack: () -> Unit,
) {
    val currentProfile by profile.profile.collectAsState()
    val records by history.records.collectAsState()
    val unlocked by achievements.unlocked.collectAsState()

    var lastAction: String? by remember { mutableStateOf(null) }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("admin_screen"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 72.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "ADMIN",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Danger zone — destructive resets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))

                // Profile summary panel.
                AdminPanel(title = "CURRENT PROFILE") {
                    SummaryLine("Name", currentProfile.displayName)
                    SummaryLine("UID", currentProfile.uid)
                    SummaryLine("Avatar id", currentProfile.avatarId ?: "(none)")
                    SummaryLine("Tutorial seen", currentProfile.hasSeenTutorial.toString())
                }

                Spacer(Modifier.height(16.dp))

                AdminPanel(title = "STORAGE") {
                    SummaryLine("Round records", records.size.toString())
                    SummaryLine("Achievements unlocked", "${unlocked.size} of 10")
                    SummaryLine("Saved solo session", if (SessionStore.read("uno.session.v1") != null) "yes" else "no")
                }

                Spacer(Modifier.height(24.dp))

                DangerButton(
                    label = "CLEAR ROUND HISTORY",
                    subtitle = "Removes the ${records.size} round record(s) + resets all stats",
                    onConfirm = {
                        history.clear()
                        lastAction = "Round history cleared."
                    },
                )
                DangerButton(
                    label = "LOCK ACHIEVEMENTS",
                    subtitle = "Re-locks all unlocked badges",
                    onConfirm = {
                        achievements.clear()
                        lastAction = "Achievements re-locked."
                    },
                )
                DangerButton(
                    label = "RESET PROFILE",
                    subtitle = "Regenerates UID, clears name + avatar + tutorial flag",
                    onConfirm = {
                        SessionStore.write("uno.profile.v1", null)
                        lastAction = "Profile reset. Reload the page to see a fresh UID."
                    },
                )
                DangerButton(
                    label = "CLEAR SAVED SOLO SESSION",
                    subtitle = "Drops the in-progress resume so the next launch starts fresh",
                    onConfirm = {
                        SessionStore.write("uno.session.v1", null)
                        lastAction = "Saved session cleared."
                    },
                )
                DangerButton(
                    label = "FACTORY RESET",
                    subtitle = "Nukes everything above in one shot",
                    onConfirm = {
                        history.clear()
                        achievements.clear()
                        SessionStore.write("uno.profile.v1", null)
                        SessionStore.write("uno.session.v1", null)
                        SessionStore.write("uno.lobby.displayName", null)
                        lastAction = "All local data cleared. Reload to restart."
                    },
                )

                if (lastAction != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        lastAction!!,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Black,
                    )
                }

                Spacer(Modifier.height(24.dp))

                ClayButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                ) {
                    Text(
                        "BACK TO HOME",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                BackIcon(size = 20.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AdminPanel(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        ClaySurface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            cornerRadius = 14.dp,
            elevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) { content() }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Two-tap destructive button. First tap shows "TAP AGAIN TO CONFIRM" for
 * ~4s; second tap within that window fires [onConfirm]. Keeps resets from
 * being one-click-regret operations.
 */
@Composable
private fun DangerButton(
    label: String,
    subtitle: String,
    onConfirm: () -> Unit,
) {
    var armed by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(armed) {
        if (armed) {
            kotlinx.coroutines.delay(4000)
            armed = false
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        GhostButton(
            onClick = {
                if (armed) {
                    armed = false
                    onConfirm()
                } else {
                    armed = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = if (armed) "TAP AGAIN TO CONFIRM" else label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
