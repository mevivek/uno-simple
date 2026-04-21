package com.vivek.unosimple.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivek.unosimple.BuildInfo
import com.vivek.unosimple.ui.common.BackIcon
import com.vivek.unosimple.ui.theme.ClaySurface

/**
 * About / credits — version, build stamp, tech stack credits, license
 * attribution for the bundled OFL fonts. Reached from the Settings screen.
 * Keeps the "who built this and how" visible without bloating Settings.
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize().testTag("about_screen"),
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 72.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "UNO SIMPLE",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "build ${BuildInfo.BUILD_STAMP}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(28.dp))

                AboutPanel(title = "WHAT THIS IS") {
                    AboutLine("A Kotlin Multiplatform + Compose Multiplatform UNO clone for private use. Not distributed, not affiliated with Mattel.")
                    AboutLine("Targets Android, iOS, Desktop (JVM), and Web (Wasm). Same engine + UI everywhere.")
                }

                Spacer(Modifier.height(16.dp))

                AboutPanel(title = "TECH") {
                    AboutLine("Kotlin 2.1 + Compose Multiplatform")
                    AboutLine("Pure-Kotlin engine in the :shared module")
                    AboutLine("Firebase Realtime Database for online multiplayer")
                    AboutLine("Web Audio API for synthesized sound effects")
                }

                Spacer(Modifier.height(16.dp))

                AboutPanel(title = "FONTS (OFL)") {
                    AboutLine("Bungee \u2014 David Jonathan Ross / Google Fonts")
                    AboutLine("Inter \u2014 Rasmus Andersson / Google Fonts")
                }

                Spacer(Modifier.height(16.dp))

                AboutPanel(title = "SOURCE") {
                    AboutLine("github.com/mevivek/uno-simple")
                    AboutLine("Deployed at uno-simple-5757a3.web.app")
                }

                Spacer(Modifier.height(20.dp))

                // Private dedication line. Small + coral + centered.
                Text(
                    "made with \u2764 for geet",
                    style = MaterialTheme.typography.labelLarge,
                    color = androidx.compose.ui.graphics.Color(0xFFFF5168),
                    fontWeight = FontWeight.Black,
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AboutPanel(title: String, content: @Composable () -> Unit) {
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) { content() }
        }
    }
}

@Composable
private fun AboutLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
