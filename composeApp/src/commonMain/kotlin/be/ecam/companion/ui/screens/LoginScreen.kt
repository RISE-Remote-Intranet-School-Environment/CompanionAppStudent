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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ecam.companion.ui.components.EcamBackground
import be.ecam.companion.viewmodel.LoginViewModel


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel

) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val passwordFocusRequester = remember { FocusRequester() }
    val buttonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) onLoginSuccess()
    }

    // Fond ECAM
    EcamBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                LoginCard(
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    onLoginClick = {
                        if (email.isNotBlank() && password.isNotBlank())
                            viewModel.login(emailOrUsername = email, password = password)
                    },
                    onMicrosoftLoginClick = {},
                    passwordFocusRequester = passwordFocusRequester,
                    buttonFocusRequester = buttonFocusRequester
                )

                Spacer(Modifier.height(20.dp))

                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        "Pas encore de compte ? Inscrivez-vous",
                        color = Color.White
                    )
                }

                if (viewModel.isLoading) {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator(color = Color.White)
                }

                if (viewModel.errorMessage.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        viewModel.errorMessage,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun LoginCard(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onMicrosoftLoginClick: () -> Unit,
    passwordFocusRequester: FocusRequester,
    buttonFocusRequester: FocusRequester
) {
    Card(
        modifier = Modifier
            .width(420.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Connexion",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email ou nom d’utilisateur", color = Color.White) },
                singleLine = true,
                modifier = Modifier.width(350.dp),
                colors = TextFieldDefaults.colors(
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

            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Mot de passe", color = Color.White) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .width(350.dp)
                    .focusRequester(passwordFocusRequester),
                colors = TextFieldDefaults.colors(
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
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .width(350.dp)
                    .height(48.dp)
                    .focusRequester(buttonFocusRequester),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Se connecter")
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onMicrosoftLoginClick,
                modifier = Modifier
                    .width(350.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2F2F2F)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Se connecter avec Microsoft", color = Color.White)
            }
        }
    }
}
