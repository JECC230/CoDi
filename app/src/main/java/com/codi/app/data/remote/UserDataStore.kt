package com.codi.app.data.remote

import android.util.Log
import com.codi.app.data.ChatMessage
import com.codi.app.data.Conversation
import com.codi.app.data.SwipeStatus
import com.codi.app.data.formatMessageTime
import com.codi.app.data.formatRelativeTime
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Todo lo que el usuario hace en CoDi, guardado en Cloud Firestore bajo su
 * propio documento, para que lo vea igual en cualquier dispositivo:
 *
 * ```
 * users/{uid}
 * ├── swipes/{profileId}            status ("INTERESA" | "PASADO"), updatedAt
 * └── chats/{contactName}           contactName, colorSeed, unread, lastMessage, updatedAt
 *     └── messages/{autoId}         text, fromMe, createdAt
 * ```
 *
 * Una cuenta nueva empieza sin chats: cada conversación nace cuando el
 * usuario interactúa (hace match o escribe).
 *
 * Firestore trae caché offline: las escrituras aparecen al instante en la UI
 * (los listeners disparan con el cambio local) y se sincronizan solas en
 * cuanto hay red, igual que en WhatsApp o Instagram. Por eso las escrituras
 * de aquí no esperan la confirmación del servidor.
 */
class UserDataStore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun user(uid: String) = firestore.collection(USERS).document(uid)
    private fun swipes(uid: String) = user(uid).collection(SWIPES)
    private fun chats(uid: String) = user(uid).collection(CHATS)
    private fun messages(uid: String, contactName: String) =
        chats(uid).document(contactName).collection(MESSAGES)

    // --- Swipes (mazo de Home y matches) ---

    /** Decisión tomada sobre cada perfil del catálogo, por id de perfil. */
    fun observeSwipes(uid: String): Flow<Map<String, SwipeStatus>> = callbackFlow {
        val registration = swipes(uid).addSnapshotListener { snapshot, error ->
            if (error != null) Log.w(TAG, "No se pudieron leer los swipes", error)
            val decisions = snapshot?.documents.orEmpty().associate { doc ->
                doc.id to SwipeStatus.from(doc.getString(FIELD_STATUS).orEmpty())
            }
            trySend(decisions)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Guarda la decisión. [onFailure] avisa si el servidor la rechaza (por
     * ejemplo por reglas de seguridad): Firestore ya la había aplicado en la
     * caché local y la revierte, así que la UI debe decírselo al usuario.
     */
    fun setSwipe(uid: String, profileId: String, status: SwipeStatus, onFailure: (Exception) -> Unit = {}) {
        swipes(uid).document(profileId)
            .set(mapOf(FIELD_STATUS to status.name, FIELD_UPDATED_AT to System.currentTimeMillis()))
            .addOnFailureListener {
                Log.w(TAG, "No se pudo guardar swipe", it)
                onFailure(it)
            }
    }

    /** "Reiniciar mazo": borra todas las decisiones de un jalón. */
    suspend fun clearSwipes(uid: String) {
        val snapshot = swipes(uid).get().await()
        snapshot.documents.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().logFailure("reiniciar mazo")
        }
    }

    // --- Chats ---

    fun observeConversations(uid: String): Flow<List<Conversation>> = callbackFlow {
        val registration = chats(uid)
            .orderBy(FIELD_UPDATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) Log.w(TAG, "No se pudieron leer los chats", error)
                val conversations = snapshot?.documents.orEmpty().map { doc ->
                    Conversation(
                        contactName = doc.getString(FIELD_CONTACT_NAME) ?: doc.id,
                        lastActivity = formatRelativeTime(doc.getLong(FIELD_UPDATED_AT) ?: 0L),
                        unread = doc.getBoolean(FIELD_UNREAD) ?: false,
                        colorSeed = doc.getLong(FIELD_COLOR_SEED)?.toInt() ?: 0,
                        preview = doc.getString(FIELD_LAST_MESSAGE).orEmpty()
                    )
                }
                trySend(conversations)
            }
        awaitClose { registration.remove() }
    }

    fun observeMessages(uid: String, contactName: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = messages(uid, contactName)
            .orderBy(FIELD_CREATED_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) Log.w(TAG, "No se pudieron leer los mensajes", error)
                val list = snapshot?.documents.orEmpty().map { doc ->
                    val createdAt = doc.getLong(FIELD_CREATED_AT) ?: 0L
                    ChatMessage(
                        id = doc.id,
                        text = doc.getString(FIELD_TEXT).orEmpty(),
                        fromMe = doc.getBoolean(FIELD_FROM_ME) ?: false,
                        timestamp = formatMessageTime(createdAt)
                    )
                }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Crea el chat con [contactName] si aún no existe (match nuevo), con un
     * primer saludo del otro CoDi para que el chat nunca aparezca vacío.
     */
    suspend fun ensureConversation(uid: String, contactName: String, colorSeed: Int) {
        val chat = chats(uid).document(contactName)
        val exists = runCatching { chat.get().await().exists() }.getOrDefault(false)
        if (exists) return
        addMessage(uid, contactName, colorSeed, "¡Hola! Vi que hicimos match, me da gusto 👋", fromMe = false, markUnread = false)
    }

    /**
     * Agrega un mensaje y actualiza el resumen del chat (último mensaje,
     * fecha, no leído) en una sola escritura atómica.
     */
    fun addMessage(
        uid: String,
        contactName: String,
        colorSeed: Int,
        text: String,
        fromMe: Boolean,
        markUnread: Boolean,
        createdAt: Long = System.currentTimeMillis()
    ) {
        val batch = firestore.batch()
        batch.set(
            messages(uid, contactName).document(),
            mapOf(FIELD_TEXT to text, FIELD_FROM_ME to fromMe, FIELD_CREATED_AT to createdAt)
        )
        val summary = mutableMapOf<String, Any>(
            FIELD_CONTACT_NAME to contactName,
            FIELD_COLOR_SEED to colorSeed,
            FIELD_LAST_MESSAGE to text,
            FIELD_UPDATED_AT to createdAt
        )
        if (markUnread) summary[FIELD_UNREAD] = true
        batch.set(chats(uid).document(contactName), summary, SetOptions.merge())
        batch.commit().logFailure("enviar mensaje")
    }

    fun markAsRead(uid: String, contactName: String) {
        chats(uid).document(contactName)
            .set(mapOf(FIELD_UNREAD to false), SetOptions.merge())
            .logFailure("marcar como leído")
    }

    private fun <T> com.google.android.gms.tasks.Task<T>.logFailure(action: String) {
        addOnFailureListener { Log.w(TAG, "No se pudo $action", it) }
    }

    private companion object {
        const val TAG = "UserDataStore"
        const val BATCH_LIMIT = 450

        const val USERS = "users"
        const val SWIPES = "swipes"
        const val CHATS = "chats"
        const val MESSAGES = "messages"

        const val FIELD_STATUS = "status"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_CONTACT_NAME = "contactName"
        const val FIELD_COLOR_SEED = "colorSeed"
        const val FIELD_UNREAD = "unread"
        const val FIELD_LAST_MESSAGE = "lastMessage"
        const val FIELD_TEXT = "text"
        const val FIELD_FROM_ME = "fromMe"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
