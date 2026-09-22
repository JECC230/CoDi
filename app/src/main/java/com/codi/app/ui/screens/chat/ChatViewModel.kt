package com.codi.app.ui.screens.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codi.app.codiRepository
import com.codi.app.data.ChatMessage
import com.codi.app.notifications.NewMessageWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val contactName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    /** Mensaje de error a mostrar una sola vez (ver [ChatViewModel.errorShown]). */
    val error: String? = null
)

/**
 * Estado de una conversación de Chat. Los mensajes se leen y se escriben en
 * la base de datos, así que lo que el usuario envía sigue ahí la próxima
 * vez que abre el chat.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.codiRepository
    private val contactName = MutableStateFlow("")
    private val input = MutableStateFlow("")
    private val error = MutableStateFlow<String?>(null)

    private val messages = contactName.flatMapLatest { name ->
        if (name.isBlank()) flowOf(emptyList()) else repository.messagesOf(name)
    }

    val uiState: StateFlow<ChatUiState> =
        combine(contactName, messages, input, error) { name, messageList, text, err ->
            ChatUiState(contactName = name, messages = messageList, input = text, error = err)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ChatUiState()
        )

    /**
     * Abre la conversación con [name]. Si es un match nuevo que todavía no
     * tenía chat, el repositorio la crea; además se marca como leída.
     */
    fun load(name: String) {
        if (contactName.value == name) return
        contactName.value = name
        viewModelScope.launch {
            repository.ensureConversation(name)
            repository.markConversationAsRead(name)
        }
    }

    fun onInputChange(text: String) {
        input.value = text
    }

    fun sendMessage() {
        val text = input.value.trim()
        val name = contactName.value
        if (text.isEmpty() || name.isBlank()) return
        if (text.length > MAX_MESSAGE_LENGTH) {
            error.value = "El mensaje es demasiado largo (máximo $MAX_MESSAGE_LENGTH caracteres)"
            return
        }
        input.value = ""
        viewModelScope.launch {
            try {
                repository.sendMessage(name, text)
                // Simula que el otro CoDi contesta unos segundos después.
                NewMessageWorker.schedule(getApplication(), name)
            } catch (e: Exception) {
                error.value = "No se pudo enviar el mensaje, intenta de nuevo"
            }
        }
    }

    /** Se llama tras mostrar el error una vez, para que no reaparezca al rotar la pantalla. */
    fun errorShown() {
        error.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MAX_MESSAGE_LENGTH = 500
    }
}
