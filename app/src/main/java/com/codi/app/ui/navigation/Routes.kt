package com.codi.app.ui.navigation

import android.net.Uri

/** Rutas de navegación de la app. */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val MENSAJES = "mensajes"
    const val PERFIL = "perfil"
    const val SETTINGS = "settings"

    const val MATCH_PATTERN = "match/{profileId}"
    const val CHAT_PATTERN = "chat/{contactName}"
    const val CODI_DETAIL_PATTERN = "codiDetail/{profileId}"

    /** Rutas que muestran la bottom navigation bar. CoDi v1 se enfoca solo en roomies: Home | Mensajes | Perfil. */
    val BOTTOM_BAR_ROUTES = setOf(HOME, MENSAJES, PERFIL)

    fun match(profileId: String): String =
        "match/${Uri.encode(profileId)}"

    fun chat(contactName: String): String =
        "chat/${Uri.encode(contactName)}"

    /** Detalle de un candidato a CoDi. */
    fun codiDetail(profileId: String): String =
        "codiDetail/${Uri.encode(profileId)}"
}
