package com.codi.app.data

/**
 * Modelos de dominio de CoDi. Son los objetos que consume la UI; la
 * persistencia vive en Cloud Firestore (ver paquete `data.remote`); el
 * catálogo de perfiles ficticios viaja dentro de la app (`data.catalog`).
 */

/** Decisión que el usuario ya tomó sobre un candidato del swipe de Home. */
enum class SwipeStatus {
    /** Todavía no se ha decidido: sigue en el mazo. */
    PENDIENTE,

    /** Se presionó "Me Interesa": hubo match. */
    INTERESA,

    /** Se presionó "Pasar": no vuelve a aparecer en el mazo. */
    PASADO;

    companion object {
        fun from(raw: String): SwipeStatus =
            entries.firstOrNull { it.name == raw } ?: PENDIENTE
    }
}

/** Candidato/a a CoDi que aparece en el swipe de Home. */
data class CoDiProfile(
    val id: String,
    val name: String,
    val age: Int,
    val zone: String,
    val colorSeed: Int,
    val interests: List<String>,
    val bio: String,
    val compatibility: Int,
    val occupation: String,
    val budget: Int,
    val schedule: String,
    val cleanliness: Int,
    val smoker: Boolean,
    val petFriendly: Boolean,
    val moveInDate: String,
    val status: SwipeStatus = SwipeStatus.PENDIENTE
)

/** Un mensaje individual dentro de una conversación de Chat. */
data class ChatMessage(
    /** Id del documento en Firestore. */
    val id: String,
    val text: String,
    val fromMe: Boolean,
    val timestamp: String
)

/** Una conversación completa que aparece en la lista de Mensajes. */
data class Conversation(
    val contactName: String,
    /** "Ahora", "5 min", "Ayer", "3 días"... calculado a partir del último mensaje. */
    val lastActivity: String,
    val unread: Boolean,
    val colorSeed: Int,
    /** Texto del último mensaje (se guarda en el documento del chat para no leer todos los mensajes). */
    val preview: String
)

/**
 * Perfil del propio usuario (el que se ve y edita en la pestaña Perfil).
 * Vive en Cloud Firestore, en el documento `users/{uid}` (ver
 * [com.codi.app.data.remote.UserProfileStore]).
 */
data class UserProfile(
    val uid: String,
    val email: String,
    val name: String,
    val lastName: String = "",
    /** Fecha de nacimiento ISO-8601 (`yyyy-MM-dd`); ver [age]. */
    val birthDate: String,
    val city: String,
    val bio: String,
    val interests: List<String>,
    val colorSeed: Int,
    val occupation: String,
    val budget: Int,
    val gender: String = "",
    val photoUri: String? = null
) {
    val fullName: String get() = listOf(name, lastName).filter { it.isNotBlank() }.joinToString(" ")
    val age: Int get() = calculateAge(birthDate)
}
