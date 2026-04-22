package com.vivek.unosimple.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.friends.FriendProfile
import com.vivek.unosimple.friends.FriendsService
import com.vivek.unosimple.platform.Clipboard
import com.vivek.unosimple.ui.common.BackIcon
import com.vivek.unosimple.ui.game.PlayerAvatar
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.ClaySurface
import com.vivek.unosimple.ui.theme.GhostButton
import com.vivek.unosimple.ui.theme.LocalClayTokens
import kotlinx.coroutines.launch

/**
 * Friends tab: current UID (copyable as your "friend code"), pending
 * incoming requests with accept/reject, friends list with remove. Add a
 * new friend by pasting their UID into the bottom field.
 */
@Composable
fun FriendsScreen(
    service: FriendsService,
    myUid: String,
    onBack: () -> Unit,
) {
    val friends by service.friends.collectAsState()
    val pending by service.pendingRequests.collectAsState()
    val scope = rememberCoroutineScope()
    var addInput by remember { mutableStateOf("") }
    var statusMsg: String? by remember { mutableStateOf(null) }
    var codeCopied by remember { mutableStateOf(false) }

    DisposableEffect(service) {
        onDispose { scope.launch { service.close() } }
    }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("friends_screen"),
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
                    "FRIENDS",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Share your code, add theirs, play together.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(20.dp))

                // My code — tap to copy.
                SectionHeader("YOUR FRIEND CODE")
                Spacer(Modifier.height(8.dp))
                ClaySurface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        Clipboard.writeText(myUid)
                        codeCopied = true
                    },
                    color = MaterialTheme.colorScheme.primary,
                    cornerRadius = 14.dp,
                    elevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            myUid,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            if (codeCopied) "COPIED" else "TAP TO COPY",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Pending requests.
                SectionHeader("INCOMING REQUESTS (${pending.size})")
                Spacer(Modifier.height(8.dp))
                if (pending.isEmpty()) {
                    EmptyRow("No pending invites.")
                } else {
                    pending.forEach { p ->
                        PendingCard(
                            profile = p,
                            onAccept = { scope.launch { service.acceptRequest(p.uid) } },
                            onReject = { scope.launch { service.rejectRequest(p.uid) } },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Friends list.
                SectionHeader("FRIENDS (${friends.size})")
                Spacer(Modifier.height(8.dp))
                if (friends.isEmpty()) {
                    EmptyRow("Your list is empty. Add a code below to start.")
                } else {
                    friends.forEach { f ->
                        FriendCard(
                            profile = f,
                            onRemove = { scope.launch { service.removeFriend(f.uid) } },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))

                SectionHeader("ADD A FRIEND")
                Spacer(Modifier.height(8.dp))
                val stroke = LocalClayTokens.current.strokeColor
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, stroke, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = addInput,
                        onValueChange = { addInput = it.trim().take(24) },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().testTag("friends.add_input"),
                    )
                    if (addInput.isEmpty()) {
                        Text(
                            "Paste friend's code",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                ClayButton(
                    onClick = {
                        val target = addInput.trim()
                        if (target.isBlank() || target == myUid) {
                            statusMsg = "That's not a valid friend code."
                            return@ClayButton
                        }
                        scope.launch {
                            service.sendRequest(target)
                            statusMsg = "Request sent to ${target.take(8)}…"
                            addInput = ""
                        }
                    },
                    enabled = addInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("friends.add_submit"),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                ) {
                    Text("SEND REQUEST", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }

                if (statusMsg != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        statusMsg!!,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            // Back button last so it z-orders above the scrolling column.
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
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun EmptyRow(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun PendingCard(profile: FriendProfile, onAccept: () -> Unit, onReject: () -> Unit) {
    ClaySurface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        cornerRadius = 14.dp,
        elevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerAvatar(id = profile.uid, name = profile.displayName, size = 36.dp, avatarOverride = profile.avatarId)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.displayName.ifBlank { "(no name)" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    profile.uid,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ClayButton(
                onClick = onAccept,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) { Text("ACCEPT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(6.dp))
            GhostButton(
                onClick = onReject,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) { Text("DISMISS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun FriendCard(profile: FriendProfile, onRemove: () -> Unit) {
    ClaySurface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        cornerRadius = 14.dp,
        elevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerAvatar(id = profile.uid, name = profile.displayName, size = 36.dp, avatarOverride = profile.avatarId)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.displayName.ifBlank { "(no name)" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    profile.uid,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GhostButton(
                onClick = onRemove,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) { Text("REMOVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}
