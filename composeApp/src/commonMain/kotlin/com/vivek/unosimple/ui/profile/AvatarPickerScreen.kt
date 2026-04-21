package com.vivek.unosimple.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.vivek.unosimple.profile.ProfileRepository
import com.vivek.unosimple.ui.common.BackIcon
import com.vivek.unosimple.ui.game.BOTS
import com.vivek.unosimple.ui.game.PlayerAvatar
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.LocalClayTokens

/**
 * Avatar picker — 3×3 grid of the [BOTS] persona faces. The user taps one to
 * bind it to their profile; a "Save" CTA persists the choice. Re-uses the
 * bot Canvas illustrations rather than shipping another set of art; the
 * personas are whimsical enough to double as user avatars.
 */
@Composable
fun AvatarPickerScreen(
    profile: ProfileRepository,
    onBack: () -> Unit,
) {
    val current by profile.profile.collectAsState()
    var selected by remember { mutableStateOf(current.avatarId) }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("avatar_picker_screen"),
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
                    .padding(top = 72.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "PICK YOUR FACE",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "You can change it later in Profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))

                // 3x3 grid of persona tiles.
                for (rowIdx in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    ) {
                        for (colIdx in 0..2) {
                            val i = rowIdx * 3 + colIdx
                            if (i < BOTS.size) {
                                val persona = BOTS[i]
                                val avatarId = "bot${i + 1}"
                                val isSelected = selected == avatarId
                                AvatarTile(
                                    avatarId = avatarId,
                                    name = persona.name,
                                    selected = isSelected,
                                    onClick = { selected = avatarId },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(8.dp))

                ClayButton(
                    onClick = {
                        profile.setAvatarId(selected)
                        onBack()
                    },
                    enabled = selected != current.avatarId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("avatar_picker.save"),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                ) {
                    Text(
                        "SAVE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarTile(
    avatarId: String,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val stroke = LocalClayTokens.current.strokeColor
    val ring = if (selected) MaterialTheme.colorScheme.primary else stroke
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(if (selected) 3.dp else 1.dp, ring, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("avatar_picker.$avatarId"),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PlayerAvatar(id = avatarId, name = name, size = 56.dp)
            Spacer(Modifier.height(2.dp))
            Text(
                name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
