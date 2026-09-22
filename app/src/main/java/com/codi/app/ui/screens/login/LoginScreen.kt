package com.codi.app.ui.screens.login

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.codi.app.R
import com.codi.app.ui.components.CoDiPrimaryButton
import com.codi.app.ui.theme.BackgroundDark
import com.codi.app.ui.theme.GradientPink
import com.codi.app.ui.theme.SurfaceCard
import com.codi.app.ui.theme.TextMuted
import com.codi.app.ui.theme.TextPrimary
import com.codi.app.ui.theme.TextSecondary

private const val MIN_PASSWORD_LENGTH = 6

/**
 * Valida el formato de los campos (no las credenciales en sí). Devuelve el
 * mensaje de error a mostrar bajo cada campo, o `null` si es válido.
 */
private fun emailFormatError(email: String): String? = when {
    email.isBlank() -> "Ingresa tu correo"
    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Correo inválido"
    else -> null
}

private fun passwordFormatError(password: String): String? = when {
    password.isBlank() -> "Ingresa tu contraseña"
    password.length < MIN_PASSWORD_LENGTH -> "Mínimo $MIN_PASSWORD_LENGTH caracteres"
    else -> null
}

/**
 * Pantalla de inicio de sesión con Firebase Auth: correo/contraseña o
 * Google. Valida el formato de los campos en tiempo real (correo bien
 * formado, contraseña con longitud mínima) antes de intentar entrar.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    // Validación en tiempo real: cada campo muestra su error en cuanto el
    // usuario empieza a escribir en él (o al intentar entrar con él vacío).
    var submitted by remember { mutableStateOf(false) }
    var emailEdited by remember { mutableStateOf(false) }
    var passwordEdited by remember { mutableStateOf(false) }

    val emailError = if (submitted || emailEdited) emailFormatError(email) else null
    val passwordError = if (submitted || passwordEdited) passwordFormatError(password) else null
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_codi_logo),
            contentDescription = "CoDi Logo",
            modifier = Modifier.size(100.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("CoDi", style = MaterialTheme.typography.headlineLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Encuentra a tu CoDi ideal", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

        Spacer(Modifier.height(36.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it.trim()
                emailEdited = true
                viewModel.errorShown()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Outlined.MailOutline, contentDescription = null, tint = TextSecondary) },
            singleLine = true,
            isError = emailError != null,
            supportingText = emailError?.let { error -> { Text(error, color = GradientPink) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = MaterialTheme.shapes.medium,
            colors = loginFieldColors()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordEdited = true
                viewModel.errorShown()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextSecondary) },
            singleLine = true,
            isError = passwordError != null,
            supportingText = passwordError?.let { error -> { Text(error, color = GradientPink) } },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = TextSecondary
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = MaterialTheme.shapes.medium,
            colors = loginFieldColors()
        )

        AnimatedVisibility(
            visible = uiState.error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                Text(
                    uiState.error.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = GradientPink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Error: ${uiState.error.orEmpty()}" }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        CoDiPrimaryButton(
            text = if (uiState.loading) "Entrando..." else "Iniciar sesión",
            enabled = !uiState.loading,
            onClick = {
                submitted = true
                val validFormat = emailFormatError(email) == null && passwordFormatError(password) == null
                if (validFormat) {
                    viewModel.login(email, password, onSuccess = onLoginSuccess)
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp)
        )

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { viewModel.loginWithGoogle(context, onSuccess = onLoginSuccess) },
            enabled = !uiState.loading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (uiState.loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("Continuar con Google", color = TextPrimary)
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("¿No tienes cuenta?", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            TextButton(onClick = onNavigateToRegister) {
                Text("Crear cuenta", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceCard,
    unfocusedContainerColor = SurfaceCard,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextMuted
)
