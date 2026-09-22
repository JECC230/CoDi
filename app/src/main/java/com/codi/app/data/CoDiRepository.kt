package com.codi.app.data

import android.content.Context
import android.net.Uri
import com.codi.app.data.catalog.Catalog
import com.codi.app.data.remote.ProfilePhotoEncoder
import com.codi.app.data.remote.UserDataStore
import com.codi.app.data.remote.UserProfileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

/**
 * Única fuente de datos de la app, 100% en la nube: perfil, decisiones del
 * swipe, matches y chats viven en Cloud Firestore bajo `users/{uid}` (ver
 * [UserProfileStore] y [UserDataStore]). Lo único local es el [Catalog] de
 * perfiles ficticios, que es contenido de la app.
 *
 * Todos los `Flow`s siguen a la cuenta con sesión activa
 * ([SessionManager.currentUid]): al iniciar sesión en otro dispositivo se ve
 * exactamente lo mismo, y al cambiar de cuenta cada pantalla pasa sola a los
 * datos de la nueva.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoDiRepository(
    context: Context,
    private val profileStore: UserProfileStore,
    private val dataStore: UserDataStore
) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val currentUid: String? get() = SessionManager.currentUid.value

    /** Consulta en tiempo real de la cuenta activa; emite [empty] si no hay sesión. */
    private fun <T> perUser(empty: T, query: (uid: String) -> Flow<T>): Flow<T> =
        SessionManager.currentUid.flatMapLatest { uid -> if (uid == null) flowOf(empty) else query(uid) }

    // --- Perfiles del catálogo (Home / detalle de CoDi / Match) ---

    /**
     * Catálogo completo con la decisión de la cuenta activa aplicada a cada
     * perfil. Un solo listener de Firestore compartido por Home, Perfil,
     * Match y el detalle.
     */
    private val catalogWithDecisions: Flow<List<CoDiProfile>> =
        perUser(emptyMap()) { uid -> dataStore.observeSwipes(uid) }
            .map { decisions ->
                Catalog.profiles.map { profile ->
                    profile.copy(status = decisions[profile.id] ?: SwipeStatus.PENDIENTE)
                }
            }
            .shareIn(scope, SharingStarted.WhileSubscribed(SHARE_TIMEOUT_MILLIS), replay = 1)

    /** Perfiles que siguen en el mazo de Home (sin decisión tomada). */
    val deck: Flow<List<CoDiProfile>> =
        catalogWithDecisions.map { list -> list.filter { it.status == SwipeStatus.PENDIENTE } }

    /** Perfiles con los que ya se hizo match. */
    val matches: Flow<List<CoDiProfile>> =
        catalogWithDecisions.map { list -> list.filter { it.status == SwipeStatus.INTERESA } }

    fun profileById(id: String): Flow<CoDiProfile?> =
        catalogWithDecisions.map { list -> list.firstOrNull { it.id == id } }

    /** Marca "Me Interesa": el perfil sale del mazo y queda como match. */
    fun markAsInteresa(profileId: String, onFailure: (Exception) -> Unit = {}) {
        val uid = currentUid ?: return
        dataStore.setSwipe(uid, profileId, SwipeStatus.INTERESA, onFailure)
    }

    /** Marca "Pasar": el perfil sale del mazo y no vuelve a aparecer. */
    fun markAsPasado(profileId: String, onFailure: (Exception) -> Unit = {}) {
        val uid = currentUid ?: return
        dataStore.setSwipe(uid, profileId, SwipeStatus.PASADO, onFailure)
    }

    /** Regresa todos los perfiles al mazo (útil cuando ya se vio todo). */
    suspend fun resetDeck() {
        val uid = currentUid ?: return
        dataStore.clearSwipes(uid)
    }

    // --- Mensajes y chat ---

    /**
     * Conversaciones para la pantalla Mensajes, la más reciente primero. Solo
     * con personas con las que hay match vigente: si se deshace el match (por
     * ejemplo con "Reiniciar mazo"), su chat deja de aparecer.
     */
    val conversations: Flow<List<Conversation>> =
        combine(
            perUser(emptyList()) { uid -> dataStore.observeConversations(uid) },
            matches
        ) { chats, matched ->
            val names = matched.mapTo(HashSet()) { it.name.lowercase() }
            chats.filter { it.contactName.lowercase() in names }
        }

    fun messagesOf(contactName: String): Flow<List<ChatMessage>> =
        perUser(emptyList()) { uid -> dataStore.observeMessages(uid, contactName) }

    /**
     * Garantiza que exista una conversación con [contactName]. Si es un match
     * nuevo que aún no tenía chat, la crea junto con un primer mensaje para
     * que la pantalla de Chat siempre tenga con qué trabajar.
     */
    suspend fun ensureConversation(contactName: String) {
        val uid = currentUid ?: return
        dataStore.ensureConversation(uid, contactName, colorSeedOf(contactName))
    }

    /** Guarda un mensaje enviado por el usuario y actualiza la conversación. */
    fun sendMessage(contactName: String, text: String) {
        val uid = currentUid ?: return
        dataStore.addMessage(uid, contactName, colorSeedOf(contactName), text, fromMe = true, markUnread = false)
    }

    fun markConversationAsRead(contactName: String) {
        val uid = currentUid ?: return
        dataStore.markAsRead(uid, contactName)
    }

    /**
     * Simula un mensaje que llega del otro CoDi (usado por el worker de
     * respuesta automática). A diferencia de [sendMessage], marca la
     * conversación como no leída, igual que haría un mensaje real entrante.
     */
    fun receiveMessage(contactName: String, text: String) {
        val uid = currentUid ?: return
        dataStore.addMessage(uid, contactName, colorSeedOf(contactName), text, fromMe = false, markUnread = true)
    }

    /** El avatar del chat usa el mismo color que el perfil del catálogo (si existe). */
    private fun colorSeedOf(contactName: String): Int =
        Catalog.profiles.firstOrNull { it.name.equals(contactName, ignoreCase = true) }?.colorSeed
            ?: contactName.hashCode().mod(6)

    // --- Perfil propio (Cloud Firestore, documento users/{uid}) ---

    /** Perfil de la cuenta con sesión activa; `null` si nadie inició sesión. */
    val user: Flow<UserProfile?> =
        perUser(null) { uid -> profileStore.observe(uid) }

    /** Guarda de un jalón todos los campos editables del perfil (estilo "Guardar cambios" de Instagram). */
    suspend fun updateUserProfile(
        name: String,
        lastName: String,
        birthDate: String,
        occupation: String,
        budget: Int,
        bio: String,
        gender: String,
        interests: List<String>
    ) {
        val uid = currentUid ?: return
        profileStore.update(
            uid,
            mapOf(
                UserProfileStore.FIELD_FIRST_NAME to name,
                UserProfileStore.FIELD_LAST_NAME to lastName,
                UserProfileStore.FIELD_BIRTH_DATE to UserProfileStore.birthDateField(birthDate),
                UserProfileStore.FIELD_OCCUPATION to occupation,
                UserProfileStore.FIELD_BUDGET to budget,
                UserProfileStore.FIELD_BIO to bio,
                UserProfileStore.FIELD_GENDER to gender,
                UserProfileStore.FIELD_INTERESTS to interests
            )
        )
    }

    /**
     * Sube la foto elegida en la galería (comprimida, ver
     * [ProfilePhotoEncoder]) al perfil en la nube, o la quita si [uri] es null.
     */
    suspend fun updateUserPhoto(uri: Uri?) {
        val uid = currentUid ?: return
        val encoded = uri?.let { ProfilePhotoEncoder.encode(appContext, it) }
        profileStore.update(uid, mapOf(UserProfileStore.FIELD_PHOTO_URI to encoded))
    }

    private companion object {
        const val SHARE_TIMEOUT_MILLIS = 5_000L
    }
}
