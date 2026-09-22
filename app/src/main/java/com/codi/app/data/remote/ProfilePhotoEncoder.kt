package com.codi.app.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Convierte la foto elegida de la galería en una miniatura JPEG lista para
 * guardarse en el perfil de Firestore (`data:image/jpeg;base64,...`).
 *
 * Se reduce a [MAX_SIDE_PX] px por lado y se comprime: queda en unas
 * decenas de KB, muy por debajo del límite de 1 MB por documento, y se ve
 * nítida en los avatares de la app. (Con el plan Blaze de Firebase esto se
 * podría subir a Cloud Storage y guardar solo la URL.)
 */
object ProfilePhotoEncoder {

    private const val MAX_SIDE_PX = 512
    private const val JPEG_QUALITY = 82
    const val DATA_URI_PREFIX = "data:image/jpeg;base64,"

    suspend fun encode(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(context, uri)
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
        DATA_URI_PREFIX + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** Decodifica solo los bytes de un `data:` URI (lo usa el avatar para mostrar la foto). */
    fun decodeDataUri(dataUri: String): Bitmap? {
        val base64 = dataUri.substringAfter("base64,", missingDelimiterValue = "")
        if (base64.isEmpty()) return null
        val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull() ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun decodeScaled(context: Context, uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (w, h) = info.size.width to info.size.height
                val scale = MAX_SIDE_PX.toFloat() / maxOf(w, h)
                if (scale < 1f) decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
                // Software para poder comprimirlo a JPEG después.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        // API 26-27: dos pasadas, primero solo el tamaño y luego con inSampleSize.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("No se pudo leer la imagen")
        val scale = MAX_SIDE_PX.toFloat() / maxOf(decoded.width, decoded.height)
        return if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
        } else {
            decoded
        }
    }
}
