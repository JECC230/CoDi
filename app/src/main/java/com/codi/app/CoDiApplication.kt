package com.codi.app

import android.app.Application
import com.codi.app.data.AuthRepository
import com.codi.app.data.CoDiRepository
import com.codi.app.data.SessionManager
import com.codi.app.data.ThemePreferences
import com.codi.app.data.remote.UserDataStore
import com.codi.app.data.remote.UserProfileStore
import com.codi.app.notifications.NotificationHelper

/**
 * Punto único donde se construyen los repositorios. Los ViewModels los
 * alcanzan a través del `Application` (ver [codiRepository] y
 * [codiAuthRepository]).
 */
class CoDiApplication : Application() {

    private val profileStore by lazy { UserProfileStore() }

    /** Se crea la primera vez que se usa, no al arrancar la app. */
    val repository: CoDiRepository by lazy {
        CoDiRepository(this, profileStore, UserDataStore())
    }

    /** Autenticación con Firebase Auth (correo/contraseña y Google). */
    val authRepository: AuthRepository by lazy {
        AuthRepository(this, profileStore)
    }

    override fun onCreate() {
        super.onCreate()
        SessionManager.init()
        ThemePreferences.init(this)
        NotificationHelper.createChannels(this)
        deleteLegacyDatabases()
    }

    /**
     * Versiones anteriores guardaban cuentas, mazo y chats en SQLite
     * (`codi.db`, luego `codi_<uid>.db`). Todo eso ahora vive en Firestore,
     * así que esas bases locales solo ocupan espacio.
     */
    private fun deleteLegacyDatabases() {
        databaseList()
            .filter { it.startsWith("codi") && it.endsWith(".db") }
            .forEach { deleteDatabase(it) }
    }
}

/** Atajo para obtener el repositorio de datos desde un `AndroidViewModel`. */
val Application.codiRepository: CoDiRepository
    get() = (this as CoDiApplication).repository

/** Atajo para obtener el repositorio de autenticación desde un `AndroidViewModel`. */
val Application.codiAuthRepository: AuthRepository
    get() = (this as CoDiApplication).authRepository
