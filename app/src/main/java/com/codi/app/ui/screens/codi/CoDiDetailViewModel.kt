package com.codi.app.ui.screens.codi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codi.app.codiRepository
import com.codi.app.data.CoDiProfile
import com.codi.app.notifications.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Detalle de un candidato a CoDi. El perfil se lee de la base de datos por id. */
@OptIn(ExperimentalCoroutinesApi::class)
class CoDiDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.codiRepository
    private val profileId = MutableStateFlow<String?>(null)

    val profile: StateFlow<CoDiProfile?> = profileId
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.profileById(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    fun load(id: String) {
        profileId.value = id
    }

    /** Registra el match desde el detalle. */
    fun meInteresa() {
        val id = profileId.value ?: return
        viewModelScope.launch { repository.markAsInteresa(id) }
        profile.value?.name?.let { NotificationHelper.notifyNewMatch(getApplication(), it) }
    }

    /** Descarta el perfil desde el detalle. */
    fun pasar() {
        val id = profileId.value ?: return
        viewModelScope.launch { repository.markAsPasado(id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
