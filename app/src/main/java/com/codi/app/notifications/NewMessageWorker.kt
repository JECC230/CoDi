package com.codi.app.notifications

import android.app.Application
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.codi.app.codiRepository
import com.codi.app.data.SessionManager
import java.util.concurrent.TimeUnit

/**
 * Los CoDis del catálogo son ficticios, así que no hay con quién chatear de
 * verdad. Este worker simula que el otro CoDi contesta unos segundos
 * después de que el usuario envía un mensaje: guarda la respuesta en el chat
 * en Firestore y dispara la notificación correspondiente.
 */
class NewMessageWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val contactName = inputData.getString(KEY_CONTACT_NAME) ?: return Result.failure()
        // Se escribe en la base de la cuenta con sesión activa; si ya se
        // cerró sesión, simplemente no llega respuesta.
        val repository = (applicationContext as Application).codiRepository
        if (SessionManager.currentUid.value == null) return Result.success()
        val reply = AUTO_REPLIES.random()

        repository.receiveMessage(contactName, reply)
        NotificationHelper.notifyNewMessage(applicationContext, contactName, reply)

        return Result.success()
    }

    companion object {
        private const val KEY_CONTACT_NAME = "contact_name"
        private const val REPLY_DELAY_SECONDS = 6L

        private val AUTO_REPLIES = listOf(
            "¡Suena bien! 👍",
            "Dale, nos vemos pronto",
            "Perfecto, gracias por avisar",
            "Claro, cualquier cosa me dices",
            "Va, quedamos así entonces",
            "Jaja sí, totalmente de acuerdo",
            "¿Te late si lo platicamos en persona?",
            "Me parece justo, así lo hacemos",
            "Ahorita no puedo, te escribo en un rato 🙌",
            "¡Qué bien! Yo también pienso lo mismo",
            "Oye, ¿y cuándo te quieres mudar?",
            "Sale, lo tomo en cuenta",
            "Me encanta la idea 😄"
        )

        /** Programa la respuesta simulada para la conversación con [contactName]. */
        fun schedule(context: Context, contactName: String) {
            val request = OneTimeWorkRequestBuilder<NewMessageWorker>()
                .setInitialDelay(REPLY_DELAY_SECONDS, TimeUnit.SECONDS)
                .setInputData(workDataOf(KEY_CONTACT_NAME to contactName))
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
