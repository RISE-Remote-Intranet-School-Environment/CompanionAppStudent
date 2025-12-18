package be.ecam.companion.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ecam.companion.viewmodel.LoginViewModel
import be.ecam.companion.ui.components.EcamBackground

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: LoginViewModel = remember { LoginViewModel() }
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) onRegisterSuccess()
    }

    EcamBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            RegisterCard(
                username = username,
                onUsernameChange = { username = it },
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                onRegisterClick = {
                    if (username.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                        viewModel.register(username, email, password)
                    }
                },
                onNavigateToLogin = onNavigateToLogin
            )
        }
    }
}


@Composable
fun RegisterCard(
    username: String,
    onUsernameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Card(
        modifier = Modifier.width(420.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.18f) // 👈 moins transparent
        )
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            @Composable
            fun registerTextFieldColors() = TextFieldDefaults.colors(
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = Color.White.copy(alpha = 0.6f),
                cursorColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
            Text(
                "Inscription",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Nom d’utilisateur", color = Color.White) },
                singleLine = true,
                modifier = Modifier.width(350.dp),
                colors = registerTextFieldColors()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email", color = Color.White) },
                singleLine = true,
                modifier = Modifier.width(350.dp),
                colors = registerTextFieldColors()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Mot de passe", color = Color.White) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.width(350.dp),
                colors = registerTextFieldColors()
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onRegisterClick,
                modifier = Modifier
                    .width(350.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("S'inscrire")
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text(
                    "Déjà un compte ? Connectez-vous",
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

