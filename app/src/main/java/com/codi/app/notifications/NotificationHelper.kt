package com.codi.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.codi.app.R

/**
 * CoDi no tiene backend real, así que no llegan push notifications de un
 * servidor. En su lugar, la app dispara notificaciones locales para
 * confirmar eventos que ya ocurrieron en SQLite: un nuevo match (inmediato)
 * o un mensaje recibido (simulado con [com.codi.app.notifications.NewMessageWorker]
 * unos segundos después de que el usuario escribe).
 */
object NotificationHelper {

    private const val CHANNEL_MATCHES = "matches"
    private const val CHANNEL_MESSAGES = "messages"

    private const val NOTIFICATION_ID_MATCH = 1001
    private const val NOTIFICATION_ID_MESSAGE_BASE = 2000

    /** Crea los canales de notificación. Se llama una sola vez al iniciar la app. */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MATCHES,
                "Nuevos matches",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Avisa cuando alguien también dio \"Me Interesa\"" }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Mensajes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisa cuando llega un mensaje nuevo en un chat" }
        )
    }

    /** En Android 13+ hace falta permiso en tiempo de ejecución para notificar. */
    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notifyNewMatch(context: Context, contactName: String) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_MATCHES)
            .setSmallIcon(R.drawable.ic_stat_codi)
            .setContentTitle("¡Es un match!")
            .setContentText("A $contactName también le gustaría ser tu CoDi")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MATCH, notification)
    }

    fun notifyNewMessage(context: Context, contactName: String, text: String) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_stat_codi)
            .setContentTitle(contactName)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // El id se deriva del contacto para que un chat activo actualice/reemplace
        // su propia notificación en vez de acumular una por cada mensaje.
        val id = NOTIFICATION_ID_MESSAGE_BASE + contactName.hashCode()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
