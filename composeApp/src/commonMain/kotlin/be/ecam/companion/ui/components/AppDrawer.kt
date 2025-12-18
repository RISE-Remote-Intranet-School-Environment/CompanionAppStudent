package be.ecam.companion.ui.components
import be.ecam.companion.viewmodel.AuthUserDTO

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp


@Composable
fun AppDrawer(
    user: AuthUserDTO?,
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
            .widthIn(min = 140.dp, max = 200.dp)
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
                DrawerProfileSection(
                    user = user,
                    onSelectDashboard = onSelectDashboard
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onSelectCourses,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Formations") }

                Button(
                    onClick = onSelectPae,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Mon PAE") }

                Button(
                    onClick = onSelectProfessors,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Professeurs") }

                Spacer(Modifier.height(16.dp))
                DrawerExtraScrollableSection()
            }

            DrawerLogoutSection(onLogout)
        }
    }
}

@Composable
private fun DrawerProfileSection(
    user: AuthUserDTO?,
    onSelectDashboard: () -> Unit
) {
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
            val initial = user?.firstName?.firstOrNull()?.uppercase() 
                ?: user?.username?.firstOrNull()?.uppercase() 
                ?: ""
            
            if (!user?.avatarUrl.isNullOrBlank()) {
                // Utiliser Kamel pour charger l'avatar
                RemoteImage(
                    url = user?.avatarUrl ?: "",
                    contentDescription = "Avatar",
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = user?.firstName?.firstOrNull()?.uppercase()
                        ?: user?.username?.firstOrNull()?.uppercase()
                        ?: "",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column {
            val fullName = buildString {
                user?.firstName?.takeIf { it.isNotBlank() }?.let { append(it) }
                user?.lastName?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append(" ")
                    append(it)
                }
            }.ifEmpty { user?.username ?: "Utilisateur" }

            Text(
                text = buildString {
                    listOfNotNull(user?.firstName, user?.lastName)
                        .joinToString(" ")
                        .takeIf { it.isNotBlank() }
                        ?.let { append(it) }
                    if (isEmpty()) append(user?.username ?: "invité")
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = user?.email ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun DrawerExtraScrollableSection() {
    Column(modifier = Modifier.padding(top = 12.dp)) {
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
