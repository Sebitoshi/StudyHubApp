package org.studyhub.project.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
actual fun rememberImagePicker(onPicked: (PickedImage) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { readPickedImage(context, uri) }
                .getOrNull()
                ?.let(onPicked)
        }
    }
    return remember(launcher) { { launcher.launch("image/*") } }
}

actual fun imagePickerSupported(): Boolean = true

/**
 * Decodifica, reduce y recomprime la imagen para que quepa en el límite de 6 MB
 * del backend y viaje como JPEG (uno de los formatos que acepta la visión).
 */
private fun readPickedImage(context: Context, uri: Uri): PickedImage? {
    val resolver = context.contentResolver
    val originalName = resolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: "imagen.jpg"

    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
    val largest = max(bitmap.width, bitmap.height)
    val scale = if (largest > MAX_DIMENSION) MAX_DIMENSION.toFloat() / largest else 1f
    val scaled = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1),
            true,
        )
    } else {
        bitmap
    }

    val output = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
    val bytes = output.toByteArray()

    if (scaled !== bitmap) scaled.recycle()
    bitmap.recycle()

    if (bytes.isEmpty()) return null
    val baseName = originalName.substringBeforeLast('.').ifBlank { "imagen" }
    return PickedImage(bytes = bytes, filename = "$baseName.jpg", mimeType = "image/jpeg")
}

private const val MAX_DIMENSION = 1600
private const val JPEG_QUALITY = 85
