package org.studyhub.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.studyhub.project.StudyHubColors
import org.studyhub.project.net.Api
import org.studyhub.project.net.DashboardSummary
import org.studyhub.project.net.PersonalInfo
import org.studyhub.project.net.ProfileAcademic
import org.studyhub.project.net.TokenStore
import org.studyhub.project.ui.icons.AppIcon
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onOpenSpace: (SpaceTarget) -> Unit,
    photoUrl: String? = null,
    unread: Int = 0,
    onBell: () -> Unit = {},
) {
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var academic by remember { mutableStateOf<ProfileAcademic?>(null) }
    var personal by remember { mutableStateOf<PersonalInfo?>(null) }
    var hours by remember { mutableStateOf("0") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    val demo = remember { TokenStore.load() == null }

    LaunchedEffect(reload) {
        loading = true
        if (demo) {
            summary = DemoData.dashboard
            academic = DemoData.academic
            personal = PersonalInfo(
                nombre = "María",
                apellido = "González",
                ciudad = "Bogotá",
                pais = "Colombia",
            )
            hours = "36"
            error = null
            loading = false
            return@LaunchedEffect
        }
        try {
            summary = Api.client.dashboard()
            academic = runCatching { Api.client.profileAcademic() }.getOrNull()
            personal = runCatching { Api.client.personalInfo() }.getOrNull()
            hours = runCatching { Api.client.studyTimerStats() }.getOrNull()?.totalHours ?: "0"
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el perfil"
        } finally {
            loading = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            loading -> LoadingState()
            error != null -> ErrorState(error.orEmpty()) {
                loading = true
                error = null
                reload++
            }

            else -> {
                val firstName = personal?.nombre?.takeIf { it.isNotBlank() }
                    ?: summary?.user?.nombre?.takeIf { it.isNotBlank() }
                    ?: "Estudiante"
                val lastName = personal?.apellido?.takeIf { it.isNotBlank() }.orEmpty()
                val fullName = "$firstName $lastName".trim()
                val career = academic?.carrera?.takeIf { it.isNotBlank() }
                val location = listOfNotNull(
                    personal?.ciudad?.takeIf { it.isNotBlank() },
                    personal?.pais?.takeIf { it.isNotBlank() },
                ).joinToString(", ").ifBlank {
                    academic?.universidad?.takeIf { it.isNotBlank() }.orEmpty()
                }

                val gamification = summary?.gamification
                val streak = gamification?.streak ?: 0
                val achievements = gamification?.achievements ?: 0
                val weekly = (summary?.completionRate ?: 0.0).roundToInt().coerceIn(0, 100)
                val pending = summary?.stats?.pendingTasks ?: 0
                val hoursLabel = hours.substringBefore(".").ifBlank { "0" }

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    StudyHubTopBar(
                        eyebrow = "Tu espacio personal",
                        title = "Mi perfil",
                        unread = unread,
                        onBell = onBell,
                        userName = fullName,
                        userPhoto = photoUrl ?: personal?.foto ?: summary?.user?.foto,
                    )
                    Spacer(Modifier.height(18.dp))

                    IdentityCard(
                        name = fullName,
                        subtitle = career?.let { "Estudiante de $it" } ?: "Estudiante",
                        location = location,
                        photoUrl = photoUrl ?: personal?.foto ?: summary?.user?.foto,
                        onEdit = { onOpenSpace(SpaceTarget.ACADEMIC) },
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("$streak", "Días de racha", Modifier.weight(1f))
                        StatCard("$weekly%", "Promedio semanal", Modifier.weight(1f))
                        StatCard(hoursLabel, "Horas estudiadas", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(22.dp))

                    SectionHeader(
                        title = "Tu progreso",
                        action = "Ver detalles",
                        onAction = { onOpenSpace(SpaceTarget.RISK) },
                    )
                    Spacer(Modifier.height(10.dp))
                    WeeklyGoalCard(progress = weekly / 100f, pending = pending)
                    Spacer(Modifier.height(22.dp))

                    Text(
                        text = "Cuenta",
                        color = StudyHubColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))

                    AccountRow(
                        icon = Icons.Outlined.MailOutline,
                        background = StudyHubColors.SageSoft,
                        tint = StudyHubColors.Primary,
                        title = "Notificaciones",
                        subtitle = "Activadas",
                        onClick = { onOpenSpace(SpaceTarget.NOTIFICATIONS) },
                    )
                    Spacer(Modifier.height(8.dp))
                    AccountRow(
                        icon = Icons.Outlined.EmojiEvents,
                        background = StudyHubColors.Peach,
                        tint = StudyHubColors.Accent,
                        title = "Logros y reconocimientos",
                        subtitle = "$achievements desbloqueados",
                        onClick = { onOpenSpace(SpaceTarget.ACHIEVEMENTS) },
                    )
                    Spacer(Modifier.height(8.dp))
                    AccountRow(
                        icon = Icons.Outlined.Settings,
                        background = StudyHubColors.Lavender,
                        tint = StudyHubColors.Info,
                        title = "Configuración",
                        subtitle = "Preferencias de la app",
                        onClick = { onOpenSpace(SpaceTarget.ACADEMIC) },
                    )
                    Spacer(Modifier.height(8.dp))
                    // Se conserva el acceso al horario semanal, como pediste.
                    AccountRow(
                        icon = Icons.Outlined.CalendarMonth,
                        background = StudyHubColors.Sky,
                        tint = StudyHubColors.Info,
                        title = "Horario",
                        subtitle = "Clases semanales",
                        onClick = { onOpenSpace(SpaceTarget.SCHEDULE) },
                    )
                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onLogout)
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(Icons.AutoMirrored.Outlined.Logout, tint = StudyHubColors.TextTertiary, size = 17.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Cerrar sesión",
                            color = StudyHubColors.TextTertiary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun IdentityCard(
    name: String,
    subtitle: String,
    location: String,
    photoUrl: String?,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StudyHubColors.SageSoft)
            .border(1.dp, StudyHubColors.Border, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            name = name,
            photoUrl = photoUrl,
            size = 64.dp,
            shape = RoundedCornerShape(18.dp),
            gradient = listOf(StudyHubColors.HeroLight, StudyHubColors.Hero),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                color = StudyHubColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = StudyHubColors.TextSecondary,
                fontSize = 11.5.sp,
                maxLines = 1,
            )
            if (location.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(Icons.Outlined.LocationOn, tint = StudyHubColors.TextTertiary, size = 12.dp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = location,
                        color = StudyHubColors.TextTertiary,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(StudyHubColors.Surface)
                .border(1.dp, StudyHubColors.Border, CircleShape)
                .clickable(onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(Icons.Outlined.Edit, tint = StudyHubColors.TextSecondary, size = 16.dp)
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(StudyHubColors.Surface)
            .border(1.dp, StudyHubColors.Border, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = StudyHubColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = StudyHubColors.TextTertiary,
            fontSize = 9.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = StudyHubColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAction)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = action,
                color = StudyHubColors.TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(2.dp))
            AppIcon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, tint = StudyHubColors.TextTertiary, size = 15.dp)
        }
    }
}

@Composable
private fun WeeklyGoalCard(progress: Float, pending: Int) {
    val safe = progress.coerceIn(0f, 1f)
    val percent = (safe * 100).roundToInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StudyHubColors.SageSoft)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Meta semanal",
                color = StudyHubColors.TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$percent%",
                color = StudyHubColors.Primary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.7f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(safe)
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(StudyHubColors.Primary),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (pending > 0) {
                "Te faltan $pending tarea${if (pending == 1) "" else "s"} para alcanzar tu meta."
            } else {
                "¡Meta alcanzada! Sigue así."
            },
            color = StudyHubColors.TextTertiary,
            fontSize = 10.5.sp,
        )
    }
}

@Composable
private fun AccountRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StudyHubColors.Surface)
            .border(1.dp, StudyHubColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(icon, tint = tint, size = 19.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = StudyHubColors.TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                color = StudyHubColors.TextTertiary,
                fontSize = 10.5.sp,
            )
        }
        AppIcon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            tint = StudyHubColors.TextTertiary,
            size = 18.dp,
        )
    }
}

@Composable
internal fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(icon, tint = StudyHubColors.InfoLight, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(label, color = StudyHubColors.TextTertiary, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
        Text(value, color = StudyHubColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
