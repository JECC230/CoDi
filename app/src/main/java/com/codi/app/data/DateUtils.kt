package com.codi.app.data

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Fecha de nacimiento guardada como texto ISO-8601 (`yyyy-MM-dd`), independiente de la zona horaria. */
private val ISO_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

/** Calcula la edad actual a partir de una fecha de nacimiento ISO-8601. Devuelve 0 si no se puede leer. */
fun calculateAge(birthDateIso: String): Int {
    val birthDate = runCatching { LocalDate.parse(birthDateIso, ISO_DATE_FORMATTER) }.getOrNull() ?: return 0
    return Period.between(birthDate, LocalDate.now()).years.coerceAtLeast(0)
}

/** Formatea una fecha ISO-8601 para mostrarla en la UI (ej. "20 de mayo de 1998"). */
fun formatBirthDate(birthDateIso: String): String {
    val date = runCatching { LocalDate.parse(birthDateIso, ISO_DATE_FORMATTER) }.getOrNull() ?: return birthDateIso
    return date.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "MX")))
}

/** Convierte millis de epoch (los que entrega el `DatePicker` de Compose, en UTC) a una fecha ISO-8601. */
fun epochMillisToIsoDate(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate().format(ISO_DATE_FORMATTER)

/** Convierte una fecha ISO-8601 a millis de epoch (medianoche UTC), como se guarda `birthDate` en Firestore. */
fun isoDateToEpochMillis(birthDateIso: String): Long? =
    runCatching {
        LocalDate.parse(birthDateIso, ISO_DATE_FORMATTER).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    }.getOrNull()

/** Hora corta de un mensaje ("10:02"). */
fun formatMessageTime(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault())
        .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

/** Tiempo relativo para la lista de Mensajes: "Ahora", "5 min", "3 h", "Ayer", "4 días", "2 sem". */
fun formatRelativeTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - epochMillis).coerceAtLeast(0)) / 60_000
    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "$minutes min"
        minutes < 24 * 60 -> "${minutes / 60} h"
        minutes < 2 * 24 * 60 -> "Ayer"
        minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)} días"
        else -> "${minutes / (7 * 24 * 60)} sem"
    }
}
