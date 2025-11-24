package be.ecam.companion.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import companion.composeapp.generated.resources.Res
import companion.composeapp.generated.resources.nicolas

@Composable
fun AppDrawer(
    onSelectDashboard: () -> Unit,
    onSelectCourses: () -> Unit,
    onSelectProfessors: () -> Unit,
    onSelectPae: () -> Unit,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 140.dp, max = 200.dp) // Max drawer size
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                DrawerProfileSection(onSelectDashboard)
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onSelectCourses,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Formations")
                }

                Button(
                    onClick = onSelectPae,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Mon PAE")
                }

                Button(
                    onClick = onSelectProfessors,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Professeurs")
                }

                Spacer(Modifier.height(16.dp))
                DrawerExtraScrollableSection()
            }

            DrawerLogoutSection(onLogout)
        }
    }
}

@Composable
private fun DrawerProfileSection(onSelectDashboard: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectDashboard() }
            .padding(bottom = 8.dp)
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
                modifier = Modifier.size(56.dp).clip(CircleShape)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("Nicolas Schell")
    }
}

@Composable
private fun DrawerExtraScrollableSection() {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text("Drawer content here")
        Spacer(Modifier.height(400.dp)) // Exemple, à remplacer par un vrai contenu
    }
}

@Composable
private fun DrawerLogoutSection(onLogout: () -> Unit) {
    Column {
        HorizontalDivider()
        TextButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}
