package com.vivek.unosimple.ui.lobby

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.persistence.SessionStore
import com.vivek.unosimple.ui.TestTags
import com.vivek.unosimple.ui.common.BackIcon
import com.vivek.unosimple.ui.theme.ClayButton
import com.vivek.unosimple.ui.theme.ClaySurface
import kotlin.random.Random

/**
 * Online multiplayer landing page.
 *
 * Layout: one hero "How do we play?" question, a name field up top,
 * and two big claymorph route cards side-by-side — **Create** (primary)
 * and **Join** (secondary). Tapping Create immediately generates a
 * room code and advances; the Join route reveals a 4-digit code field
 * with big number-pad-style boxes.
 */
@Composable
fun OnlineLobbyScreen(
    profileName: String = "",
    onCreateRoom: (displayName: String, roomCode: String) -> Unit,
    onJoinRoom: (displayName: String, roomCode: String) -> Unit,
    onBack: () -> Unit,
) {
    // Pre-fill from the profile (if set), else fall back to the per-lobby
    // cache we used before the profile feature. Either way the user can
    // still edit in the field.
    var name by remember {
        mutableStateOf(
            profileName.ifBlank { SessionStore.read(DISPLAY_NAME_KEY).orEmpty() }
        )
    }
    var joinCode by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(Mode.Choose) }
    LaunchedEffect(name) {
        if (name.isNotBlank()) SessionStore.write(DISPLAY_NAME_KEY, name)
    }

    Surface(
        modifier = Modifier.fillMaxSize().testTag(TestTags.ONLINE_LOBBY_SCREEN),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Back button in the top-left as a clay icon.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        if (mode == Mode.Join) mode = Mode.Choose else onBack()
                    },
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
                    "Play online",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(24.dp))

                // Name field — claymorph rounded pill, no Material outline.
                NameField(
                    name = name,
                    onChange = { name = it.uppercase().take(20) },
                )

                Spacer(Modifier.height(28.dp))

                when (mode) {
                    Mode.Choose -> ChooseRoute(
                        onCreate = {
                            onCreateRoom(name.ifBlank { "HOST" }, generateRoomCode())
                        },
                        onJoinPressed = { mode = Mode.Join },
                    )
                    Mode.Join -> JoinRoute(
                        joinCode = joinCode,
                        onCodeChange = { input -> joinCode = input.filter { it.isDigit() }.take(4) },
                        onJoin = { onJoinRoom(name.ifBlank { "GUEST" }, joinCode) },
                    )
                }
            }
        }
    }
}

private enum class Mode { Choose, Join }

@Composable
private fun NameField(name: String, onChange: (String) -> Unit) {
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
                "You",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(48.dp),
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = name,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.ONLINE_LOBBY_NAME_FIELD),
                )
                if (name.isEmpty()) {
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
}

@Composable
private fun ChooseRoute(onCreate: () -> Unit, onJoinPressed: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RouteCard(
            title = "Create",
            subtitle = "new room",
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .weight(1f)
                .testTag(TestTags.ONLINE_LOBBY_CREATE_BUTTON),
            onClick = onCreate,
        )
        RouteCard(
            title = "Join",
            subtitle = "with code",
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.weight(1f),
            onClick = onJoinPressed,
        )
    }
}

@Composable
private fun RouteCard(
    title: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(modifier = modifier.clickable(onClick = onClick)) {
        ClaySurface(
            color = color,
            cornerRadius = 24.dp,
            elevation = 10.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = contentColor,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun JoinRoute(
    joinCode: String,
    onCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "4-digit code",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(14.dp))

        // Four big boxes showing each digit, with a hidden BasicTextField
        // layered behind for input. Tapping anywhere focuses the field.
        Box(contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0..3) {
                    DigitBox(
                        digit = joinCode.getOrNull(i)?.toString() ?: "",
                        focused = joinCode.length == i,
                    )
                }
            }
            // Invisible input spanning the row of boxes.
            BasicTextField(
                value = joinCode,
                onValueChange = onCodeChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(color = androidx.compose.ui.graphics.Color.Transparent),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .testTag(TestTags.ONLINE_LOBBY_CODE_FIELD),
            )
        }

        Spacer(Modifier.height(28.dp))

        ClayButton(
            onClick = onJoin,
            enabled = joinCode.length == 4,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.ONLINE_LOBBY_JOIN_BUTTON),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
        ) {
            Text(
                "Join room",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun DigitBox(digit: String, focused: Boolean) {
    val borderColor = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    ClaySurface(
        color = MaterialTheme.colorScheme.surface,
        cornerRadius = 16.dp,
        elevation = if (focused) 6.dp else 3.dp,
    ) {
        Box(
            modifier = Modifier.size(width = 58.dp, height = 72.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = digit,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (focused && digit.isEmpty()) {
                Box(
                    modifier = Modifier
                        .size(width = 2.dp, height = 28.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            // Bottom accent under focused slot for extra signal.
            if (focused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .size(width = 20.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(borderColor),
                )
            }
        }
    }
}

private const val DISPLAY_NAME_KEY = "uno.lobby.displayName"

/**
 * 4-digit numeric room code. Easy to read aloud and type on a phone keypad;
 * 10000 possible codes is plenty for a hobby game where only a handful of
 * rooms are live at once.
 */
internal fun generateRoomCode(rng: Random = Random): String {
    return buildString(4) { repeat(4) { append(rng.nextInt(0, 10)) } }
}
