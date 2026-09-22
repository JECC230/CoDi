package com.codi.app.ui.screens.login

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codi.app.codiAuthRepository
import com.codi.app.data.AuthResult
import com.codi.app.data.calculateAge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MIN_PASSWORD_LENGTH = 6
private const val MIN_AGE = 18
private const val MAX_AGE = 99
private const val MAX_BIO_LENGTH = 280

data class RegisterUiState(
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    /** ISO-8601 (`yyyy-MM-dd`), o "" si aún no se elige. */
    val birthDate: String = "",
    val occupation: String = "",
    val bio: String = "",
    val loading: Boolean = false,
    val submitError: String? = null,
    /** Solo se muestran errores de validación después del primer intento de enviar. */
    val touched: Boolean = false
) {
    val nameError: String?
        get() = if (touched && name.isBlank()) "Ingresa tu nombre" else null

    val lastNameError: String?
        get() = if (touched && lastName.isBlank()) "Ingresa tu apellido" else null

    val emailError: String?
        get() = if (!touched) null else when {
            email.isBlank() -> "Ingresa tu correo"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Correo inválido"
            else -> null
        }

    val passwordError: String?
        get() = if (!touched) null else when {
            password.isBlank() -> "Ingresa una contraseña"
            password.length < MIN_PASSWORD_LENGTH -> "Mínimo $MIN_PASSWORD_LENGTH caracteres"
            else -> null
        }

    val confirmPasswordError: String?
        get() = if (!touched) null else when {
            confirmPassword != password -> "Las contraseñas no coinciden"
            else -> null
        }

    val birthDateError: String?
        get() = if (!touched) null else {
            when {
                birthDate.isBlank() -> "Elige tu fecha de nacimiento"
                calculateAge(birthDate) !in MIN_AGE..MAX_AGE -> "Debes tener entre $MIN_AGE y $MAX_AGE años"
                else -> null
            }
        }

    val occupationError: String?
        get() = if (touched && occupation.isBlank()) "Ingresa tu ocupación" else null

    val isValid: Boolean
        get() = name.isNotBlank() && lastName.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
            password.length >= MIN_PASSWORD_LENGTH && confirmPassword == password &&
            birthDate.isNotBlank() && calculateAge(birthDate) in MIN_AGE..MAX_AGE &&
            occupation.isNotBlank()
}

/**
 * Registro de cuenta con Firebase Auth + perfil en Firestore (ver
 * [com.codi.app.data.AuthRepository.register]).
 * Valida en tiempo real conforme el usuario escribe, pero solo muestra los
 * mensajes de error después del primer intento de enviar el formulario.
 */
class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = application.codiAuthRepository
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = update { it.copy(name = value, submitError = null) }
    fun onLastNameChange(value: String) = update { it.copy(lastName = value, submitError = null) }
    fun onEmailChange(value: String) = update { it.copy(email = value, submitError = null) }
    fun onPasswordChange(value: String) = update { it.copy(password = value, submitError = null) }
    fun onConfirmPasswordChange(value: String) = update { it.copy(confirmPassword = value, submitError = null) }
    fun onBirthDateChange(isoDate: String) = update { it.copy(birthDate = isoDate, submitError = null) }
    fun onOccupationChange(value: String) = update { it.copy(occupation = value, submitError = null) }
    fun onBioChange(value: String) {
        if (value.length <= MAX_BIO_LENGTH) update { it.copy(bio = value) }
    }

    fun register(onSuccess: () -> Unit) {
        update { it.copy(touched = true) }
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            update { it.copy(loading = true) }
            val result = authRepository.register(
                email = state.email,
                password = state.password,
                name = state.name.trim(),
                lastName = state.lastName.trim(),
                birthDate = state.birthDate,
                occupation = state.occupation.trim(),
                bio = state.bio.trim()
            )
            when (result) {
                is AuthResult.Success -> {
                    update { it.copy(loading = false) }
                    onSuccess()
                }
                is AuthResult.Error -> update { it.copy(loading = false, submitError = result.message) }
                AuthResult.Cancelled -> update { it.copy(loading = false) }
            }
        }
    }

    private inline fun update(transform: (RegisterUiState) -> RegisterUiState) {
        _uiState.value = transform(_uiState.value)
    }
}
