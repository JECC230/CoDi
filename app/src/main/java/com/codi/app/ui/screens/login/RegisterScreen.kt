package com.codi.app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.codi.app.ui.components.BirthDateField
import com.codi.app.ui.components.CoDiPrimaryButton
import com.codi.app.ui.theme.BackgroundDark
import com.codi.app.ui.theme.GradientPink
import com.codi.app.ui.theme.SurfaceCard
import com.codi.app.ui.theme.TextMuted
import com.codi.app.ui.theme.TextPrimary
import com.codi.app.ui.theme.TextSecondary

/** Registro de cuenta: nombre, apellido, fecha de nacimiento, ocupación, bio y credenciales. */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Text("Crea tu cuenta", style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                modifier = Modifier.weight(1f),
                label = { Text("Nombre") },
                singleLine = true,
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { error -> { Text(error, color = GradientPink) } },
                shape = MaterialTheme.shapes.medium,
                colors = registerFieldColors()
            )
            OutlinedTextField(
                value = uiState.lastName,
                onValueChange = viewModel::onLastNameChange,
                modifier = Modifier.weight(1f),
                label = { Text("Apellido") },
                singleLine = true,
                isError = uiState.lastNameError != null,
                supportingText = uiState.lastNameError?.let { error -> { Text(error, color = GradientPink) } },
                shape = MaterialTheme.shapes.medium,
                colors = registerFieldColors()
            )
        }
        Spacer(Modifier.height(12.dp))

        BirthDateField(
            isoDate = uiState.birthDate,
            onDateSelected = viewModel::onBirthDateChange,
            isError = uiState.birthDateError != null,
            supportingText = uiState.birthDateError?.let { error -> { Text(error, color = GradientPink) } }
                ?: { Text("Debes ser mayor de edad", color = TextSecondary) },
            colors = registerFieldColors()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.occupation,
            onValueChange = viewModel::onOccupationChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ocupación") },
            singleLine = true,
            isError = uiState.occupationError != null,
            supportingText = uiState.occupationError?.let { error -> { Text(error, color = GradientPink) } },
            shape = MaterialTheme.shapes.medium,
            colors = registerFieldColors()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.bio,
            onValueChange = viewModel::onBioChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Cuéntanos de ti (opcional)") },
            minLines = 2,
            supportingText = { Text("${uiState.bio.length}/280", color = TextSecondary) },
            shape = MaterialTheme.shapes.medium,
            colors = registerFieldColors()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = uiState.emailError != null,
            supportingText = uiState.emailError?.let { error -> { Text(error, color = GradientPink) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = MaterialTheme.shapes.medium,
            colors = registerFieldColors()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Contraseña") },
            singleLine = true,
            isError = uiState.passwordError != null,
            supportingText = uiState.passwordError?.let { error -> { Text(error, color = GradientPink) } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = MaterialTheme.shapes.medium,
            colors = registerFieldColors()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirma tu contraseña") },
            singleLine = true,
            isError = uiState.confirmPasswordError != null,
            supportingText = uiState.confirmPasswordError?.let { error -> { Text(error, color = GradientPink) } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = MaterialTheme.shapes.medium,
            colors = registerFieldColors()
        )

        if (uiState.submitError != null) {
            Spacer(Modifier.height(8.dp))
            Text(uiState.submitError.orEmpty(), style = MaterialTheme.typography.labelMedium, color = GradientPink)
        }

        Spacer(Modifier.height(20.dp))
        CoDiPrimaryButton(
            text = if (uiState.loading) "Creando cuenta..." else "Crear cuenta",
            enabled = !uiState.loading,
            onClick = { viewModel.register(onSuccess = onRegisterSuccess) },
            modifier = Modifier.fillMaxWidth().height(54.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun registerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceCard,
    unfocusedContainerColor = SurfaceCard,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextMuted
)
