package com.codi.app.ui.components

import com.codi.app.data.remote.ProfilePhotoEncoder
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codi.app.data.epochMillisToIsoDate
import com.codi.app.data.formatBirthDate
import com.codi.app.ui.theme.AccentBlue
import com.codi.app.ui.theme.ChipBackground
import com.codi.app.ui.theme.PillShape
import com.codi.app.ui.theme.TextPrimary
import com.codi.app.ui.theme.TextSecondary
import com.codi.app.ui.theme.placeholderColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect

/**
 * Placeholder de foto de perfil/imagen: círculo (o superficie con esquinas
 * grandes si [circular] es false) relleno con un color de marca derivado de
 * [seed], mostrando un ícono como marcador visual mientras no hay backend
 * real con fotos.
 */
@Composable
fun PlaceholderAvatar(
    seed: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    circular: Boolean = true
) {
    val shape = if (circular) CircleShape else MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .size(size)
            .background(placeholderColor(seed), shape),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(size * 0.4f)
            )
        }
    }
}

/** Chip tipo "pill" para intereses, amenidades o filtros no seleccionados. */
@Composable
fun InterestChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = ChipBackground,
    contentColor: Color = TextPrimary
) {
    Surface(
        modifier = modifier,
        shape = PillShape,
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/** Chip de filtro/selección con estado seleccionado/no seleccionado (ej. intereses del perfil). */
@Composable
fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = PillShape,
        color = if (selected) AccentBlue else ChipBackground,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        onClick = onClick
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

/** Badge pequeño tipo pill, usado por ejemplo para "NUEVA CONEXIÓN". */
@Composable
fun SectionBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = PillShape,
        color = Color.White.copy(alpha = 0.16f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

/** Botón principal (relleno, color de acento) usado en toda la app. */
@Composable
fun CoDiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentBlue,
            contentColor = TextPrimary
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** Botón secundario (outline) usado por ejemplo en "Pasar". */
@Composable
fun CoDiOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/**
 * Decodifica la imagen de [uriString] (`data:` de Firestore o `content://`
 * local) fuera del hilo principal; se re-decodifica si cambia la URI.
 */
@Composable
private fun rememberUriBitmap(uriString: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uriString) {
        bitmap = if (uriString == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    if (uriString.startsWith("data:")) {
                        // Foto sincronizada desde el perfil en Firestore.
                        ProfilePhotoEncoder.decodeDataUri(uriString)?.asImageBitmap()
                    } else {
                        context.contentResolver.openInputStream(Uri.parse(uriString))?.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }
                }.getOrNull()
            }
        }
    }
    return bitmap
}

/**
 * Foto de perfil real (elegida de la galería) o, si no hay ninguna,
 * [PlaceholderAvatar]. Se usa en todos los lugares donde se muestra el
 * avatar del propio usuario (Home, Perfil, Match) para que la foto se vea
 * consistente en toda la app en cuanto se guarda.
 */
@Composable
fun UserAvatar(
    photoUri: String?,
    colorSeed: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val bitmap = rememberUriBitmap(photoUri)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        PlaceholderAvatar(seed = colorSeed, size = size, icon = icon, modifier = modifier)
    }
}

/**
 * Campo de solo lectura que abre un [DatePicker] al tocarlo. Se usa para la
 * fecha de nacimiento (Registro y edición de perfil); la edad se calcula a
 * partir de la fecha elegida, nunca se pide directamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthDateField(
    isoDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Fecha de nacimiento",
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    colors: TextFieldColors? = null
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = if (isoDate.isBlank()) "" else formatBirthDate(isoDate),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TextSecondary) },
            isError = isError,
            supportingText = supportingText,
            modifier = Modifier.fillMaxWidth(),
            colors = colors ?: OutlinedTextFieldDefaults.colors()
        )
        // Intercepta el toque completo del campo: readOnly ya bloquea el
        // teclado, pero sigue permitiendo foco/cursor; esta capa transparente
        // es la que realmente abre el selector de fecha.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    showPicker = true
                }
        )
    }

    if (showPicker) {
        val initialMillis = isoDate.takeIf { it.isNotBlank() }?.let {
            runCatching {
                java.time.LocalDate.parse(it).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrNull()
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateSelected(epochMillisToIsoDate(it)) }
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
