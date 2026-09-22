package com.codi.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.codi.app.data.ThemeMode
import com.codi.app.data.ThemePreferences
import com.codi.app.ui.navigation.CoDiApp
import com.codi.app.ui.theme.CoDiTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* si se niega, la app sigue funcionando sin notificaciones */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            val themeMode by ThemePreferences.themeMode.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()
            val isDarkMode = when (themeMode) {
                ThemeMode.SYSTEM -> systemInDarkTheme
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            CoDiTheme(darkTheme = isDarkMode) {
                CoDiApp()
            }
        }
    }

    /**
     * Desde Android 13 (API 33) mostrar notificaciones requiere permiso en
     * tiempo de ejecución. Se pide una sola vez al abrir la app; si el
     * usuario lo niega, los matches y mensajes simplemente no notifican
     * (el resto de la app sigue funcionando igual, todo vive en SQLite).
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
