package com.codi.app.ui.screens.login

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codi.app.codiAuthRepository
import com.codi.app.data.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null
)

/** Login con Firebase Auth: correo/contraseña o Google (ver [com.codi.app.data.AuthRepository]). */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = application.codiAuthRepository
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(loading = true)
            when (val result = authRepository.login(email, password)) {
                is AuthResult.Success -> {
                    _uiState.value = LoginUiState()
                    onSuccess()
                }
                is AuthResult.Error -> _uiState.value = LoginUiState(error = result.message)
                AuthResult.Cancelled -> _uiState.value = LoginUiState()
            }
        }
    }

    /**
     * "Continuar con Google". [activityContext] tiene que ser la Activity
     * (Credential Manager muestra ahí el selector de cuentas); solo se usa
     * durante la llamada, el ViewModel no la guarda.
     */
    fun loginWithGoogle(activityContext: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(loading = true)
            when (val result = authRepository.loginWithGoogle(activityContext)) {
                is AuthResult.Success -> {
                    _uiState.value = LoginUiState()
                    onSuccess()
                }
                is AuthResult.Error -> _uiState.value = LoginUiState(error = result.message)
                AuthResult.Cancelled -> _uiState.value = LoginUiState()
            }
        }
    }

    fun errorShown() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
