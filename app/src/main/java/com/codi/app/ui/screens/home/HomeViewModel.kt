package com.codi.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codi.app.codiRepository
import com.codi.app.data.CoDiProfile
import com.codi.app.data.UserProfile
import com.codi.app.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val deck: List<CoDiProfile> = emptyList(),
    val user: UserProfile? = null
) {
    /** Tarjeta que se está mostrando: siempre la primera del mazo. */
    val currentProfile: CoDiProfile? get() = deck.firstOrNull()
}

/**
 * Maneja el mazo de perfiles del swipe de Home. El mazo se lee de la base
 * de datos (solo los perfiles con estado `PENDIENTE`), así que tanto
 * "Pasar" como "Me Interesa" quedan guardados en SQLite y el mazo se ve
 * igual al volver a abrir la app.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.codiRepository

    /** Aviso de un guardado rechazado por el servidor; la pantalla lo muestra una vez. */
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val onSaveFailure: (Exception) -> Unit = {
        _saveError.value = "No se pudo guardar tu decisión. Revisa tu conexión e intenta de nuevo."
    }

    fun saveErrorShown() {
        _saveError.value = null
    }

    val uiState: StateFlow<HomeUiState> =
        combine(repository.deck, repository.user) { deck, user ->
            HomeUiState(loading = false, deck = deck, user = user)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState()
        )

    /** Descarta el perfil actual: no volverá a aparecer en el mazo. */
    fun pasar() {
        val profile = uiState.value.currentProfile ?: return
        repository.markAsPasado(profile.id, onSaveFailure)
    }

    /**
     * Registra el match con el perfil actual y lo devuelve para que la
     * pantalla pueda navegar a la pantalla de Match.
     */
    fun meInteresa(): CoDiProfile? {
        val profile = uiState.value.currentProfile ?: return null
        repository.markAsInteresa(profile.id, onSaveFailure)
        NotificationHelper.notifyNewMatch(getApplication(), profile.name)
        return profile
    }

    /** Regresa todos los perfiles al mazo cuando ya se revisaron todos. */
    fun reiniciarMazo() {
        viewModelScope.launch { repository.resetDeck() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
