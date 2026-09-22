package com.codi.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codi.app.codiAuthRepository
import com.codi.app.data.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val savingPassword: Boolean = false,
    val passwordMessage: String? = null
)

/** Cuenta y seguridad, dentro de Ajustes: cambio de contraseña y cierre de sesión. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = application.codiAuthRepository
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(savingPassword = true)
            val message = when (val result = authRepository.changePassword(newPassword)) {
                is AuthResult.Success -> "Contraseña actualizada"
                is AuthResult.Error -> result.message
                AuthResult.Cancelled -> null
            }
            _uiState.value = SettingsUiState(passwordMessage = message)
        }
    }

    fun messageShown() {
        _uiState.value = _uiState.value.copy(passwordMessage = null)
    }

    /** Cierra la sesión activa; la navegación de vuelta a Login la hace la pantalla. */
    fun logout() = authRepository.logout()
}
