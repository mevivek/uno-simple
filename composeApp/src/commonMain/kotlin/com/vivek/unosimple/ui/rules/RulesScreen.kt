package com.vivek.unosimple.ui.rules

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.DrawTwoCard
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.ReverseCard
import com.vivek.unosimple.engine.models.SkipCard
import com.vivek.unosimple.engine.models.WildCard
import com.vivek.unosimple.engine.models.WildDrawFourCard
import com.vivek.unosimple.ui.common.BackIcon
import com.vivek.unosimple.ui.game.CardView
import com.vivek.unosimple.ui.theme.ClaySurface

/**
 * Rules reference — a scroll view of rule cards with live [CardView] demos
 * for each action card. Linked from Settings ("How to play") and the in-
 * game Pause overlay. Content kept short + concrete so a lapsed player
 * can re-learn UNO in 30 seconds.
 */
@Composable
fun RulesScreen(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize().testTag("rules_screen"),
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
                    "HOW TO PLAY",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Match the top discard by color or number. First to empty their hand wins.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))

                RuleSectionHeader("ACTION CARDS")
                Spacer(Modifier.height(12.dp))

                RuleRow(
                    title = "Skip",
                    body = "Next player loses their turn.",
                ) { CardView(card = SkipCard(CardColor.BLUE), enabled = true, onClick = null) }

                RuleRow(
                    title = "Reverse",
                    body = "Play order flips. With 2 players it acts like Skip.",
                ) { CardView(card = ReverseCard(CardColor.GREEN), enabled = true, onClick = null) }

                RuleRow(
                    title = "Draw Two",
                    body = "Next player draws two cards and loses their turn.",
                ) { CardView(card = DrawTwoCard(CardColor.RED), enabled = true, onClick = null) }

                RuleRow(
                    title = "Wild",
                    body = "Playable on any color. Pick the new active color.",
                ) { CardView(card = WildCard, enabled = true, onClick = null) }

                RuleRow(
                    title = "Wild Draw Four",
                    body = "Next player draws four and loses their turn. You pick the new color.",
                ) { CardView(card = WildDrawFourCard, enabled = true, onClick = null) }

                Spacer(Modifier.height(20.dp))

                RuleSectionHeader("CALLING UNO")
                Spacer(Modifier.height(12.dp))
                RuleRow(
                    title = "One card left",
                    body = "Tap CALL UNO before playing your second-to-last card. Forget, and the engine deals you four penalty cards.",
                ) { CardView(card = NumberCard(CardColor.YELLOW, 7), enabled = true, onClick = null) }

                Spacer(Modifier.height(20.dp))

                RuleSectionHeader("SCORING")
                Spacer(Modifier.height(12.dp))
                RuleBodyOnly(
                    "At round end, the winner collects the sum of every other player's remaining hand: number cards face value, action cards 20 each, wilds 50 each.",
                )

                Spacer(Modifier.height(36.dp))
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
private fun RuleSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun RuleRow(
    title: String,
    body: String,
    card: @Composable () -> Unit,
) {
    ClaySurface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        color = MaterialTheme.colorScheme.surface,
        cornerRadius = 14.dp,
        elevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(68.dp), contentAlignment = Alignment.Center) {
                card()
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RuleBodyOnly(text: String) {
    ClaySurface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        cornerRadius = 14.dp,
        elevation = 6.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
