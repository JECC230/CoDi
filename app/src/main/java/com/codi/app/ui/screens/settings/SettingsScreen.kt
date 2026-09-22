package com.codi.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.codi.app.data.ThemeMode
import com.codi.app.data.ThemePreferences
import com.codi.app.ui.components.CoDiPrimaryButton
import com.codi.app.ui.components.FilterChip
import com.codi.app.ui.theme.AccentBlue
import com.codi.app.ui.theme.BackgroundDark
import com.codi.app.ui.theme.GradientPink
import com.codi.app.ui.theme.SurfaceCard
import com.codi.app.ui.theme.TextMuted
import com.codi.app.ui.theme.TextPrimary
import com.codi.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Ajustes de la cuenta, estilo Instagram: apariencia (tema), soporte/ayuda
 * y cuenta y seguridad. Se llega aquí desde el ícono de engrane en Perfil.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeMode by ThemePreferences.themeMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    LaunchedEffect(uiState.passwordMessage) {
        val message = uiState.passwordMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.messageShown()
    }

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
                }
                Text("Ajustes", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionTitle("Apariencia")
                Spacer(Modifier.height(8.dp))
                Surface(color = SurfaceCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = AccentBlue)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Tema", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                Text(
                                    when (themeMode) {
                                        ThemeMode.SYSTEM -> "Sigue el tema del dispositivo automáticamente"
                                        ThemeMode.LIGHT -> "Claro"
                                        ThemeMode.DARK -> "Oscuro"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                text = "Sistema",
                                selected = themeMode == ThemeMode.SYSTEM,
                                onClick = { ThemePreferences.setThemeMode(ThemeMode.SYSTEM) }
                            )
                            FilterChip(
                                text = "Claro",
                                selected = themeMode == ThemeMode.LIGHT,
                                onClick = { ThemePreferences.setThemeMode(ThemeMode.LIGHT) }
                            )
                            FilterChip(
                                text = "Oscuro",
                                selected = themeMode == ThemeMode.DARK,
                                onClick = { ThemePreferences.setThemeMode(ThemeMode.DARK) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                SectionTitle("Soporte y ayuda")
                Spacer(Modifier.height(8.dp))
                SettingsRow(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    title = "Centro de ayuda",
                    onClick = { dialog = SettingsDialog.HELP }
                )
                SettingsRow(
                    icon = Icons.Outlined.BugReport,
                    title = "Reportar un problema",
                    onClick = { dialog = SettingsDialog.REPORT }
                )
                SettingsRow(
                    icon = Icons.AutoMirrored.Outlined.Article,
                    title = "Términos y condiciones",
                    onClick = { dialog = SettingsDialog.TERMS }
                )

                Spacer(Modifier.height(24.dp))
                SectionTitle("Cuenta y seguridad")
                Spacer(Modifier.height(8.dp))
                SettingsRow(
                    icon = Icons.Outlined.Lock,
                    title = "Cambiar contraseña",
                    onClick = { dialog = SettingsDialog.CHANGE_PASSWORD }
                )

                Spacer(Modifier.height(32.dp))
                CoDiPrimaryButton(
                    text = "Cerrar sesión",
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    when (dialog) {
        SettingsDialog.HELP -> InfoDialog(
            title = "Centro de ayuda",
            body = "CoDi es un prototipo escolar sin soporte real. En una versión publicada, aquí encontrarías preguntas frecuentes sobre cómo hacer match, editar tu perfil y usar el chat.",
            onDismiss = { dialog = null }
        )
        SettingsDialog.REPORT -> InfoDialog(
            title = "Reportar un problema",
            body = "Este proyecto no tiene backend ni buzón de soporte real. En una versión publicada, este botón enviaría tu reporte (con capturas y datos del dispositivo) a nuestro equipo.",
            onDismiss = { dialog = null }
        )
        SettingsDialog.TERMS -> InfoDialog(
            title = "Términos y condiciones",
            body = "CoDi es un proyecto educativo. Todos los perfiles del mazo son ficticios. Tu cuenta se administra con Firebase Authentication y tu perfil (nombre, fecha de nacimiento, bio, intereses) se guarda en Cloud Firestore; tus matches y chats simulados se quedan en este dispositivo. No compartimos tus datos con terceros.",
            onDismiss = { dialog = null }
        )
        SettingsDialog.CHANGE_PASSWORD -> ChangePasswordDialog(
            saving = uiState.savingPassword,
            onConfirm = { newPassword ->
                viewModel.changePassword(newPassword)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        null -> Unit
    }
}

private enum class SettingsDialog { HELP, REPORT, TERMS, CHANGE_PASSWORD }

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        color = SurfaceCard,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AccentBlue)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido") }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    saving: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val error = when {
        newPassword.isNotEmpty() && newPassword.length < 6 -> "Mínimo 6 caracteres"
        confirmPassword.isNotEmpty() && confirmPassword != newPassword -> "Las contraseñas no coinciden"
        else -> null
    }
    val canConfirm = newPassword.length >= 6 && newPassword == confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar contraseña") },
        text = {
            Column {
                Text(
                    "Se actualiza tu contraseña en Firebase. Si iniciaste sesión hace mucho, te pediremos volver a entrar antes de cambiarla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nueva contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null && newPassword.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null && confirmPassword.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(error, style = MaterialTheme.typography.labelSmall, color = GradientPink)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newPassword) }, enabled = canConfirm && !saving) {
                Text(if (saving) "Guardando..." else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
