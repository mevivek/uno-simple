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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.vivek.unosimple.persistence.RoundRecord
import com.vivek.unosimple.persistence.SessionStore
import com.vivek.unosimple.profile.ProfileRepository
import com.vivek.unosimple.ui.common.BackIcon
import com.vivek.unosimple.ui.game.PlayerAvatar
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.ClaySurface
import com.vivek.unosimple.ui.theme.GhostButton
import kotlinx.coroutines.delay

/**
 * Admin / management panel. Reached via the URL path `/admin` on Wasm.
 * Scopes:
 *   - USERS     — every local user profile stored on this device (just
 *                 one, since the app is single-profile-per-install).
 *                 Delete = regenerate UID + wipe name/avatar/tutorial flag.
 *   - SESSIONS  — in-progress solo session (if any). Delete = drop the
 *                 resume blob so the next launch starts fresh.
 *   - HISTORY   — every completed round with per-row delete + a bulk
 *                 clear button.
 *   - BULK      — factory reset nukes the above in one shot.
 *
 * Two-tap-to-confirm on every destructive button. No real multi-user
 * admin — the app is personal-device only, so "users" here means "the
 * local profile stored on this machine".
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
    val savedSessionRaw = remember(records.size) { SessionStore.read("uno.session.v1") }

    var lastAction: String? by remember { mutableStateOf(null) }
    LaunchedEffect(lastAction) {
        if (lastAction != null) {
            delay(3500)
            lastAction = null
        }
    }
    fun flash(msg: String) { lastAction = msg }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("admin_screen"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 72.dp, bottom = 32.dp),
            ) {
                Text(
                    "ADMIN",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Manage every local user, session, and round on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(20.dp))

                // ---- USERS ----------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader("USERS (1)")
                    SmallConfirmButton(
                        label = "DELETE ALL",
                        onConfirm = {
                            SessionStore.write("uno.profile.v1", null)
                            SessionStore.write("uno.lobby.displayName", null)
                            flash("All user profiles wiped. Reload for a fresh UID.")
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                UserCard(
                    name = currentProfile.displayName,
                    uid = currentProfile.uid,
                    avatarId = currentProfile.avatarId,
                    tutorialSeen = currentProfile.hasSeenTutorial,
                    onDelete = {
                        SessionStore.write("uno.profile.v1", null)
                        flash("Profile wiped. Reload the page to get a fresh UID.")
                    },
                )

                Spacer(Modifier.height(20.dp))

                // ---- SESSIONS -------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader("GAME SESSIONS (${if (savedSessionRaw != null) 1 else 0})")
                    if (savedSessionRaw != null) {
                        SmallConfirmButton(
                            label = "DELETE ALL",
                            onConfirm = {
                                SessionStore.write("uno.session.v1", null)
                                flash("All in-progress sessions cleared.")
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (savedSessionRaw != null) {
                    SessionCard(
                        raw = savedSessionRaw,
                        onDelete = {
                            SessionStore.write("uno.session.v1", null)
                            flash("In-progress session cleared.")
                        },
                    )
                } else {
                    EmptyHint("No in-progress session on this device.")
                }

                Spacer(Modifier.height(20.dp))

                // ---- HISTORY --------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader("ROUND HISTORY (${records.size})")
                    if (records.isNotEmpty()) {
                        SmallConfirmButton(
                            label = "CLEAR ALL",
                            onConfirm = {
                                history.clear()
                                flash("Cleared ${records.size} round record(s).")
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (records.isEmpty()) {
                    EmptyHint("No rounds played yet.")
                } else {
                    // Newest-first display. The stored order is chronological
                    // so we just reverse-iterate the indices.
                    records.indices.reversed().forEach { idx ->
                        HistoryRowCard(
                            ordinal = idx + 1,
                            record = records[idx],
                            onDelete = {
                                history.removeAt(idx)
                                flash("Round #${idx + 1} deleted.")
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ---- BULK + ACHIEVEMENTS --------------------------------
                SectionHeader("BULK")
                Spacer(Modifier.height(8.dp))
                ClaySurface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    cornerRadius = 14.dp,
                    elevation = 6.dp,
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Achievements unlocked: ${unlocked.size} of 10",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            SmallConfirmButton(
                                label = "RE-LOCK",
                                onConfirm = {
                                    achievements.clear()
                                    flash("Achievements re-locked.")
                                },
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        SmallConfirmButton(
                            label = "FACTORY RESET EVERYTHING",
                            fullWidth = true,
                            onConfirm = {
                                history.clear()
                                achievements.clear()
                                SessionStore.write("uno.profile.v1", null)
                                SessionStore.write("uno.session.v1", null)
                                SessionStore.write("uno.lobby.displayName", null)
                                flash("Everything cleared. Reload to start fresh.")
                            },
                        )
                    }
                }

                if (lastAction != null) {
                    Spacer(Modifier.height(16.dp))
                    ClaySurface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        cornerRadius = 12.dp,
                        elevation = 4.dp,
                    ) {
                        Text(
                            lastAction!!,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
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

// ---------------------------------------------------------------------------
// Rows
// ---------------------------------------------------------------------------

@Composable
private fun UserCard(
    name: String,
    uid: String,
    avatarId: String?,
    tutorialSeen: Boolean,
    onDelete: () -> Unit,
) {
    ClaySurface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        cornerRadius = 14.dp,
        elevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerAvatar(id = uid, name = name, size = 44.dp, avatarOverride = avatarId)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    name.ifBlank { "(no name)" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "UID: $uid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "avatar=${avatarId ?: "none"} · tutorial=${if (tutorialSeen) "seen" else "new"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SmallConfirmButton(label = "DELETE", onConfirm = onDelete)
        }
    }
}

@Composable
private fun SessionCard(raw: String, onDelete: () -> Unit) {
    val summary = remember(raw) { summarizeSession(raw) }
    ClaySurface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        cornerRadius = 14.dp,
        elevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "In-progress solo session",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SmallConfirmButton(label = "DELETE", onConfirm = onDelete)
        }
    }
}

@Composable
private fun HistoryRowCard(
    ordinal: Int,
    record: RoundRecord,
    onDelete: () -> Unit,
) {
    val humanBadge = if (record.humanWon) "YOU WON" else "YOU LOST"
    val humanBadgeColor = if (record.humanWon) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    ClaySurface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        cornerRadius = 12.dp,
        elevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ordinal + winner.
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#$ordinal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${record.winnerName} won +${record.deltaPoints}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = record.botNames.joinToString(separator = ", ").ifBlank { "(online / hotseat)" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // YOU WON / LOST badge.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(humanBadgeColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    humanBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.width(8.dp))
            SmallConfirmButton(label = "DEL", onConfirm = onDelete, tight = true)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
    )
}

/**
 * Small two-tap delete chip. First tap arms (shows "SURE?"), second tap
 * within ~3s confirms. Inline so each destructive row has its own
 * affordance.
 */
@Composable
private fun SmallConfirmButton(
    label: String,
    onConfirm: () -> Unit,
    tight: Boolean = false,
    fullWidth: Boolean = false,
) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(3000)
            armed = false
        }
    }
    val modifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier
    val padding = if (tight) PaddingValues(horizontal = 8.dp, vertical = 4.dp) else PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    GhostButton(
        onClick = {
            if (armed) { armed = false; onConfirm() } else { armed = true }
        },
        modifier = modifier,
        contentPadding = padding,
    ) {
        Text(
            text = if (armed) "SURE?" else label,
            style = if (tight) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Black,
        )
    }
}

/**
 * Lightweight summary of the saved-session JSON blob. Parses just enough
 * to surface bot count + active seat list without depending on GameState's
 * full schema (which lives in :shared and would need serialization setup
 * re-duplicated here). Best-effort — falls back to "saved session exists".
 */
private fun summarizeSession(raw: String): String {
    // Pull a few simple fields with naive substring matching. The blob is
    // our own JSON so format is stable; if extraction fails, we show a
    // generic label.
    return runCatching {
        val botCount = Regex("\"botCount\"\\s*:\\s*(\\d+)").find(raw)?.groupValues?.get(1)?.toIntOrNull()
        val names = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").findAll(raw).map { it.groupValues[1] }.take(6).toList()
        buildString {
            append(if (botCount != null) "$botCount bot(s)" else "solo round")
            if (names.isNotEmpty()) append(" · seats: ${names.joinToString(", ")}")
        }
    }.getOrDefault("saved session exists")
}
