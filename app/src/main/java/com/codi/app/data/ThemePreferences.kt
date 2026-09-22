package com.codi.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

/** "Sistema" sigue el tema del dispositivo en automático; los otros dos lo fijan sin importar el sistema. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Preferencia de apariencia, elevada a la raíz de la app (ver `MainActivity`)
 * para que un cambio en Ajustes se refleje de inmediato en todas las
 * pantallas. Persiste en `SharedPreferences` para recordar la elección
 * entre sesiones.
 */
object ThemePreferences {
    private const val PREFS_NAME = "codi_settings"
    private const val KEY_THEME_MODE = "theme_mode"

    private lateinit var prefs: android.content.SharedPreferences

    /** Por defecto sigue el tema del sistema, como cualquier app bien portada. */
    val themeMode = MutableStateFlow(ThemeMode.SYSTEM)

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        themeMode.value = runCatching { ThemeMode.valueOf(stored ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        themeMode.value = mode
    }
}
