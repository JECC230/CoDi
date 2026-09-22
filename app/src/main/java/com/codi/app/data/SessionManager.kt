package com.codi.app.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sesión activa de CoDi: qué cuenta de Firebase Auth está usando la app
 * ahora mismo. Firebase ya persiste la sesión entre relanzamientos, así que
 * aquí solo se expone su `uid` como `StateFlow` para que los repositorios y
 * la navegación reaccionen cuando alguien inicia o cierra sesión.
 */
object SessionManager {

    private val _currentUid = MutableStateFlow<String?>(null)

    /** `uid` de Firebase de la cuenta con sesión activa; `null` = nadie ha iniciado sesión. */
    val currentUid: StateFlow<String?> = _currentUid.asStateFlow()

    fun init() {
        val auth = FirebaseAuth.getInstance()
        _currentUid.value = auth.currentUser?.uid
        auth.addAuthStateListener { _currentUid.value = it.currentUser?.uid }
    }
}
