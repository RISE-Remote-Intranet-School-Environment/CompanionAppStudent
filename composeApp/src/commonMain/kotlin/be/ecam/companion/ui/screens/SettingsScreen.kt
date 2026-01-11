@file:Suppress("DEPRECATION")

package be.ecam.companion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import be.ecam.companion.ui.theme.LocalAppSettingsController
import be.ecam.companion.ui.theme.ThemeMode
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import companion.composeapp.generated.resources.Res
import companion.composeapp.generated.resources.claco2_slogan_svg
import org.jetbrains.compose.resources.painterResource

@Composable
fun SettingsScreen(
    isColorBlindMode: Boolean,
    onColorBlindModeChange: (Boolean) -> Unit,
    bearerToken: String? = null,
    modifier: Modifier = Modifier
) {
    var tokenVisible by remember { mutableStateOf(false) }
    val settings = LocalAppSettingsController.current

    val screenPresets = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f)
    val textPresets = be.ecam.companion.ui.theme.TextScaleMode.entries
    val screenIndex = nearestIndex(screenPresets, settings.screenSizeMode.scale)
    val textIndex = textPresets.indexOf(settings.textScaleMode).coerceAtLeast(0)
    val isDark = settings.themeMode.isDark

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Paramètres",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Suivre les réglages de l’appareil",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Synchronise le thème et la taille du texte avec le système.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.followSystemSettings,
                        onCheckedChange = settings.setFollowSystemSettings
                    )
                }

                SettingsSliderRow(
                    icon = Icons.Filled.DisplaySettings,
                    title = "Taille d’écran",
                    description = "Zoom: ${(screenPresets[screenIndex] * 100).toInt()}%",
                    value = screenIndex.toFloat(),
                    steps = screenPresets.size - 2,
                    valueRange = 0f..(screenPresets.size - 1).toFloat(),
                    onValueChange = { settings.setScreenSizeMode(be.ecam.companion.ui.theme.ScreenSizeMode.fromScale(screenPresets[it.toInt()])) },
                    enabled = !settings.followSystemSettings
                )

                SettingsSliderRow(
                    icon = Icons.Filled.FormatSize,
                    title = "Taille de texte",
                    description = textPresets[textIndex].description(),
                    value = textIndex.toFloat(),
                    steps = textPresets.size - 2,
                    valueRange = 0f..(textPresets.size - 1).toFloat(),
                    onValueChange = { settings.setTextScaleMode(textPresets[it.toInt()]) },
                    enabled = !settings.followSystemSettings
                )

                SettingsToggleRow(
                    icon = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    title = "Thème",
                    description = if (isDark) "Mode sombre" else "Mode clair",
                    isDark = isDark,
                    onToggle = { settings.setThemeMode(if (it) ThemeMode.DARK else ThemeMode.LIGHT) },
                    enabled = !settings.followSystemSettings
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mode daltonien",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Ajuste les couleurs pour améliorer la lisibilité (contraste élevé).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isColorBlindMode,
                    onCheckedChange = onColorBlindModeChange
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        bearerToken?.takeIf { it.isNotBlank() }?.let { token ->
            val clipboard = LocalClipboardManager.current
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("JWT token", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = if (tokenVisible) token else "**********",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        trailingIcon = {
                            TextButton(onClick = { tokenVisible = !tokenVisible }) {
                                Text(if (tokenVisible) "Masquer" else "Afficher")
                            }
                        }
                    )
                    TextButton(
                        onClick = { clipboard.setText(AnnotatedString(token)) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Copier")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        HorizontalDivider()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(Res.drawable.claco2_slogan_svg),
                    contentDescription = "ClacO₂",
                    modifier = Modifier
                        .width(140.dp)
                        .height(50.dp),
                    contentScale = ContentScale.Fit
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "v1.0.0 - ClacOxygen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun SettingsSliderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    value: Float,
    steps: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    enabled: Boolean
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Petit", style = MaterialTheme.typography.labelSmall)
            Text("Grand", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = isDark, onCheckedChange = onToggle, enabled = enabled)
    }
}

private fun nearestIndex(values: List<Float>, target: Float): Int {
    var bestIndex = 0
    var bestDelta = Float.MAX_VALUE
    values.forEachIndexed { index, value ->
        val delta = kotlin.math.abs(value - target)
        if (delta < bestDelta) {
            bestDelta = delta
            bestIndex = index
        }
    }
    return bestIndex
}
