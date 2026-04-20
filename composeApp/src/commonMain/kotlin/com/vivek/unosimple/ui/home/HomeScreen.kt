package com.vivek.unosimple.ui.home

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivek.unosimple.BuildInfo
import com.vivek.unosimple.ui.TestTags
import com.vivek.unosimple.ui.common.GearIcon
import com.vivek.unosimple.ui.game.BOTS
import com.vivek.unosimple.ui.game.PlayerAvatar
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.ClaySurface
import com.vivek.unosimple.ui.theme.GhostButton
import com.vivek.unosimple.ui.theme.LocalClayTokens

@Composable
fun HomeScreen(
    onStartGame: (botCount: Int) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenLobby: () -> Unit = {},
    onOpenOnlineLobby: () -> Unit = {},
    firebaseAvailable: Boolean = true,
) {
    var botCount by rememberSaveable { mutableStateOf(2) }

    Surface(
        modifier = Modifier.fillMaxSize().testTag(TestTags.HOME_SCREEN),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // Warm amber glow at the top fades into the obsidian base —
                    // gives the hero title something to sit in front of, and
                    // reads as "stage lighting" against the dark felt.
                    Brush.radialGradient(
                        0f to LocalClayTokens.current.tableAccent,
                        0.5f to MaterialTheme.colorScheme.background,
                        1f to MaterialTheme.colorScheme.background,
                        radius = 1200f,
                        center = Offset(600f, 220f),
                    )
                ),
        ) {
            HomeBackdrop()
            SettingsGear(onOpenSettings)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 64.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                // Hero title with warm amber gradient + tight letter-spacing.
                HeroTitle()

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Saturday-night card battles",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(36.dp))

                SectionLabel("PICK YOUR OPPONENTS")
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (index in 0..2) {
                        OpponentSlot(
                            index = index,
                            active = botCount >= index + 1,
                            onClick = { botCount = index + 1 },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = versusCaption(botCount),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(Modifier.height(28.dp))

                // Primary CTA — the single amber slab that dominates the fold.
                ClayButton(
                    onClick = { onStartGame(botCount) },
                    modifier = Modifier
                        .widthIn(min = 260.dp)
                        .testTag(TestTags.HOME_START_BUTTON),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    contentPadding = PaddingValues(horizontal = 40.dp, vertical = 20.dp),
                ) {
                    Text(
                        text = "PLAY",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Secondary modes — quieter ghost buttons so the eye still
                // lands on PLAY first.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                ) {
                    GhostButton(
                        onClick = onOpenLobby,
                        modifier = Modifier.testTag(TestTags.HOME_HOTSEAT_BUTTON),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = "PASS & PLAY",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    GhostButton(
                        onClick = onOpenOnlineLobby,
                        enabled = firebaseAvailable,
                        modifier = Modifier.testTag(TestTags.HOME_ONLINE_BUTTON),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = if (firebaseAvailable) "ONLINE" else "ONLINE (N/A)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "build ${BuildInfo.BUILD_STAMP}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun HeroTitle() {
    Text(
        text = "UNO SIMPLE",
        modifier = Modifier.testTag(TestTags.HOME_TITLE),
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsGear(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpenSettings)
            .testTag(TestTags.HOME_SETTINGS_BUTTON),
        contentAlignment = Alignment.Center,
    ) {
        GearIcon(size = 22.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Persona selector tile. Shows illustrated face + name on a dark panel;
 * inactive tiles dim + scale down. Active tile gets an amber bottom-edge
 * accent so the eye snaps to who's in the round.
 */
@Composable
private fun OpponentSlot(
    index: Int,
    active: Boolean,
    onClick: () -> Unit,
) {
    val count = index + 1
    val persona = BOTS[index]
    val botId = "bot$count"

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (active) 1f else 0.9f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        ),
        label = "slot-scale",
    )
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (active) 1f else 0.4f,
        label = "slot-alpha",
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("${TestTags.HOME_OPPONENT_CHIP_PREFIX}$count"),
    ) {
        ClaySurface(
            color = if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
            cornerRadius = 16.dp,
            elevation = if (active) 10.dp else 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .size(width = 80.dp, height = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(modifier = Modifier.alpha(alpha)) {
                    PlayerAvatar(id = botId, name = persona.name, size = 58.dp)
                }
                Text(
                    text = persona.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (active) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    0.5f to MaterialTheme.colorScheme.primary,
                                    1f to Color.Transparent,
                                ),
                            ),
                    )
                }
            }
        }
    }
}

private fun versusCaption(botCount: Int): String = when (botCount) {
    1 -> "You vs ${BOTS[0].name}"
    2 -> "You vs ${BOTS[0].name} & ${BOTS[1].name}"
    3 -> "You vs ${BOTS[0].name}, ${BOTS[1].name} & ${BOTS[2].name}"
    else -> "You vs $botCount bots"
}
