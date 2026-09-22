package com.codi.app.data.remote

import android.util.Log
import com.codi.app.data.UserProfile
import com.codi.app.data.epochMillisToIsoDate
import com.codi.app.data.isoDateToEpochMillis
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Perfil del propio usuario en Cloud Firestore: un documento por cuenta en
 * `users/{uid}`, con los campos
 * `firstName, lastName, birthDate (Long, millis UTC), email, occupation,
 * city, bio, gender, budget, photoUri, interests`. `photoUri` guarda la foto
 * ya comprimida como `data:image/jpeg;base64,...` (ver [ProfilePhotoEncoder]),
 * así viaja con el perfil a cualquier dispositivo.
 *
 * Firestore mantiene una caché local, así que las escrituras se ven al
 * instante en la UI (el listener de [observe] dispara con el cambio
 * pendiente) aunque el servidor todavía no confirme.
 */
class UserProfileStore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun document(uid: String) = firestore.collection(COLLECTION).document(uid)

    /**
     * Perfil de [uid] en tiempo real. Si el documento todavía no existe (o no
     * se pudo leer, por ejemplo sin reglas de Firestore configuradas), emite
     * un perfil mínimo armado con los datos de Firebase Auth para que Home y
     * Perfil sigan funcionando y el usuario pueda completarlo y guardarlo.
     */
    fun observe(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) Log.w(TAG, "No se pudo leer users/$uid", error)
            val profile = snapshot?.takeIf { it.exists() }?.toUserProfile(uid) ?: fallbackProfile(uid)
            trySend(profile)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Escribe todos los campos del perfil, usado al registrarse. Con `merge`
     * para no pisar otros campos del mismo documento que se hayan escrito en
     * paralelo.
     */
    suspend fun create(profile: UserProfile) {
        document(profile.uid).set(profile.toFirestore(), SetOptions.merge()).awaitServer()
    }

    /**
     * Crea el documento solo si no existe todavía: primer inicio de sesión
     * con Google, donde el nombre sale de la cuenta de Google.
     */
    suspend fun createIfMissing(profile: UserProfile) {
        val existing = document(profile.uid).get().await()
        if (!existing.exists()) create(profile)
    }

    /**
     * Actualiza solo los campos indicados (con `merge`, así también crea el
     * documento si por algún motivo no existía).
     */
    suspend fun update(uid: String, fields: Map<String, Any?>) {
        val withEmail = fields + (FIELD_EMAIL to auth.currentUser?.email.orEmpty())
        document(uid).set(withEmail, SetOptions.merge()).awaitServer()
    }

    /**
     * Espera a que el servidor confirme la escritura, pero no para siempre:
     * sin conexión Firestore ya la dejó guardada en su caché local y la
     * sincroniza sola en cuanto vuelva la red. Los errores reales (reglas,
     * base inexistente) sí se propagan.
     */
    private suspend fun Task<Void>.awaitServer() {
        withTimeoutOrNull(SERVER_ACK_TIMEOUT_MILLIS) { await() }
    }

    private fun fallbackProfile(uid: String): UserProfile? {
        val user = auth.currentUser?.takeIf { it.uid == uid } ?: return null
        val (firstName, lastName) = splitDisplayName(user.displayName)
        return UserProfile(
            uid = uid,
            email = user.email.orEmpty(),
            name = firstName,
            lastName = lastName,
            birthDate = "",
            city = DEFAULT_CITY,
            bio = "",
            interests = emptyList(),
            colorSeed = colorSeedFor(uid),
            occupation = "",
            budget = 0
        )
    }

    companion object {
        private const val TAG = "UserProfileStore"
        private const val COLLECTION = "users"
        private const val SERVER_ACK_TIMEOUT_MILLIS = 8_000L

        const val DEFAULT_CITY = "Ciudad de México"

        const val FIELD_FIRST_NAME = "firstName"
        const val FIELD_LAST_NAME = "lastName"
        const val FIELD_BIRTH_DATE = "birthDate"
        const val FIELD_EMAIL = "email"
        const val FIELD_OCCUPATION = "occupation"
        const val FIELD_CITY = "city"
        const val FIELD_BIO = "bio"
        const val FIELD_GENDER = "gender"
        const val FIELD_BUDGET = "budget"
        const val FIELD_PHOTO_URI = "photoUri"
        const val FIELD_INTERESTS = "interests"

        /** Color del avatar por defecto: estable por cuenta, no se guarda en la nube. */
        fun colorSeedFor(uid: String): Int = uid.hashCode().mod(6)

        /** "Ana María López" -> ("Ana María", "López"); lo usa el primer login con Google. */
        fun splitDisplayName(displayName: String?): Pair<String, String> {
            val parts = displayName.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            return when (parts.size) {
                0 -> "" to ""
                1 -> parts[0] to ""
                else -> parts.dropLast(1).joinToString(" ") to parts.last()
            }
        }

        /** Convierte la fecha ISO del modelo al Long que se guarda en Firestore (o null si está vacía). */
        fun birthDateField(isoDate: String): Long? = isoDateToEpochMillis(isoDate)
    }
}

private fun UserProfile.toFirestore(): Map<String, Any?> = mapOf(
    UserProfileStore.FIELD_FIRST_NAME to name,
    UserProfileStore.FIELD_LAST_NAME to lastName,
    UserProfileStore.FIELD_BIRTH_DATE to UserProfileStore.birthDateField(birthDate),
    UserProfileStore.FIELD_EMAIL to email,
    UserProfileStore.FIELD_OCCUPATION to occupation,
    UserProfileStore.FIELD_CITY to city,
    UserProfileStore.FIELD_BIO to bio,
    UserProfileStore.FIELD_GENDER to gender,
    UserProfileStore.FIELD_BUDGET to budget,
    UserProfileStore.FIELD_PHOTO_URI to photoUri,
    UserProfileStore.FIELD_INTERESTS to interests
)

private fun DocumentSnapshot.toUserProfile(uid: String): UserProfile = UserProfile(
    uid = uid,
    email = getString(UserProfileStore.FIELD_EMAIL).orEmpty(),
    name = getString(UserProfileStore.FIELD_FIRST_NAME).orEmpty(),
    lastName = getString(UserProfileStore.FIELD_LAST_NAME).orEmpty(),
    birthDate = getLong(UserProfileStore.FIELD_BIRTH_DATE)?.let(::epochMillisToIsoDate).orEmpty(),
    city = getString(UserProfileStore.FIELD_CITY) ?: UserProfileStore.DEFAULT_CITY,
    bio = getString(UserProfileStore.FIELD_BIO).orEmpty(),
    interests = (get(UserProfileStore.FIELD_INTERESTS) as? List<*>)?.filterIsInstance<String>().orEmpty(),
    colorSeed = UserProfileStore.colorSeedFor(uid),
    occupation = getString(UserProfileStore.FIELD_OCCUPATION).orEmpty(),
    budget = getLong(UserProfileStore.FIELD_BUDGET)?.toInt() ?: 0,
    gender = getString(UserProfileStore.FIELD_GENDER).orEmpty(),
    photoUri = getString(UserProfileStore.FIELD_PHOTO_URI)
)
