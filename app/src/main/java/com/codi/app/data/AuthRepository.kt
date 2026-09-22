package com.codi.app.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.codi.app.data.remote.UserProfileStore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Resultado de un intento de login/registro. */
sealed class AuthResult {
    data class Success(val email: String) : AuthResult()
    data class Error(val message: String) : AuthResult()

    /** El usuario cerró el selector de cuentas de Google: no es un error que mostrar. */
    data object Cancelled : AuthResult()
}

/**
 * Autenticación con Firebase Authentication: correo/contraseña y Google
 * (flujo nativo con Credential Manager). El perfil de cada cuenta se guarda
 * en Cloud Firestore mediante [UserProfileStore].
 */
class AuthRepository(
    context: Context,
    private val profileStore: UserProfileStore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Crea la cuenta en Firebase Auth y su documento `users/{uid}`. Si el
     * perfil no se puede guardar en Firestore se borra la cuenta recién
     * creada, para que reintentar el registro no choque con "ese correo ya
     * existe" y no quede una cuenta sin perfil.
     */
    suspend fun register(
        email: String,
        password: String,
        name: String,
        lastName: String,
        birthDate: String,
        occupation: String,
        bio: String
    ): AuthResult {
        val normalized = email.trim().lowercase()
        val user = try {
            auth.createUserWithEmailAndPassword(normalized, password).await().user
                ?: return AuthResult.Error(GENERIC_ERROR)
        } catch (e: Exception) {
            return AuthResult.Error(e.toMessage())
        }

        return try {
            profileStore.create(
                UserProfile(
                    uid = user.uid,
                    email = normalized,
                    name = name,
                    lastName = lastName,
                    birthDate = birthDate,
                    city = UserProfileStore.DEFAULT_CITY,
                    bio = bio,
                    interests = emptyList(),
                    colorSeed = UserProfileStore.colorSeedFor(user.uid),
                    occupation = occupation,
                    // El presupuesto no se pide al registrarse; se ajusta después desde Perfil.
                    budget = 0
                )
            )
            AuthResult.Success(normalized)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo crear el perfil en Firestore", e)
            runCatching { user.delete().await() }
            auth.signOut()
            AuthResult.Error(e.toMessage())
        }
    }

    suspend fun login(email: String, password: String): AuthResult =
        try {
            val normalized = email.trim().lowercase()
            auth.signInWithEmailAndPassword(normalized, password).await()
            AuthResult.Success(normalized)
        } catch (e: Exception) {
            AuthResult.Error(e.toMessage())
        }

    /**
     * "Continuar con Google": muestra el selector de cuentas de Credential
     * Manager, cambia el ID token de Google por una sesión de Firebase y, si
     * es la primera vez, crea el perfil en Firestore con el nombre de la
     * cuenta de Google.
     *
     * [activityContext] debe ser la Activity: Credential Manager la necesita
     * para mostrar su hoja inferior.
     */
    suspend fun loginWithGoogle(activityContext: Context): AuthResult {
        val serverClientId = webClientId()
            ?: return AuthResult.Error(
                "Google no está configurado todavía en Firebase: habilita el proveedor Google, " +
                    "registra la huella SHA-1 de la app y vuelve a descargar google-services.json."
            )

        val idToken = when (val result = requestGoogleIdToken(activityContext, serverClientId)) {
            is GoogleTokenResult.Token -> result.idToken
            is GoogleTokenResult.Failure -> return result.authResult
        }

        val user = try {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user
                ?: return AuthResult.Error(GENERIC_ERROR)
        } catch (e: Exception) {
            return AuthResult.Error(e.toMessage())
        }

        // Si esto falla (sin red, reglas de Firestore), la sesión igual es
        // válida: Perfil arma un perfil mínimo con los datos de Google y se
        // guarda completo la primera vez que el usuario lo edite.
        try {
            val (firstName, lastName) = UserProfileStore.splitDisplayName(user.displayName)
            profileStore.createIfMissing(
                UserProfile(
                    uid = user.uid,
                    email = user.email.orEmpty(),
                    name = firstName,
                    lastName = lastName,
                    birthDate = "",
                    city = UserProfileStore.DEFAULT_CITY,
                    bio = "",
                    interests = emptyList(),
                    colorSeed = UserProfileStore.colorSeedFor(user.uid),
                    occupation = "",
                    budget = 0
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo crear el perfil de Google en Firestore", e)
        }
        return AuthResult.Success(user.email.orEmpty())
    }

    private sealed class GoogleTokenResult {
        data class Token(val idToken: String) : GoogleTokenResult()
        data class Failure(val authResult: AuthResult) : GoogleTokenResult()
    }

    /**
     * Pide un ID token de Google en dos intentos:
     *
     * 1. El selector inferior de cuentas del teléfono ([GetGoogleIdOption]).
     * 2. Si ese no ofrece ninguna cuenta, el diálogo clásico de "Iniciar
     *    sesión con Google" ([GetSignInWithGoogleOption]), que es el
     *    recomendado para un botón explícito: permite agregar una cuenta y,
     *    si la app no está bien registrada en Google, muestra el error real.
     *
     * Ojo: Credential Manager devuelve [NoCredentialException] tanto cuando no
     * hay cuentas como cuando la huella SHA-1 del APK no está registrada en
     * Firebase, así que ese error no se puede leer como "no hay cuentas".
     */
    private suspend fun requestGoogleIdToken(activityContext: Context, serverClientId: String): GoogleTokenResult {
        val credentialManager = CredentialManager.create(activityContext)
        val bottomSheet = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()
        val signInDialog = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
            .build()

        for (request in listOf(bottomSheet, signInDialog)) {
            try {
                val credential = credentialManager.getCredential(activityContext, request).credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    return GoogleTokenResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
                }
                return GoogleTokenResult.Failure(AuthResult.Error("No se recibió una cuenta de Google válida"))
            } catch (e: GetCredentialCancellationException) {
                return GoogleTokenResult.Failure(AuthResult.Cancelled)
            } catch (e: NoCredentialException) {
                Log.w(TAG, "Sin credenciales con ${request.credentialOptions.first()::class.simpleName}", e)
                if (request === signInDialog) {
                    return GoogleTokenResult.Failure(AuthResult.Error(googleErrorMessage(e)))
                }
                // Siguiente intento: el diálogo clásico.
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Credential Manager falló", e)
                return GoogleTokenResult.Failure(AuthResult.Error(googleErrorMessage(e)))
            }
        }
        return GoogleTokenResult.Failure(AuthResult.Error("No se pudo iniciar sesión con Google"))
    }

    /** Traduce los errores de Credential Manager a algo accionable, con el detalle técnico al final. */
    private fun googleErrorMessage(e: GetCredentialException): String {
        val detail = e.errorMessage?.toString().orEmpty()
        val reason = when {
            "28444" in detail || "developer console" in detail.lowercase() || "10:" in detail ->
                "Google no reconoce esta versión de la app. Revisa que la huella SHA-1 del APK esté registrada en Firebase."
            "7:" in detail || "network" in detail.lowercase() ->
                "Sin conexión a internet. Revisa tu red e intenta de nuevo."
            e is NoCredentialException ->
                "Google no ofreció ninguna cuenta. Si tu teléfono sí tiene una cuenta de Google, la huella SHA-1 de esta versión de la app no está registrada en Firebase."
            else -> "No se pudo iniciar sesión con Google."
        }
        return if (detail.isBlank()) "$reason (${e.type.substringAfterLast('.')})" else "$reason\n[$detail]"
    }

    /**
     * Cambia la contraseña en Firebase Auth. Las cuentas que entraron con
     * Google no tienen contraseña propia en CoDi.
     */
    suspend fun changePassword(newPassword: String): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Error("No hay una sesión activa")
        if (user.providerData.none { it.providerId == "password" }) {
            return AuthResult.Error("Tu cuenta usa Google: la contraseña se administra desde tu cuenta de Google")
        }
        return try {
            user.updatePassword(newPassword).await()
            AuthResult.Success(user.email.orEmpty())
        } catch (e: Exception) {
            AuthResult.Error(e.toMessage())
        }
    }

    /**
     * Cierra la sesión de Firebase y limpia la cuenta de Google recordada por
     * Credential Manager, para que el siguiente "Continuar con Google" vuelva
     * a mostrar el selector de cuentas.
     */
    fun logout() {
        auth.signOut()
        scope.launch {
            runCatching { CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest()) }
        }
    }

    /**
     * `default_web_client_id` lo genera el plugin de Google Services a partir
     * del cliente OAuth "web" de google-services.json. Se busca por nombre
     * (en vez de `R.string.default_web_client_id`) porque ese recurso solo
     * existe cuando Google Sign-In ya está habilitado en la consola, y así la
     * app compila también antes de eso.
     */
    private fun webClientId(): String? {
        val resId = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        return if (resId == 0) null else appContext.getString(resId).takeIf { it.isNotBlank() }
    }

    private companion object {
        const val TAG = "AuthRepository"
        const val GENERIC_ERROR = "Algo salió mal, intenta de nuevo"
    }
}

/** Traduce las excepciones de Firebase a mensajes en español para la UI. */
private fun Exception.toMessage(): String = when (this) {
    is FirebaseAuthRecentLoginRequiredException ->
        "Por seguridad, cierra sesión y vuelve a entrar antes de cambiar tu contraseña"
    is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo"
    is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil, usa al menos 6 caracteres"
    is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo o fue deshabilitada"
    is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
    is FirebaseNetworkException -> "Sin conexión a internet. Revisa tu red e intenta de nuevo"
    is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera un momento e intenta de nuevo"
    is FirebaseAuthException -> when (errorCode) {
        "ERROR_OPERATION_NOT_ALLOWED" ->
            "Este método de inicio de sesión no está habilitado en Firebase Console"
        else -> "Error de autenticación ($errorCode)"
    }
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firestore rechazó el guardado del perfil: revisa las reglas de seguridad"
        FirebaseFirestoreException.Code.NOT_FOUND ->
            "La base de datos de Firestore no existe todavía: créala en Firebase Console"
        FirebaseFirestoreException.Code.UNAVAILABLE -> "Sin conexión a internet. Revisa tu red e intenta de nuevo"
        else -> "No se pudo guardar tu perfil (${code.name})"
    }
    else -> when {
        // Firebase lo devuelve como error interno cuando Authentication no se
        // ha activado en la consola para este proyecto.
        message?.contains("CONFIGURATION_NOT_FOUND") == true ->
            "Firebase Authentication no está activado en este proyecto: actívalo en Firebase Console"
        else -> message ?: "Algo salió mal, intenta de nuevo"
    }
}
