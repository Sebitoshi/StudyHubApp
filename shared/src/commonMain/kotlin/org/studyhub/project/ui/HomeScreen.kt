package org.studyhub.project.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WorkspacePremium
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.studyhub.project.StudyHubColors
import org.studyhub.project.net.Api
import org.studyhub.project.net.CalendarEvent
import org.studyhub.project.net.DashboardSummary
import org.studyhub.project.net.GamificationProgress
import org.studyhub.project.net.RiskLevel
import org.studyhub.project.net.TokenStore
import org.studyhub.project.net.UpcomingClass
import org.studyhub.project.net.platformCurrentTimeHM
import org.studyhub.project.net.platformTodayDayOfWeek
import org.studyhub.project.net.platformTodayIso
import org.studyhub.project.ui.icons.AppIcon
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    unread: Int,
    onBell: () -> Unit,
    onOpenSpace: (SpaceTarget) -> Unit,
    onOpenSubject: (Int, String, String) -> Unit,
    onOpenSubjects: () -> Unit,
) {
    val demo = remember { TokenStore.load() == null }
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var exams by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var todayClasses by remember { mutableStateOf<List<UpcomingClass>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        if (demo) {
            summary = DemoData.dashboard
            exams = DemoData.exams
            todayClasses = DemoData.dashboard.upcomingClasses
            error = null
            loading = false
            return@LaunchedEffect
        }
        try {
            summary = Api.client.dashboard()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el resumen"
        } finally {
            loading = false
        }
        // Exámenes próximos (no bloquean el dashboard si fallan)
        exams = runCatching { Api.client.upcomingExams() }.getOrDefault(emptyList())

        // Clases de HOY (todas las del día, incluidas las en curso y las que ya terminaron):
        // el backend solo manda las que aún no empiezan, así que las calculamos de los horarios.
        try {
            val subjects = Api.client.subjects()
            val today = todayDayOfWeek()
            val details = coroutineScope {
                subjects.map { s ->
                    async { runCatching { Api.client.subjectDetail(s.id) }.getOrNull() }
                }.mapNotNull { it.await() }
            }
            todayClasses = details
                .flatMap { d ->
                    d.schedules.filter { it.dayOfWeek == today }.map { sch ->
                        UpcomingClass(
                            subject = d.nombre,
                            classroom = sch.classroom,
                            profesor = d.profesor,
                            startTime = sch.startTime,
                            endTime = sch.endTime,
                            color = d.color,
                        )
                    }
                }
                .sortedBy { it.startTime }
        } catch (_: Exception) {
            // Sin conexión: se muestran las del resumen (si las hay)
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            loading -> LoadingState()
            error != null -> ErrorState(error.orEmpty()) {
                loading = true
                error = null
                summary = null
            }
            else -> summary?.let {
                DashboardContent(
                    data = it,
                    exams = exams,
                    todayClasses = todayClasses,
                    unread = unread,
                    onBell = onBell,
                    onOpenSpace = onOpenSpace,
                    onOpenSubject = onOpenSubject,
                    onOpenSubjects = onOpenSubjects,
                    displayName = if (demo) DemoData.userName else (it.user?.nombre ?: "Estudiante"),
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    data: DashboardSummary,
    exams: List<CalendarEvent>,
    todayClasses: List<UpcomingClass>,
    unread: Int,
    onBell: () -> Unit,
    onOpenSpace: (SpaceTarget) -> Unit,
    onOpenSubject: (Int, String, String) -> Unit,
    onOpenSubjects: () -> Unit,
    displayName: String,
) {
    val stats = data.stats
    val gamification = data.gamification
    val completion = data.completionRate.roundToInt().coerceIn(0, 100)
    val firstName = data.user?.nombre?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "Estudiante"
    val nextClass = todayClasses.firstOrNull() ?: data.upcomingClasses.firstOrNull()
    val pending = stats?.pendingTasks ?: 0
    val completed = stats?.completedTasks ?: 0
    val totalTasks = completed + pending
    val pendingProgress = if (totalTasks > 0) completed.toFloat() / totalTasks else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        StudyHubTopBar(
            eyebrow = todayLongLabel(),
            title = "${greetingForNow()}, $firstName",
            unread = unread,
            onBell = onBell,
            userName = displayName,
        )
        Spacer(Modifier.height(18.dp))

        // Progreso semanal
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Tu progreso semanal",
                color = StudyHubColors.TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$completion%",
                color = StudyHubColors.TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(7.dp))
        GlassProgress(
            progress = completion / 100f,
            color = StudyHubColors.Primary,
            track = StudyHubColors.SurfaceLight,
            height = 6.dp,
        )
        Spacer(Modifier.height(18.dp))

        // Hero de enfoque
        FocusHero(
            progress = completion,
            onStart = { onOpenSpace(SpaceTarget.FOCUS) },
        )
        Spacer(Modifier.height(22.dp))

        // Tu día
        SectionTitle(
            text = "Tu día",
            action = "Ver agenda",
            onAction = { onOpenSpace(SpaceTarget.CALENDAR) },
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DayCard(
                modifier = Modifier.weight(1f),
                background = StudyHubColors.Cream,
                icon = Icons.Outlined.MenuBook,
                iconTint = StudyHubColors.Accent,
                eyebrow = "Próxima clase",
                title = nextClass?.subject ?: "Sin clases hoy",
                subtitle = if (nextClass != null) {
                    buildString {
                        append(nextClass.startTime.ifBlank { "—" })
                        if (nextClass.classroom.isNotBlank()) append(" — ${nextClass.classroom}")
                    }
                } else {
                    "Disfruta tu día"
                },
            )
            DayCard(
                modifier = Modifier.weight(1f),
                background = StudyHubColors.Lavender,
                icon = Icons.Outlined.AccessTime,
                iconTint = StudyHubColors.Info,
                eyebrow = "Tiempo de estudio",
                title = "$pending tareas",
                subtitle = if (totalTasks > 0) "$completed de $totalTasks completadas" else "Todo al día",
                progress = pendingProgress,
                progressColor = StudyHubColors.Info,
            )
        }
        Spacer(Modifier.height(12.dp))

        // Racha
        StreakCard(
            streak = gamification?.streak ?: 0,
            onClick = { onOpenSpace(SpaceTarget.ACHIEVEMENTS) },
        )
        Spacer(Modifier.height(22.dp))

        // Recomendado
        SectionTitle(
            text = "Recomendado para ti",
            action = "Ver todo",
            onAction = onOpenSubjects,
        )
        Spacer(Modifier.height(10.dp))

        val recommendations = data.upcomingTasks.take(3)
        if (recommendations.isNotEmpty()) {
            recommendations.forEach { task ->
                RecommendationRow(
                    title = task.title,
                    subtitle = buildString {
                        if (task.subject.isNotBlank()) append(task.subject)
                        val days = daysUntilDue(task.dueDate)
                        if (days != null) {
                            if (isNotEmpty()) append(" · ")
                            append(
                                when {
                                    days < 0 -> "atrasada"
                                    days == 0 -> "hoy"
                                    days == 1 -> "mañana"
                                    else -> "en $days días"
                                },
                            )
                        }
                    },
                    tint = parseColor(task.subjectColor),
                    icon = Icons.Outlined.PendingActions,
                    onClick = { onOpenSubject(task.subjectId, task.subject, task.subjectColor) },
                )
                Spacer(Modifier.height(9.dp))
            }
        } else {
            data.recentNotes.take(3).forEach { note ->
                RecommendationRow(
                    title = note.title,
                    subtitle = note.subject.ifBlank { "Apunte" },
                    tint = parseColor(note.subjectColor),
                    icon = if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.NoteAlt,
                    onClick = {},
                )
                Spacer(Modifier.height(9.dp))
            }
        }

        if (exams.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            SectionTitle(
                text = "Exámenes y eventos",
                action = "Ver agenda",
                onAction = { onOpenSpace(SpaceTarget.CALENDAR) },
            )
            Spacer(Modifier.height(10.dp))
            exams.take(2).forEach { exam ->
                RecommendationRow(
                    title = exam.title.ifBlank { "Examen" },
                    subtitle = buildString {
                        append(formatDate(exam.startAt))
                        if (exam.subject?.nombre?.isNotBlank() == true) append(" · ${exam.subject?.nombre}")
                    },
                    tint = StudyHubColors.Danger,
                    icon = Icons.Outlined.Event,
                    onClick = { onOpenSpace(SpaceTarget.CALENDAR) },
                )
                Spacer(Modifier.height(9.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FocusHero(progress: Int, onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(StudyHubColors.Hero, StudyHubColors.HeroDark),
                ),
            )
            .clickable(onClick = onStart)
            .padding(20.dp),
    ) {
        // Círculo decorativo del fondo
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(112.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f)),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "ENFOQUE DE HOY",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Un paso a la vez,\ntú puedes.",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.4).sp,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "Completa tus metas y mantén tu racha.",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White)
                        .clickable(onClick = onStart)
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = "Empezar",
                        color = StudyHubColors.Primary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    icon = Icons.Outlined.TrackChanges,
                    tint = Color.White.copy(alpha = 0.92f),
                    size = 30.dp,
                )
            }
        }
    }
}

@Composable
private fun DayCard(
    modifier: Modifier,
    background: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    eyebrow: String,
    title: String,
    subtitle: String,
    progress: Float? = null,
    progressColor: Color = StudyHubColors.Primary,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(icon, tint = iconTint, size = 17.dp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = eyebrow,
            color = StudyHubColors.TextSecondary,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = title,
            color = StudyHubColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = StudyHubColors.TextTertiary,
            fontSize = 10.sp,
            maxLines = 1,
        )
        if (progress != null) {
            Spacer(Modifier.height(8.dp))
            GlassProgress(
                progress = progress,
                color = progressColor,
                track = Color.White.copy(alpha = 0.8f),
                height = 4.dp,
            )
        }
    }
}

@Composable
private fun StreakCard(streak: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StudyHubColors.Peach)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(Icons.Outlined.LocalFireDepartment, tint = StudyHubColors.Accent, size = 21.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (streak > 0) "¡Llevas $streak días!" else "Empieza tu racha",
                color = StudyHubColors.TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (streak > 0) "Tu constancia está dando frutos." else "Un pequeño paso cada día.",
                color = StudyHubColors.TextSecondary,
                fontSize = 10.5.sp,
            )
        }
        AppIcon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            tint = StudyHubColors.TextSecondary,
            size = 18.dp,
        )
    }
}

@Composable
private fun RecommendationRow(
    title: String,
    subtitle: String,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StudyHubColors.Surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(icon, tint = tint, size = 18.dp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = StudyHubColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = StudyHubColors.TextTertiary,
                fontSize = 10.5.sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(6.dp))
        AppIcon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            tint = StudyHubColors.TextTertiary,
            size = 17.dp,
        )
    }
}

// ─── Helpers de fecha ───────────────────────────────────────────────────

private val DAYS_LONG = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
private val MONTHS_LONG = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

/** "Sábado, 25 de octubre" para el eyebrow del inicio. */
internal fun todayLongLabel(): String {
    val parts = todayIso().split("-")
    val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
    val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
    val dow = platformTodayDayOfWeek().coerceIn(0, 6)
    return "${DAYS_LONG[dow]}, $day de ${MONTHS_LONG.getOrNull(month - 1) ?: ""}"
}

internal fun greetingForNow(): String {
    val hour = platformCurrentTimeHM().substringBefore(":").toIntOrNull() ?: 12
    return when {
        hour < 12 -> "Buenos días"
        hour < 19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

@Composable
fun StreakPill(streak: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(StudyHubColors.Peach)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(Icons.Outlined.LocalFireDepartment, tint = StudyHubColors.Accent, size = 17.dp)
        Spacer(Modifier.width(7.dp))
        Text(
            text = "$streak días de racha",
            color = StudyHubColors.AccentDark,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun GamificationCard(g: GamificationProgress) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LeadingIcon(Icons.Outlined.WorkspacePremium, StudyHubColors.Accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Nivel ${g.level}",
                    color = StudyHubColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${g.xp} XP · ${g.xpForNextLevel} XP para el siguiente nivel",
                    color = StudyHubColors.TextTertiary,
                    fontSize = 12.sp,
                )
            }
            Badge("${g.achievements} logros", StudyHubColors.Accent)
        }
        Spacer(Modifier.height(12.dp))
        GlassProgress(
            progress = if (g.xpForNextLevel > 0) g.xp.toFloat() / g.xpForNextLevel else 0f,
            color = StudyHubColors.Accent,
        )
    }
}

@Composable
fun RiskCard(risk: RiskLevel, onOpenSpace: () -> Unit) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LeadingIcon(Icons.Outlined.Security, StudyHubColors.Danger)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Riesgo Académico",
                    color = StudyHubColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Nivel de riesgo: ${risk.level}",
                    color = riskColor(risk.level),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            ScoreRing(score = risk.score, label = "riesgo", color = riskColor(risk.level), size = 74.dp)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenSpace)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Ver análisis",
                color = StudyHubColors.Primary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
            )
            AppIcon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                tint = StudyHubColors.Primary,
                size = 15.dp,
            )
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────

fun parseColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    val value = clean.toLongOrNull(16) ?: return StudyHubColors.Primary
    return if (clean.length == 6) Color(0xFF000000L or value) else Color(value)
}

fun riskColor(level: String): Color = when (level.uppercase()) {
    "ALTO", "HIGH" -> StudyHubColors.Danger
    "MEDIO", "MEDIUM" -> StudyHubColors.Accent
    else -> StudyHubColors.Secondary
}

val MONTHS_SHORT = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** Días naturales que faltan para la fecha ISO, en hora local; negativo si ya pasó. */
fun daysUntilDue(iso: String): Int? {
    if (iso.isBlank()) return null
    val parts = iso.substringBefore("T").split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    val dueDay = dateToEpochDay(y, m, d) ?: return null
    val todayParts = todayIso().split("-")
    val ty = todayParts.getOrNull(0)?.toIntOrNull() ?: return null
    val tm = todayParts.getOrNull(1)?.toIntOrNull() ?: return null
    val td = todayParts.getOrNull(2)?.toIntOrNull() ?: return null
    val today = dateToEpochDay(ty, tm, td) ?: return null
    return (dueDay - today).toInt()
}

/** Día epoch (días desde 1970-01-01) de una fecha civil. */
internal fun dateToEpochDay(y: Int, m: Int, d: Int): Long? {
    if (m < 1 || m > 12 || d < 1 || d > 31) return null
    var year = y.toLong()
    var month = m.toLong()
    if (month <= 2) {
        year -= 1
        month += 12
    }
    val era = year / 400
    val yoe = year - era * 400
    val doy = (153 * (month - 3) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

/** Fecha local de hoy en formato AAAA-MM-DD. */
fun todayIso(): String = platformTodayIso()

/** Badge con los días que faltan para la fecha límite (reemplaza la prioridad). */
@Composable
fun DaysLeftBadge(dueDate: String) {
    val days = daysUntilDue(dueDate) ?: return
    val (text, color) = when {
        days < 0 -> "Atrasada" to StudyHubColors.Danger
        days == 0 -> "Hoy" to StudyHubColors.Accent
        days == 1 -> "Mañana" to StudyHubColors.Accent
        else -> "en $days días" to StudyHubColors.Info
    }
    Badge(text, color, small = true)
}

fun formatDate(iso: String): String {
    if (iso.isBlank()) return ""
    val datePart = iso.substringBefore("T")
    val parts = datePart.split("-")
    if (parts.size != 3) return datePart
    val day = parts[2].toIntOrNull() ?: return datePart
    val month = parts[1].toIntOrNull() ?: return datePart
    return "$day ${MONTHS_SHORT.getOrNull(month - 1) ?: parts[1]}"
}

private fun timeOf(iso: String): String {
    if (iso.isBlank()) return ""
    val t = iso.substringAfter("T", "").substringBefore("Z").substringBefore("+")
    return t.split(":").take(2).joinToString(":")
}

/** dayOfWeek del backend (0=Domingo … 6=Sábado) para hoy, usando hora LOCAL. */
internal fun todayDayOfWeek(): Int = platformTodayDayOfWeek()

/** "En curso", "Terminó" o null según la hora actual local. */
internal fun classStatus(startTime: String, endTime: String): String? {
    if (startTime.isBlank() || endTime.isBlank()) return null
    val now = platformCurrentTimeHM()
    return when {
        startTime <= now && now < endTime -> "En curso"
        endTime <= now -> "Terminó"
        else -> null
    }
}
