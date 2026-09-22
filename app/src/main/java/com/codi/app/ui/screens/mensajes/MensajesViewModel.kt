package com.codi.app.ui.screens.mensajes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codi.app.codiRepository
import com.codi.app.data.Conversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MensajesUiState(
    val query: String = "",
    val conversations: List<Conversation> = emptyList()
)

/** Lista de conversaciones leída de la base de datos, con búsqueda. */
class MensajesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.codiRepository
    private val query = MutableStateFlow("")

    val uiState: StateFlow<MensajesUiState> =
        combine(repository.conversations, query) { conversations, text ->
            MensajesUiState(
                query = text,
                conversations = if (text.isBlank()) {
                    conversations
                } else {
                    conversations.filter {
                        it.contactName.contains(text, ignoreCase = true) ||
                            it.preview.contains(text, ignoreCase = true)
                    }
                }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MensajesUiState()
        )

    fun onQueryChange(text: String) {
        query.value = text
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
