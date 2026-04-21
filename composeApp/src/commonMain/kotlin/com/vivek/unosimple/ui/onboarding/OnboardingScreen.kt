package com.vivek.unosimple.ui.onboarding

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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.profile.ProfileRepository
import com.vivek.unosimple.ui.game.BOTS
import com.vivek.unosimple.ui.game.PlayerAvatar
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.LocalClayTokens

/**
 * Two-step onboarding for first-run users: name + avatar pick on one
 * scrollable panel, then a big "LET'S PLAY" CTA that marks the tutorial as
 * seen and hands off to Home. Intentionally simple — a full guided tutorial
 * round is a later phase.
 *
 * If the user pre-set a display name or avatar in a prior session, those
 * pre-fill and the flow lets them confirm + continue.
 */
@Composable
fun OnboardingScreen(
    profile: ProfileRepository,
    onDone: () -> Unit,
) {
    val current by profile.profile.collectAsState()
    var name by remember { mutableStateOf(current.displayName.ifBlank { "" }) }
    var avatarId: String? by remember { mutableStateOf(current.avatarId ?: "bot1") }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("onboarding_screen"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0f to LocalClayTokens.current.tableAccent.copy(alpha = 0.55f),
                        0.55f to MaterialTheme.colorScheme.background,
                        radius = 900f,
                    )
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 60.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "WELCOME",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Set yourself up — name + face",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(28.dp))

                // Current pick preview — the human-sized avatar with the
                // picked persona. Updates live as the user taps below.
                PlayerAvatar(
                    id = "bot1",
                    name = name.ifBlank { "You" },
                    size = 96.dp,
                    avatarOverride = avatarId,
                )

                Spacer(Modifier.height(20.dp))

                // Name field.
                NameInput(
                    name = name,
                    onChange = { name = it.take(20) },
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    "PICK YOUR FACE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(12.dp))

                // Horizontal fan of persona tiles — all 9 side by side.
                // FlowRow would wrap; this is a Row with small tiles so a
                // phone-width viewport shows all 9 without scroll.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    for (i in BOTS.indices) {
                        val id = "bot${i + 1}"
                        MiniAvatarTile(
                            avatarId = id,
                            name = BOTS[i].name,
                            selected = avatarId == id,
                            onClick = { avatarId = id },
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                ClayButton(
                    onClick = {
                        profile.setDisplayName(name.ifBlank { "Player" })
                        profile.setAvatarId(avatarId)
                        profile.markTutorialSeen()
                        onDone()
                    },
                    enabled = name.trim().isNotEmpty(),
                    modifier = Modifier
                        .widthIn(min = 240.dp)
                        .testTag("onboarding.continue"),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    contentPadding = PaddingValues(horizontal = 40.dp, vertical = 18.dp),
                ) {
                    Text(
                        "LET'S PLAY",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun NameInput(name: String, onChange: (String) -> Unit) {
    val stroke = LocalClayTokens.current.strokeColor
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, stroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        BasicTextField(
            value = name,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding.name"),
        )
        if (name.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Your name",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
private fun MiniAvatarTile(
    avatarId: String,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val stroke = LocalClayTokens.current.strokeColor
    val ring = if (selected) MaterialTheme.colorScheme.primary else stroke
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, ring, CircleShape)
            .clickable(onClick = onClick)
            .testTag("onboarding.avatar.$avatarId"),
        contentAlignment = Alignment.Center,
    ) {
        // Small avatar — initials fallback isn't meaningful here; always use
        // the persona illustration.
        PlayerAvatar(id = avatarId, name = name, size = 36.dp)
    }
}
