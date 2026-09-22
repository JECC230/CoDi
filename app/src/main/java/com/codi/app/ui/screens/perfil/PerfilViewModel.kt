package com.codi.app.ui.screens.perfil

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codi.app.codiRepository
import com.codi.app.data.CoDiProfile
import com.codi.app.data.UserProfile
import com.codi.app.data.calculateAge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Copia editable del perfil; se compara contra el snapshot guardado para saber si hay cambios. */
data class ProfileDraft(
    val name: String = "",
    val lastName: String = "",
    /** ISO-8601 (`yyyy-MM-dd`), o "" si aún no se elige. */
    val birthDate: String = "",
    val occupation: String = "",
    val budget: String = "",
    val bio: String = "",
    val gender: String = "",
    val interests: Set<String> = emptySet()
) {
    companion object {
        fun from(user: UserProfile) = ProfileDraft(
            name = user.name,
            lastName = user.lastName,
            birthDate = user.birthDate,
            occupation = user.occupation,
            budget = user.budget.toString(),
            bio = user.bio,
            gender = user.gender,
            interests = user.interests.toSet()
        )
    }
}

data class PerfilUiState(
    val user: UserProfile? = null,
    val matches: List<CoDiProfile> = emptyList(),
    val draft: ProfileDraft = ProfileDraft(),
    val saved: Boolean = false,
    val saveError: String? = null
) {
    private val savedSnapshot: ProfileDraft? get() = user?.let(ProfileDraft::from)

    /** Hay cambios sin guardar en cualquier campo del perfil. */
    val isDirty: Boolean get() = savedSnapshot != null && draft != savedSnapshot

    val nameError: String? get() = if (draft.name.isBlank()) "El nombre no puede estar vacío" else null
    val lastNameError: String? get() = if (draft.lastName.isBlank()) "El apellido no puede estar vacío" else null
    val birthDateError: String?
        get() = when {
            draft.birthDate.isBlank() -> "Elige tu fecha de nacimiento"
            calculateAge(draft.birthDate) !in 18..99 -> "Debes tener entre 18 y 99 años"
            else -> null
        }
    val occupationError: String? get() = if (draft.occupation.isBlank()) "Ingresa tu ocupación" else null
    val budgetError: String?
        get() {
            val value = draft.budget.toIntOrNull()
            return when {
                draft.budget.isBlank() -> "Ingresa tu presupuesto"
                value == null || value < 0 -> "Presupuesto inválido"
                else -> null
            }
        }
    val bioError: String?
        get() = when {
            draft.bio.isBlank() -> "La biografía no puede estar vacía"
            draft.bio.length > PerfilViewModel.MAX_BIO_LENGTH -> "Máximo ${PerfilViewModel.MAX_BIO_LENGTH} caracteres"
            else -> null
        }

    val isValid: Boolean
        get() = nameError == null && lastNameError == null && birthDateError == null &&
            occupationError == null && budgetError == null && bioError == null

    val canSave: Boolean get() = isDirty && isValid
}

/**
 * Perfil del propio usuario (cuenta con sesión activa). Toda la edición
 * -nombre, apellido, fecha de nacimiento, ocupación, presupuesto, bio,
 * género, intereses y foto- se guarda en Cloud Firestore.
 */
class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.codiRepository

    /** `null` = todavía no se ha tocado ningún campo; se usa el snapshot de la base. */
    private val draftOverride = MutableStateFlow<ProfileDraft?>(null)
    private val saved = MutableStateFlow(false)
    private val saveError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PerfilUiState> =
        combine(
            repository.user,
            repository.matches,
            draftOverride,
            saved,
            saveError
        ) { user, matches, override, savedFlag, error ->
            PerfilUiState(
                user = user,
                matches = matches,
                draft = override ?: user?.let(ProfileDraft::from) ?: ProfileDraft(),
                saved = savedFlag,
                saveError = error
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PerfilUiState()
        )

    private fun updateDraft(transform: (ProfileDraft) -> ProfileDraft) {
        draftOverride.value = transform(uiState.value.draft)
        saved.value = false
    }

    fun onNameChange(value: String) = updateDraft { it.copy(name = value) }
    fun onLastNameChange(value: String) = updateDraft { it.copy(lastName = value) }
    fun onBirthDateChange(isoDate: String) = updateDraft { it.copy(birthDate = isoDate) }
    fun onOccupationChange(value: String) = updateDraft { it.copy(occupation = value) }

    fun onBudgetChange(value: String) {
        if (value.length <= 6 && value.all { it.isDigit() }) updateDraft { it.copy(budget = value) }
    }

    fun onBioChange(value: String) {
        if (value.length <= MAX_BIO_LENGTH) updateDraft { it.copy(bio = value) }
    }

    fun onGenderChange(value: String) = updateDraft { it.copy(gender = value) }

    fun onToggleInterest(interest: String) = updateDraft { draft ->
        val current = draft.interests.toMutableSet()
        if (!current.remove(interest)) current.add(interest)
        draft.copy(interests = current)
    }

    /** Descarta los cambios sin guardar y vuelve a mostrar lo que hay en la base (botón "Cancelar"). */
    fun discardDraft() {
        draftOverride.value = null
        saved.value = false
        saveError.value = null
    }

    /** Guarda todos los campos del perfil de un jalón. No hace nada si no pasa validación. */
    fun guardarPerfil() {
        val state = uiState.value
        if (!state.canSave) return
        val draft = state.draft
        viewModelScope.launch {
            try {
                repository.updateUserProfile(
                    name = draft.name.trim(),
                    lastName = draft.lastName.trim(),
                    birthDate = draft.birthDate,
                    occupation = draft.occupation.trim(),
                    budget = draft.budget.toInt(),
                    bio = draft.bio.trim(),
                    gender = draft.gender,
                    interests = draft.interests.toList()
                )
                draftOverride.value = null
                saved.value = true
            } catch (e: Exception) {
                saveError.value = "No se pudo guardar tu perfil, intenta de nuevo"
            }
        }
    }

    /** Sube la foto elegida de la galería al perfil en la nube (o la quita si [uri] es null). */
    fun updatePhoto(uri: Uri?) {
        viewModelScope.launch {
            try {
                repository.updateUserPhoto(uri)
            } catch (e: Exception) {
                saveError.value = "No se pudo actualizar tu foto, intenta de nuevo"
            }
        }
    }

    /** Se llama tras mostrar el error una vez, para que no reaparezca al rotar la pantalla. */
    fun errorShown() {
        saveError.value = null
    }

    /** Se llama tras mostrar la confirmación de guardado, para que no se repita. */
    fun savedShown() {
        saved.value = false
    }

    companion object {
        const val MAX_BIO_LENGTH = 280

        /** Opciones de estilo de vida seleccionables como chips en el editor de perfil. */
        val INTEREST_OPTIONS = listOf(
            "Mascotas", "No fumador", "Estudio nocturno", "Música/DJ", "Deportes",
            "Cocinar", "Yoga", "Gym", "Series", "Café", "Viajes", "Arte", "Videojuegos"
        )

        val GENDER_OPTIONS = listOf("Mujer", "Hombre", "Otro", "Prefiero no decir")

        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
