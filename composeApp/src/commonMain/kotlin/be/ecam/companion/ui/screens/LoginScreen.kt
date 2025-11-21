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
import be.ecam.companion.viewmodel.LoginViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit, // ← nouveau paramètre
    viewModel: LoginViewModel = remember { LoginViewModel() }
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val passwordFocusRequester = remember { FocusRequester() }
    val buttonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) {
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoginCard(
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                onLoginClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        viewModel.login(emailOrUsername = email, password = password)
                    }
                },
                onMicrosoftLoginClick = { /* OAuth Microsoft */ },
                passwordFocusRequester = passwordFocusRequester,
                buttonFocusRequester = buttonFocusRequester
            )

            Spacer(Modifier.height(16.dp))

            // Bouton pour aller à l'inscription
            TextButton(onClick = onNavigateToRegister) {
                Text("Pas encore de compte ? Inscrivez-vous")
            }

            Spacer(Modifier.height(16.dp))

            if (viewModel.isLoading) {
                CircularProgressIndicator()
            } else if (viewModel.errorMessage.isNotEmpty()) {
                Text(
                    text = viewModel.errorMessage,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
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
            .width(450.dp)
            .border(2.dp, Color.Gray, shape = RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Connexion",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(24.dp))

            // Champ email
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email ou nom d’utilisateur") },
                singleLine = true,
                modifier = Modifier
                    .width(400.dp)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                            onLoginClick()
                            true
                        } else false
                    }
            )

            Spacer(Modifier.height(12.dp))

            // Champ mot de passe
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Mot de passe") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .width(400.dp)
                    .focusRequester(passwordFocusRequester)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                            onLoginClick()
                            true
                        } else false
                    }
            )

            Spacer(Modifier.height(24.dp))

            // Bouton de connexion
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .width(400.dp)
                    .height(50.dp)
                    .focusRequester(buttonFocusRequester),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Se connecter")
            }

            Spacer(Modifier.height(16.dp))

            

            // Bouton Microsoft
            Button(
                onClick = onMicrosoftLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F2F2F)),
                modifier = Modifier
                    .width(400.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Se connecter avec Microsoft", color = Color.White)
            }
        }
    }
}
