package com.codi.app.ui.screens.match

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codi.app.codiRepository
import com.codi.app.data.CoDiProfile
import com.codi.app.data.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MatchUiState(
    val profile: CoDiProfile? = null,
    val user: UserProfile? = null
) {
    /** Intereses que el usuario y su nuevo CoDi tienen en común. */
    val sharedInterests: List<String>
        get() {
            val mine = user?.interests ?: return emptyList()
            val theirs = profile?.interests ?: return emptyList()
            return theirs.filter { it in mine }.ifEmpty { theirs.take(3) }
        }
}

/** Pantalla de match: lee de la base el perfil con el que se hizo match. */
@OptIn(ExperimentalCoroutinesApi::class)
class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.codiRepository
    private val profileId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MatchUiState> =
        combine(
            profileId.flatMapLatest { id ->
                if (id == null) flowOf(null) else repository.profileById(id)
            },
            repository.user
        ) { profile, user ->
            MatchUiState(profile = profile, user = user)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MatchUiState()
        )

    fun load(id: String) {
        profileId.value = id
    }

    /**
     * Crea la conversación con el nuevo CoDi si aún no existe, para que al
     * entrar al chat ya haya con qué empezar.
     */
    fun prepararChat(contactName: String) {
        viewModelScope.launch { repository.ensureConversation(contactName) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
