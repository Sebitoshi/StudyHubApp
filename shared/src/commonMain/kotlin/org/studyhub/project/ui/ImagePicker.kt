package org.studyhub.project.ui

import androidx.compose.runtime.Composable

/** Imagen elegida por el usuario, ya normalizada para enviarla al backend. */
data class PickedImage(
    val bytes: ByteArray,
    val filename: String,
    val mimeType: String,
)

/**
 * Abre el selector de imágenes del sistema y entrega la imagen elegida.
 * Devuelve una función que se llama para lanzar el selector.
 */
@Composable
expect fun rememberImagePicker(onPicked: (PickedImage) -> Unit): () -> Unit

/** Algunas plataformas aún no tienen selector implementado. */
expect fun imagePickerSupported(): Boolean
