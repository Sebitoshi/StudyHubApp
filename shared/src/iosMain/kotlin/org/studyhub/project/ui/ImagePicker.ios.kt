package org.studyhub.project.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePicker(onPicked: (PickedImage) -> Unit): () -> Unit = {}

actual fun imagePickerSupported(): Boolean = false
