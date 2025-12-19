package be.ecam.companion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    isColorBlindMode: Boolean,
    onColorBlindModeChange: (Boolean) -> Unit,
    bearerToken: String? = null,
    modifier: Modifier = Modifier
 ) {
    var tokenVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mode Daltonien",
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
                        value = if (tokenVisible) token else "••••••••••",
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
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Companion App Student v1.0.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
