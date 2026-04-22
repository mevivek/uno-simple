package com.vivek.unosimple.ui.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.vivek.unosimple.profile.ProfileRepository
import com.vivek.unosimple.ui.common.BackIcon
import com.vivek.unosimple.ui.game.HeartsRainOverlay
import com.vivek.unosimple.ui.game.PlayerAvatar
import com.vivek.unosimple.ui.game.isGeetName
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.ClaySurface
import kotlinx.coroutines.delay

/**
 * Profile screen: a big avatar (color derived from the profile UID) and an
 * editable display name. The UID itself is read-only — it's the device's
 * stable identity, later used as the friend code in F2.
 */
@Composable
fun ProfileScreen(
    profile: ProfileRepository,
    onBack: () -> Unit,
    onPickAvatar: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
) {
    val current by profile.profile.collectAsState()
    var editing by remember { mutableStateOf(current.displayName) }

    // Geet easter egg — also triggers here when the user edits their name.
    // Fires on every transition INTO a matching name (not on each keystroke
    // after, since the predicate re-checks only when the string equals).
    // Also fires once on entry if the stored name was already set to Geet,
    // so she gets a welcome-back shower.
    var heartsRaining by remember { mutableStateOf(false) }
    LaunchedEffect(editing) {
        if (isGeetName(editing)) {
            heartsRaining = true
            profile.setAvatarId("bot10")
            delay(3800)
            heartsRaining = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 80.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Profile",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(24.dp))

                // Big avatar — renders the picked persona if one is set,
                // else the uid-derived initial-on-disc fallback. Tapping
                // routes to the dedicated avatar picker.
                Box(
                    modifier = Modifier.clickable(onClick = onPickAvatar),
                ) {
                    PlayerAvatar(
                        id = current.uid,
                        name = current.displayName.ifBlank { "?" },
                        size = 96.dp,
                        avatarOverride = current.avatarId,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "TAP TO CHANGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black,
                )

                Spacer(Modifier.height(20.dp))

                // Name editor — clay-style pill with inline text field.
                ClaySurface(
                    color = MaterialTheme.colorScheme.surface,
                    cornerRadius = 20.dp,
                    elevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Name",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(56.dp),
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            BasicTextField(
                                value = editing,
                                onValueChange = { editing = it.take(20) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (editing.isEmpty()) {
                                Text(
                                    "Enter name",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Read-only UID chip — short, shareable. F2 will use this as
                // a friend code.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ID",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            current.uid,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Stats entry — big amber card linking to the dashboard.
                ClaySurface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenStats),
                    color = MaterialTheme.colorScheme.primary,
                    cornerRadius = 14.dp,
                    elevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "STATS & ACHIEVEMENTS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            "VIEW \u203A",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                ClaySurface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFriends),
                    color = MaterialTheme.colorScheme.secondary,
                    cornerRadius = 14.dp,
                    elevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "FRIENDS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondary,
                        )
                        Text(
                            "OPEN \u203A",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                ClayButton(
                    onClick = {
                        profile.setDisplayName(editing)
                        onBack()
                    },
                    enabled = editing.trim().isNotEmpty() && editing.trim() != current.displayName,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                ) {
                    Text(
                        "Save",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            // Easter-egg overlay — pink hearts fall when Geet is entered.
            HeartsRainOverlay(visible = heartsRaining)
        }
    }
}
