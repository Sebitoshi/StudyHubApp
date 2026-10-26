package org.studyhub.project

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * StudyHub — "Sage & Paper".
 *
 * Identidad visual clara, cálida y calmada: fondo papel verdoso, tarjetas blancas,
 * acentos salvia y bloques pastel (crema, lavanda, melocotón y cielo).
 * Los nombres de los tokens se mantienen para no romper las pantallas existentes.
 */
object StudyHubColors {
    // Superficies
    val Bg = Color(0xFFF4F7EF)
    val BgAlt = Color(0xFFEFF4E8)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceLight = Color(0xFFEDF2E7)
    val SurfaceHover = Color(0xFFE2EAD9)

    val Glass = Color(0xFFFFFFFF)
    val GlassBorder = Color(0xFFE7ECDF)
    val Border = Color(0xFFE4EAD9)

    // Salvia (color principal)
    val Primary = Color(0xFF5E7C50)
    val PrimaryDark = Color(0xFF4C6741)
    val PrimaryLight = Color(0xFF6F8F5F)

    // Apoyo
    val Secondary = Color(0xFF7D9C6B)
    val SecondaryLight = Color(0xFF5E7C50)
    val Accent = Color(0xFFC98A4A)
    val AccentLight = Color(0xFFB9763A)
    val AccentDark = Color(0xFF9C5F2A)
    val Danger = Color(0xFFC06055)
    val DangerLight = Color(0xFFB4534A)
    val Info = Color(0xFF6B82B8)
    val InfoLight = Color(0xFF5B72A8)

    // Texto
    val TextPrimary = Color(0xFF2F3A2B)
    val TextSecondary = Color(0xFF6F7A68)
    val TextTertiary = Color(0xFF9AA391)

    // Bloques pastel del nuevo diseño
    val Hero = Color(0xFF5E7C50)
    val HeroDark = Color(0xFF4F6B45)
    val HeroLight = Color(0xFF7C9A6D)
    val Cream = Color(0xFFFBF7EA)
    val Lavender = Color(0xFFEFEEF7)
    val SageSoft = Color(0xFFEAF0E3)
    val Peach = Color(0xFFFAEEE6)
    val Sky = Color(0xFFEAF0F7)
    val Rose = Color(0xFFF7EBED)
}

private val StudyHubColorScheme = lightColorScheme(
    primary = StudyHubColors.Primary,
    onPrimary = Color.White,
    primaryContainer = StudyHubColors.SageSoft,
    onPrimaryContainer = StudyHubColors.PrimaryDark,
    inversePrimary = StudyHubColors.PrimaryLight,
    secondary = StudyHubColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = StudyHubColors.SageSoft,
    onSecondaryContainer = StudyHubColors.PrimaryDark,
    tertiary = StudyHubColors.Accent,
    onTertiary = Color.White,
    tertiaryContainer = StudyHubColors.Peach,
    onTertiaryContainer = StudyHubColors.AccentDark,
    background = StudyHubColors.Bg,
    onBackground = StudyHubColors.TextPrimary,
    surface = StudyHubColors.Surface,
    onSurface = StudyHubColors.TextPrimary,
    surfaceVariant = StudyHubColors.SurfaceLight,
    onSurfaceVariant = StudyHubColors.TextSecondary,
    surfaceTint = StudyHubColors.Primary,
    outline = StudyHubColors.Border,
    outlineVariant = StudyHubColors.SurfaceLight,
    error = StudyHubColors.Danger,
    onError = Color.White,
    errorContainer = StudyHubColors.Rose,
    onErrorContainer = StudyHubColors.DangerLight,
)

private val StudyHubShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun StudyHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudyHubColorScheme,
        shapes = StudyHubShapes,
        // TODO: cargar la fuente Inter como recurso y aplicarla aquí
        content = content,
    )
}
