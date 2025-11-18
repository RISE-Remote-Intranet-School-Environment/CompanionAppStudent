package be.ecam.companion.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import companion.composeapp.generated.resources.Res
import companion.composeapp.generated.resources.nicolas
import be.ecam.companion.ui.components.BottomItem

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    scope: CoroutineScope,
    onNavigateToDashboard: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onLogout: () -> Unit
) {
    val scroll = rememberScrollState()

    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // --- Top section ---
            Column {

                // --- Profile clickable area ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable {
                            onNavigateToDashboard()
                            scope.launch { drawerState.close() }
                        }
                ) {

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.nicolas),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(Modifier.width(8.dp))
                    Text("Nicolas Schell")
                }

                // --- Formations button ---
                Button(
                    onClick = {
                        onNavigateToCourses()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Formations")
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Middle scrollable content ---
            Column(modifier = Modifier.verticalScroll(scroll)) {
                Text("Drawer content here")
            }

            // --- Bottom section ---
            Column {
                HorizontalDivider()
                TextButton(
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    }
                ) { Text("Logout") }
            }
        }
    }
}
