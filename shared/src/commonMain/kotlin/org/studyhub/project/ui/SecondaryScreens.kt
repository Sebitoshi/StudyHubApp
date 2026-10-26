package org.studyhub.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WorkHistory
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.studyhub.project.StudyHubColors
import org.studyhub.project.net.AiGoal
import org.studyhub.project.net.AiResource
import org.studyhub.project.net.Api
import org.studyhub.project.net.CalendarData
import org.studyhub.project.net.CalendarEvent
import org.studyhub.project.net.CalendarEventRequest
import org.studyhub.project.net.KnowledgeGap
import org.studyhub.project.net.ProfileAcademic
import org.studyhub.project.net.ResumeMe
import org.studyhub.project.net.RiskAnalysis
import org.studyhub.project.net.Roadmap
import org.studyhub.project.net.SandboxExercise
import org.studyhub.project.net.SandboxStats
import org.studyhub.project.net.StudySession
import org.studyhub.project.net.StudyTimerStats
import org.studyhub.project.net.Subject
import org.studyhub.project.net.TokenStore
import org.studyhub.project.net.dayName
import org.studyhub.project.ui.icons.AppIcon

// ─── Destinos de "Tu espacio" ───────────────────────────────────────────

enum class SpaceTarget(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradient: List<Color>,
) {
    RISK("Riesgo Académico", "Análisis de tu actividad", Icons.Outlined.Security, listOf(StudyHubColors.Accent, StudyHubColors.Danger)),
    ACHIEVEMENTS("Logros", "Gamificación", Icons.Outlined.EmojiEvents, listOf(StudyHubColors.Accent, StudyHubColors.AccentDark)),
    QUIZZES("Quizzes", "Generados por IA", Icons.Outlined.Quiz, listOf(StudyHubColors.Accent, StudyHubColors.Danger)),
    BREACHES("Brechas", "De conocimiento", Icons.Outlined.Psychology, listOf(StudyHubColors.Info, StudyHubColors.Primary)),
    SCHEDULE("Horario", "Clases semanales", Icons.Outlined.CalendarMonth, listOf(StudyHubColors.Accent, StudyHubColors.AccentLight)),
    CALENDAR("Calendario", "Exámenes y eventos", Icons.Outlined.CalendarMonth, listOf(StudyHubColors.Info, StudyHubColors.InfoLight)),
    FOCUS("Enfoque", "Temporizador de estudio", Icons.Outlined.Timer, listOf(StudyHubColors.Primary, StudyHubColors.Secondary)),
    ACADEMIC("Perfil Académico", "Carrera, semestre y promedio", Icons.Outlined.School, listOf(StudyHubColors.Primary, StudyHubColors.Info)),
    NOTIFICATIONS("Notificaciones", "Alertas y recordatorios", Icons.Outlined.Notifications, listOf(StudyHubColors.Danger, StudyHubColors.Accent)),
    GOALS("Metas de Aprendizaje", "IA · progreso y fechas objetivo", Icons.Outlined.TrackChanges, listOf(StudyHubColors.Info, StudyHubColors.Primary)),
    // No se muestran en la grilla (perfil): CV y Laboratorio
    CV("Mi CV", "Builder + PDF", Icons.Outlined.Description, listOf(StudyHubColors.Info, StudyHubColors.Primary)),
    LAB("Laboratorio", "Ejercicios", Icons.Outlined.Science, listOf(StudyHubColors.Primary, StudyHubColors.Secondary)),
}

private data class SpaceGroup(val title: String, val targets: List<SpaceTarget>)

/** Vistas de "Tu espacio" agrupadas por categoría, como filas compactas. */
private val SPACE_GROUPS = listOf(
    SpaceGroup("Rendimiento", listOf(SpaceTarget.RISK, SpaceTarget.BREACHES)),
    SpaceGroup("Aprendizaje", listOf(SpaceTarget.QUIZZES, SpaceTarget.GOALS)),
    SpaceGroup("Organización", listOf(SpaceTarget.SCHEDULE, SpaceTarget.CALENDAR, SpaceTarget.FOCUS)),
    SpaceGroup("Cuenta", listOf(SpaceTarget.NOTIFICATIONS)),
)

@Composable
fun SpaceGrid(onOpenSpace: (SpaceTarget) -> Unit) {
    Column {
        SPACE_GROUPS.forEachIndexed { index, group ->
            Text(
                text = group.title.uppercase(),
                color = StudyHubColors.TextTertiary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, top = if (index == 0) 0.dp else 10.dp, bottom = 6.dp),
            )
            group.targets.forEach { target ->
                SpaceRow(target = target, onClick = { onOpenSpace(target) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SpaceRow(target: SpaceTarget, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        radius = 16.dp,
        contentPadding = 12.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = target.icon, gradient = target.gradient, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = target.label,
                    color = StudyHubColors.TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (target.subtitle.isNotBlank()) {
                    Text(
                        text = target.subtitle,
                        color = StudyHubColors.TextTertiary,
                        fontSize = 11.sp,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            AppIcon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, tint = StudyHubColors.TextTertiary, size = 20.dp)
        }
    }
}

// ─── Despachador de vistas secundarias ──────────────────────────────────

@Composable
fun SecondaryScreen(target: SpaceTarget, onBack: () -> Unit) {
    when (target) {
        SpaceTarget.CV -> CvScreen(onBack)
        SpaceTarget.LAB -> LabScreen(onBack)
        SpaceTarget.SCHEDULE -> ScheduleScreen(onBack)
        SpaceTarget.RISK -> RiskScreen(onBack)
        SpaceTarget.ACHIEVEMENTS -> AchievementsScreen(onBack)
        SpaceTarget.QUIZZES -> QuizzesScreen(onBack)
        SpaceTarget.BREACHES -> BreachesScreen(onBack)
        SpaceTarget.CALENDAR -> CalendarScreen(onBack)
        SpaceTarget.FOCUS -> FocusScreen(onBack)
        SpaceTarget.ACADEMIC -> AcademicScreen(onBack)
        SpaceTarget.NOTIFICATIONS -> NotificationsOverlay(onClose = onBack)
        SpaceTarget.GOALS -> GoalsScreen(onBack)
    }
}

// ─── CV / Hoja de vida ──────────────────────────────────────────────────

@Composable
private fun CvScreen(onBack: () -> Unit) {
    var resume by remember { mutableStateOf<ResumeMe?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            resume = Api.client.resume()
            error = null
        } catch (e: Exception) {
            // El backend responde 404 {"message":"CV no encontrado"} cuando aún no existe;
            // se muestra como estado vacío en vez de un error.
            if (e.message?.contains("no encontrado", ignoreCase = true) == true) {
                resume = null
            } else {
                error = e.message ?: "No se pudo cargar tu CV"
            }
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar("Mi CV", "Constructor de hoja de vida", onBack)
        Box(Modifier.weight(1f)) {
            when {
                loading -> LoadingState()
                error != null -> ErrorState(error.orEmpty())
                else -> resume?.let { CvContent(it) } ?: EmptyState("Aún no has creado tu CV. Créalo desde la web y aparecerá aquí.")
            }
        }
    }
}

@Composable
private fun CvContent(r: ResumeMe) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        if (r.titulo.isBlank() && r.resumen.isBlank()) {
            EmptyState("Aún no has creado tu CV. Créalo desde la web y aparecerá aquí.")
            return
        }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Outlined.Description, SpaceTarget.CV.gradient, size = 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.titulo.ifBlank { "Mi CV" }, color = StudyHubColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    if (r.slug.isNotBlank()) {
                        Text("studyhub.app/cv/${r.slug}", color = StudyHubColors.PrimaryLight, fontSize = 11.5.sp)
                    }
                }
            }
            if (r.resumen.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(r.resumen, color = StudyHubColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        Spacer(Modifier.height(18.dp))

        if (r.experiences.isNotEmpty()) {
            SectionTitle("Experiencia", icon = Icons.Outlined.WorkHistory, iconTint = StudyHubColors.InfoLight)
            Spacer(Modifier.height(10.dp))
            r.experiences.forEach { exp ->
                GlassCard(modifier = Modifier.padding(bottom = 8.dp), radius = 16.dp, contentPadding = 14.dp) {
                    Text(exp.position, color = StudyHubColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${exp.company} · ${exp.startDate.take(4)}${if (exp.isCurrent) " — actual" else " — ${exp.endDate.take(4)}"}", color = StudyHubColors.TextTertiary, fontSize = 12.sp)
                    if (exp.description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(exp.description, color = StudyHubColors.TextSecondary, fontSize = 12.5.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (r.educations.isNotEmpty()) {
            SectionTitle("Educación", icon = Icons.Outlined.MenuBook, iconTint = StudyHubColors.SecondaryLight)
            Spacer(Modifier.height(10.dp))
            r.educations.forEach { edu ->
                GlassCard(modifier = Modifier.padding(bottom = 8.dp), radius = 16.dp, contentPadding = 14.dp) {
                    Text(edu.degree, color = StudyHubColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${edu.institution} · ${edu.startDate.take(4)} — ${edu.endDate.take(4)}", color = StudyHubColors.TextTertiary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (r.projects.isNotEmpty()) {
            SectionTitle("Proyectos", icon = Icons.Outlined.WorkspacePremium, iconTint = StudyHubColors.AccentLight)
            Spacer(Modifier.height(10.dp))
            r.projects.forEach { proj ->
                GlassCard(modifier = Modifier.padding(bottom = 8.dp), radius = 16.dp, contentPadding = 14.dp) {
                    Text(proj.name, color = StudyHubColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (proj.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(proj.description, color = StudyHubColors.TextSecondary, fontSize = 12.5.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (r.certificates.isNotEmpty()) {
            SectionTitle("Certificados", icon = Icons.Outlined.Star, iconTint = StudyHubColors.AccentLight)
            Spacer(Modifier.height(10.dp))
            r.certificates.forEach { cert ->
                GlassCard(modifier = Modifier.padding(bottom = 8.dp), radius = 16.dp, contentPadding = 14.dp) {
                    Text(cert.name, color = StudyHubColors.TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("${cert.issuer} · ${cert.year}", color = StudyHubColors.TextTertiary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (r.languages.isNotEmpty()) {
            SectionTitle("Idiomas", icon = Icons.Outlined.Language, iconTint = StudyHubColors.InfoLight)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                r.languages.forEach { lang ->
                    Badge(lang.name.ifBlank { "Idioma" } + if (lang.level.isNotBlank()) " · ${lang.level}" else "", StudyHubColors.InfoLight)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ─── Laboratorio / Sandbox ──────────────────────────────────────────────

@Composable
private fun LabScreen(onBack: () -> Unit) {
    var stats by remember { mutableStateOf<SandboxStats?>(null) }
    var exercises by remember { mutableStateOf<List<SandboxExercise>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            stats = Api.client.sandboxStats()
            exercises = Api.client.sandboxExercises()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el laboratorio"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar("Laboratorio", "Práctica de ejercicios", onBack)
        Box(Modifier.weight(1f)) {
            when {
                loading -> LoadingState()
                error != null -> ErrorState(error.orEmpty())
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                ) {
                    val s = stats
                    if (s != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(Icons.Outlined.CheckCircle, StudyHubColors.SecondaryLight, "${s.resolved}", "Ejercicios resueltos", Modifier.weight(1f))
                            StatCard(Icons.Outlined.Science, StudyHubColors.InfoLight, "${s.accuracy}%", "Precisión", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(Icons.Outlined.LocalFireDepartment, StudyHubColors.AccentLight, "${s.streak}", "Racha de práctica", Modifier.weight(1f))
                            StatCard(Icons.Outlined.Quiz, StudyHubColors.PrimaryLight, "${s.totalAttempts}", "Intentos totales", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                    SectionTitle("Ejercicios", icon = Icons.Outlined.Science, iconTint = StudyHubColors.SecondaryLight)
                    Spacer(Modifier.height(10.dp))
                    if (exercises.isEmpty()) {
                        EmptyState("Aún no hay ejercicios en el laboratorio.")
                    }
                    exercises.forEach { ex ->
                        GlassCard(modifier = Modifier.padding(bottom = 8.dp), radius = 16.dp, contentPadding = 14.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LeadingIcon(
                                    if (ex.solved) Icons.Outlined.CheckCircle else Icons.Outlined.Code,
                                    if (ex.solved) StudyHubColors.SecondaryLight else StudyHubColors.PrimaryLight,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(ex.title.ifBlank { "Ejercicio #${ex.id}" }, color = StudyHubColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Badge(ex.language.ifBlank { "GENERAL" }, StudyHubColors.InfoLight, small = true)
                                        Spacer(Modifier.width(8.dp))
                                        Badge(ex.difficulty, difficultyColor(ex.difficulty), small = true)
                                    }
                                }
                                if (ex.solved) {
                                    Badge("Resuelto", StudyHubColors.SecondaryLight, small = true)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

// ─── Horario semanal ────────────────────────────────────────────────────

@Composable
private fun ScheduleScreen(onBack: () -> Unit) {
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            subjects = Api.client.subjects()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el horario"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar("Horario", "Clases de la semana", onBack)
        Box(Modifier.weight(1f)) {
            when {
                loading -> LoadingState()
                error != null -> ErrorState(error.orEmpty())
                // El listado de materias no trae los horarios; se agregan desde el detalle.
                else -> ScheduleAggregated(subjects)
            }
        }
    }
}

@Composable
private fun ScheduleAggregated(subjects: List<Subject>) {
    // El detalle trae los horarios; cargamos los detalles de cada materia.
    var rows by remember(subjects) { mutableStateOf<List<Pair<Subject, org.studyhub.project.net.Schedule>>>(emptyList()) }
    var loadingDetail by remember(subjects) { mutableStateOf(true) }

    LaunchedEffect(subjects) {
        loadingDetail = true
        val all = mutableListOf<Pair<Subject, org.studyhub.project.net.Schedule>>()
        subjects.forEach { sub ->
            runCatching { Api.client.subjectDetail(sub.id) }
                .getOrNull()
                ?.schedules
                ?.forEach { all += sub to it }
        }
        rows = all
        loadingDetail = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        if (loadingDetail) {
            LoadingState()
            return
        }
        if (rows.isEmpty()) {
            EmptyState("Aún no tienes clases en el horario.")
            return
        }
        val byDay = rows.groupBy { dayName(it.second.dayOfWeek) }
        val dayOrder = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        dayOrder.forEach { day ->
            val dayRows = byDay[day] ?: byDay.entries.firstOrNull { it.key.contains(day.take(3), ignoreCase = true) }?.value ?: return@forEach
            SectionTitle(day, icon = Icons.Outlined.CalendarMonth, iconTint = StudyHubColors.InfoLight)
            Spacer(Modifier.height(10.dp))
            dayRows.sortedBy { it.second.startTime }.forEach { (sub, sched) ->
                GlassCard(modifier = Modifier.padding(bottom = 8.dp), radius = 16.dp, contentPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.width(84.dp)) {
                            Text(sched.startTime, color = StudyHubColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            Text(sched.endTime, color = StudyHubColors.TextTertiary, fontSize = 10.5.sp)
                        }
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(34.dp)
                                .background(parseColor(sub.color))
                                .padding(0.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sub.nombre, color = StudyHubColors.TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text("Salón: ${sched.classroom.ifBlank { sub.salon }}", color = StudyHubColors.TextTertiary, fontSize = 11.5.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ─── Calendario / Eventos ───────────────────────────────────────────────

@Composable
internal fun CalendarScreen(onBack: () -> Unit, embedded: Boolean = false) {
    val demo = remember { TokenStore.load() == null }
    var exams by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var data by remember { mutableStateOf<CalendarData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<CalendarEvent?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        if (demo) {
            exams = DemoData.exams
            data = DemoData.calendar
            error = null
            loading = false
            return@LaunchedEffect
        }
        try {
            exams = Api.client.upcomingExams()
            data = Api.client.calendarEvents()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el calendario"
        } finally {
            loading = false
        }
    }

    val editingEvent = editing
    if (editingEvent != null) {
        EditEventScreen(
            event = editingEvent,
            onBack = { editing = null },
            onDone = {
                editing = null
                reload++
            },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        if (!embedded) {
            FullScreenBackBar("Calendario", "Exámenes, eventos y tareas", onBack)
        }
        Box(Modifier.weight(1f)) {
            when {
                loading -> LoadingState()
                error != null -> ErrorState(error.orEmpty())
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                ) {
                    if (embedded) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "TU CALENDARIO",
                            color = StudyHubColors.TextSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Agenda",
                            color = StudyHubColors.TextPrimary,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp,
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                    SectionTitle("Exámenes próximos", icon = Icons.Outlined.Event, iconTint = StudyHubColors.Danger)
                    Spacer(Modifier.height(10.dp))
                    if (exams.isEmpty()) {
                        EmptyState("No tienes exámenes en los próximos 30 días.")
                    }
                    exams.forEach { ex ->
                        CalendarRow(
                            title = ex.title.ifBlank { "Examen" },
                            date = formatDate(ex.startAt),
                            time = timeOf(ex.startAt),
                            subject = ex.subject?.nombre ?: "",
                            color = parseColor(ex.subject?.color?.takeIf { it.isNotBlank() } ?: ex.color),
                            badge = "Examen",
                            badgeColor = StudyHubColors.DangerLight,
                            onClick = { editing = ex },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(18.dp))

                    SectionTitle("Eventos y tareas", icon = Icons.Outlined.CalendarMonth, iconTint = StudyHubColors.InfoLight)
                    Spacer(Modifier.height(10.dp))
                    val items = buildList {
                        data?.events?.forEach { add(CalItem(it.title, it.startAt, it.subject?.nombre ?: "", it.subject?.color?.takeIf { c -> c.isNotBlank() } ?: it.color, "Evento", it)) }
                        data?.tasks?.forEach { add(CalItem(it.title, it.startAt, it.subject?.nombre ?: "", it.subject?.color?.takeIf { c -> c.isNotBlank() } ?: it.color, "Tarea", null)) }
                    }.sortedBy { it.startAt }
                    if (items.isEmpty()) {
                        EmptyState("Sin eventos ni tareas este mes.")
                    }
                    items.forEach { item ->
                        CalendarRow(
                            title = item.title.ifBlank { item.kind },
                            date = formatDate(item.startAt),
                            time = timeOf(item.startAt),
                            subject = item.subject,
                            color = parseColor(item.color),
                            badge = item.kind,
                            badgeColor = if (item.kind == "Tarea") StudyHubColors.AccentLight else StudyHubColors.InfoLight,
                            onClick = {
                                // Solo los eventos propios son editables; las tareas se gestionan en la materia
                                item.event?.let { editing = it }
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

/** Pestaña Agenda: vista semanal del nuevo diseño. */
private data class CalItem(
    val title: String,
    val startAt: String,
    val subject: String,
    val color: String,
    val kind: String,
    val event: CalendarEvent? = null,
)

@Composable
private fun CalendarRow(
    title: String,
    date: String,
    time: String,
    subject: String,
    color: Color,
    badge: String,
    badgeColor: Color,
    onClick: (() -> Unit)? = null,
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        radius = 16.dp,
        contentPadding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .background(color)
                    .padding(0.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = StudyHubColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(
                    buildString {
                        append(date)
                        if (time.isNotBlank()) append(" · $time")
                        if (subject.isNotBlank()) append(" · $subject")
                    },
                    color = StudyHubColors.TextTertiary,
                    fontSize = 11.5.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Badge(badge, badgeColor, small = true)
            if (onClick != null) {
                Spacer(Modifier.width(4.dp))
                AppIcon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, tint = StudyHubColors.TextTertiary, size = 18.dp)
            }
        }
    }
}

private fun timeOf(iso: String): String {
    if (iso.isBlank()) return ""
    val t = iso.substringAfter("T", "").substringBefore("Z").substringBefore("+")
    return t.split(":").take(2).joinToString(":")
}

@Composable
internal fun EditEventScreen(
    event: CalendarEvent,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    var title by remember { mutableStateOf(event.title) }
    var date by remember { mutableStateOf(event.startAt.substringBefore("T")) }
    var startTime by remember { mutableStateOf(timeOf(event.startAt)) }
    var endTime by remember { mutableStateOf(timeOf(event.endAt.ifBlank { event.startAt })) }
    var type by remember { mutableStateOf(if (event.type == "EXAM") "EXAM" else "EVENT") }
    var subjectId by remember { mutableStateOf(event.subject?.id) }
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        subjects = runCatching { Api.client.subjects() }.getOrDefault(emptyList())
    }

    val timesValid = isValidTime(startTime) &&
        (endTime.isBlank() || !isValidTime(endTime) || (timeToMinutes(endTime) ?: 0) > (timeToMinutes(startTime) ?: 0))
    val canSave = title.isNotBlank() && date.length == 10 && isValidTime(startTime) && timesValid && !saving

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar(
            if (event.id == 0) "Nuevo evento" else "Editar ${if (type == "EXAM") "examen" else "evento"}",
            if (event.id == 0) "Completa los datos para agendarlo" else "Toca los campos para modificarlo",
            onBack,
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            GlassTextField(
                value = title,
                onValueChange = { title = it },
                label = "Título",
                placeholder = "Ej: Parcial de Cálculo",
            )
            Spacer(Modifier.height(14.dp))
            GlassTextField(
                value = date,
                onValueChange = { date = it.filter { c -> c.isDigit() || c == '-' }.take(10) },
                label = "Fecha (AAAA-MM-DD)",
                placeholder = "2026-08-15",
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassTextField(
                    value = startTime,
                    onValueChange = { startTime = it.filter { c -> c.isDigit() || c == ':' }.take(5) },
                    label = "Inicio (HH:mm)",
                    placeholder = "08:00",
                    modifier = Modifier.weight(1f),
                )
                GlassTextField(
                    value = endTime,
                    onValueChange = { endTime = it.filter { c -> c.isDigit() || c == ':' }.take(5) },
                    label = "Fin (HH:mm)",
                    placeholder = "10:00 (opcional)",
                    modifier = Modifier.weight(1f),
                )
            }
            if (startTime.isNotBlank() && endTime.isNotBlank() && !timesValid) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "La hora de fin debe ser posterior a la de inicio.",
                    color = StudyHubColors.DangerLight,
                    fontSize = 11.5.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Tipo", color = StudyHubColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("EVENT" to "Evento", "EXAM" to "Examen").forEach { (value, label) ->
                    val active = type == value
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) StudyHubColors.Primary.copy(alpha = 0.18f) else StudyHubColors.Surface)
                            .border(1.dp, if (active) StudyHubColors.Primary else StudyHubColors.Border, RoundedCornerShape(12.dp))
                            .clickable { type = value }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (active) StudyHubColors.PrimaryLight else StudyHubColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Materia", color = StudyHubColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val noneSelected = subjectId == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (noneSelected) StudyHubColors.Primary.copy(alpha = 0.18f) else StudyHubColors.Surface)
                        .border(1.dp, if (noneSelected) StudyHubColors.Primary else StudyHubColors.Border, RoundedCornerShape(12.dp))
                        .clickable { subjectId = null }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        "Sin materia",
                        color = if (noneSelected) StudyHubColors.PrimaryLight else StudyHubColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (noneSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
                subjects.forEach { s ->
                    val active = subjectId == s.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) StudyHubColors.Primary.copy(alpha = 0.18f) else StudyHubColors.Surface)
                            .border(1.dp, if (active) StudyHubColors.Primary else StudyHubColors.Border, RoundedCornerShape(12.dp))
                            .clickable { subjectId = s.id }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            s.nombre,
                            color = if (active) StudyHubColors.PrimaryLight else StudyHubColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error.orEmpty(), color = StudyHubColors.DangerLight, fontSize = 12.5.sp)
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = if (event.id == 0) "Crear evento" else "Guardar cambios",
                icon = Icons.Outlined.Save,
                enabled = canSave,
                loading = saving,
            ) {
                saving = true
                scope.launch {
                    try {
                        val end = if (endTime.isNotBlank()) "$date" + "T$endTime:00.000Z" else ""
                        val request = CalendarEventRequest(
                            title = title.trim(),
                            startAt = "$date" + "T$startTime:00.000Z",
                            endAt = end,
                            type = type,
                            subjectId = subjectId,
                        )
                        if (event.id == 0) {
                            Api.client.createCalendarEvent(request)
                        } else {
                            Api.client.updateCalendarEvent(id = event.id, request = request)
                        }
                        onDone()
                    } catch (e: Exception) {
                        error = e.message ?: "No se pudo guardar el evento"
                    } finally {
                        saving = false
                    }
                }
            }
            if (event.id != 0) {
                Spacer(Modifier.height(10.dp))
                if (!confirmDelete) {
                    Text(
                        text = "Eliminar ${if (type == "EXAM") "examen" else "evento"}",
                        color = StudyHubColors.DangerLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudyHubColors.DangerLight.copy(alpha = 0.10f))
                            .clickable { confirmDelete = true }
                            .padding(vertical = 12.dp),
                    )
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Cancelar",
                            color = StudyHubColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudyHubColors.Surface)
                                .clickable { confirmDelete = false }
                                .padding(vertical = 12.dp),
                        )
                        Text(
                            text = if (deleting) "Eliminando…" else "Sí, eliminar",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudyHubColors.DangerLight)
                                .clickable(enabled = !deleting) {
                                    deleting = true
                                    scope.launch {
                                        try {
                                            Api.client.deleteCalendarEvent(event.id)
                                            onDone()
                                        } catch (e: Exception) {
                                            error = e.message ?: "No se pudo eliminar el evento"
                                            confirmDelete = false
                                        } finally {
                                            deleting = false
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Enfoque / Temporizador de estudio ──────────────────────────────────

private data class FocusTechnique(val id: String, val label: String, val minutes: Int, val xp: Int)

private val FOCUS_TECHNIQUES = listOf(
    FocusTechnique("POMODORO_25_5", "25 min", 25, 10),
    FocusTechnique("POMODORO_50_10", "50 min", 50, 25),
    FocusTechnique("DEEP_BLOCK_90", "90 min", 90, 50),
)

private data class FocusPeriod(val id: String, val label: String)

private val FOCUS_PERIODS = listOf(
    FocusPeriod("day", "Hoy"),
    FocusPeriod("week", "Semana"),
    FocusPeriod("month", "Mes"),
    FocusPeriod("all", "Todo"),
)

private const val SESSIONS_PAGE_SIZE = 20

@Composable
private fun FocusScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf(FOCUS_TECHNIQUES[0]) }
    var remaining by remember { mutableStateOf(FOCUS_TECHNIQUES[0].minutes * 60) }
    var running by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf<StudyTimerStats?>(null) }
    var sessions by remember { mutableStateOf<List<StudySession>>(emptyList()) }
    var period by remember { mutableStateOf("week") }
    var totalSessions by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var loadingMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var focusSubjectId by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        subjects = runCatching { Api.client.subjects() }.getOrDefault(emptyList())
    }

    suspend fun load(keepScroll: Boolean = false) {
        loading = true
        try {
            val result = Api.client.studyTimerSessions(period = period, page = 1, limit = SESSIONS_PAGE_SIZE)
            sessions = if (keepScroll) sessions + result.sessions else result.sessions
            totalSessions = result.total
            page = 1
            error = null
        } catch (e: Exception) {
            if (stats == null) error = e.message ?: "No se pudieron cargar tus sesiones"
        } finally {
            loading = false
        }
    }

    suspend fun loadMore() {
        if (loadingMore || sessions.size >= totalSessions) return
        loadingMore = true
        try {
            val next = page + 1
            val result = Api.client.studyTimerSessions(period = period, page = next, limit = SESSIONS_PAGE_SIZE)
            sessions = sessions + result.sessions
            page = next
        } catch (_: Exception) {
            // Silencioso: el botón queda disponible para reintentar
        } finally {
            loadingMore = false
        }
    }

    LaunchedEffect(Unit) { load() }
    // Al cambiar de periodo se recarga, pero se mantiene la lista anterior visible
    // (loading solo muestra el spinner cuando aún no hay nada que mostrar).
    LaunchedEffect(period) { load() }

    // Tick de 1 segundo
    LaunchedEffect(running, remaining) {
        if (running && remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }

    // Al llegar a 0 → guardar la sesión y sumar XP
    LaunchedEffect(remaining) {
        if (running && remaining == 0) {
            running = false
            scope.launch {
                try {
                    val session = Api.client.saveStudySession(selected.minutes, selected.id, focusSubjectId)
                    message = "Sesión completada: +${session.xpEarned} XP"
                } catch (e: Exception) {
                    message = e.message ?: "Sesión completada"
                }
                load()
            }
        }
    }

    fun select(tech: FocusTechnique) {
        selected = tech
        remaining = tech.minutes * 60
        running = false
        message = null
    }

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar("Enfoque", "Temporizador de estudio", onBack)
        Box(Modifier.weight(1f)) {
            when {
                loading && stats == null -> LoadingState()
                error != null && stats == null -> ErrorState(error.orEmpty())
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(Icons.Outlined.Timer, StudyHubColors.SecondaryLight, "${stats?.totalHours ?: "0"} h", "Estudio esta semana", Modifier.weight(1f))
                        StatCard(Icons.Outlined.History, StudyHubColors.InfoLight, "$totalSessions", "Sesiones", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(18.dp))

                    GlassCard(radius = 20.dp, contentPadding = 18.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FOCUS_TECHNIQUES.forEach { tech ->
                                    val activeTech = tech.id == selected.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (activeTech) StudyHubColors.Primary.copy(alpha = 0.22f) else StudyHubColors.Surface)
                                            .border(1.dp, if (activeTech) StudyHubColors.PrimaryLight else StudyHubColors.Border, RoundedCornerShape(12.dp))
                                            .clickable { select(tech) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    ) {
                                        Text(
                                            tech.label,
                                            color = if (activeTech) StudyHubColors.PrimaryLight else StudyHubColors.TextSecondary,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                            if (subjects.isNotEmpty()) {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Materia de la sesión",
                                    color = StudyHubColors.TextTertiary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    val noneSelected = focusSubjectId == null
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (noneSelected) StudyHubColors.Primary.copy(alpha = 0.22f) else StudyHubColors.Surface)
                                            .border(1.dp, if (noneSelected) StudyHubColors.PrimaryLight else StudyHubColors.Border, RoundedCornerShape(12.dp))
                                            .clickable { focusSubjectId = null }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    ) {
                                        Text(
                                            "Sin materia",
                                            color = if (noneSelected) StudyHubColors.PrimaryLight else StudyHubColors.TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    subjects.forEach { s ->
                                        val active = focusSubjectId == s.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (active) StudyHubColors.Primary.copy(alpha = 0.22f) else StudyHubColors.Surface)
                                                .border(1.dp, if (active) StudyHubColors.PrimaryLight else StudyHubColors.Border, RoundedCornerShape(12.dp))
                                                .clickable { focusSubjectId = s.id }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                        ) {
                                            Text(
                                                s.nombre,
                                                color = if (active) StudyHubColors.PrimaryLight else StudyHubColors.TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = "${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}",
                                color = StudyHubColors.TextPrimary,
                                fontSize = 54.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "+${selected.xp} XP al completar",
                                color = StudyHubColors.TextTertiary,
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.height(16.dp))
                            PrimaryButton(
                                text = when {
                                    running -> "Pausar"
                                    remaining < selected.minutes * 60 -> "Reanudar"
                                    else -> "Iniciar"
                                },
                                icon = if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            ) {
                                if (running) {
                                    running = false
                                } else {
                                    message = null
                                    running = true
                                }
                            }
                            if (remaining < selected.minutes * 60 && !running) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Reiniciar",
                                    color = StudyHubColors.DangerLight,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { select(selected) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                            message?.let {
                                Spacer(Modifier.height(12.dp))
                                Text(it, color = StudyHubColors.SecondaryLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))

                    SectionTitle("Historial", icon = Icons.Outlined.History, iconTint = StudyHubColors.InfoLight)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FOCUS_PERIODS.forEach { p ->
                            val active = p.id == period
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) StudyHubColors.InfoLight.copy(alpha = 0.22f) else StudyHubColors.Surface)
                                    .border(1.dp, if (active) StudyHubColors.InfoLight else StudyHubColors.Border, RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (p.id != period) period = p.id
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    p.label,
                                    color = if (active) StudyHubColors.InfoLight else StudyHubColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (sessions.isEmpty() && loading) {
                        LoadingState()
                    } else if (sessions.isEmpty()) {
                        EmptyState("Aún no has completado sesiones en este periodo. ¡Empieza tu primer bloque de enfoque!")
                    } else {
                        // Indicador sutil de que se está actualizando el periodo
                        if (loading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                color = StudyHubColors.InfoLight,
                                trackColor = StudyHubColors.SurfaceLight,
                            )
                        }
                        sessions.forEach { s ->
                            GlassCard(modifier = Modifier.padding(bottom = 8.dp), radius = 16.dp, contentPadding = 14.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LeadingIcon(Icons.Outlined.Timer, StudyHubColors.SecondaryLight)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(techniqueLabel(s.technique), color = StudyHubColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        val subjectName = s.subjectId?.let { id -> subjects.firstOrNull { it.id == id }?.nombre }
                                        Text(
                                            buildString {
                                                append("${s.durationMinutes} min · ${formatDate(s.completedAt)}")
                                                if (!subjectName.isNullOrBlank()) append(" · $subjectName")
                                            },
                                            color = StudyHubColors.TextTertiary,
                                            fontSize = 11.5.sp,
                                        )
                                    }
                                    Badge("+${s.xpEarned} XP", StudyHubColors.AccentLight, small = true)
                                }
                            }
                        }
                        if (sessions.size < totalSessions) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (loadingMore) "Cargando…" else "Cargar más (${sessions.size}/$totalSessions)",
                                color = StudyHubColors.PrimaryLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudyHubColors.Surface)
                                    .clickable(enabled = !loadingMore) { scope.launch { loadMore() } }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

private fun modalidadLabel(value: String?): String = when (value) {
    "ON_SITE" -> "Presencial"
    "REMOTE" -> "Remoto"
    "HYBRID" -> "Híbrido"
    else -> value?.ifBlank { null } ?: "No definida"
}

private fun techniqueLabel(tech: String): String = when (tech) {
    "POMODORO_25_5" -> "Pomodoro 25/5"
    "POMODORO_50_10" -> "Pomodoro 50/10"
    "DEEP_BLOCK_90" -> "Bloque profundo 90 min"
    else -> tech.replace("_", " ")
}

// ─── Perfil académico ───────────────────────────────────────────────────

@Composable
private fun AcademicScreen(onBack: () -> Unit) {
    var profile by remember { mutableStateOf<ProfileAcademic?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var universidad by remember { mutableStateOf("") }
    var carrera by remember { mutableStateOf("") }
    var facultad by remember { mutableStateOf("") }
    var semestre by remember { mutableStateOf("") }
    var promedio by remember { mutableStateOf("") }
    var modalidad by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val demo = remember { TokenStore.load() == null }

    LaunchedEffect(Unit) {
        if (demo) {
            profile = DemoData.academic
            error = null
            loading = false
            return@LaunchedEffect
        }
        try {
            profile = Api.client.profileAcademic()
            error = null
        } catch (e: Exception) {
            // El backend responde 404 {"message":"Perfil académico no encontrado"}
            // cuando aún no existe: se muestra el formulario para crearlo.
            if (e.message?.contains("no encontrado", ignoreCase = true) == true) {
                profile = null
                error = null
            } else {
                error = e.message ?: "No se pudo cargar tu perfil académico"
            }
        } finally {
            loading = false
        }
    }

    fun startEdit() {
        val p = profile ?: ProfileAcademic()
        universidad = p.universidad
        carrera = p.carrera
        facultad = p.facultad
        semestre = if (p.semestreActual > 0) p.semestreActual.toString() else ""
        promedio = if (p.promedio > 0) p.promedio.toString() else ""
        modalidad = p.modalidad
        editing = true
    }

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar("Perfil Académico", "Tu información de estudio", onBack)
        Box(Modifier.weight(1f)) {
            when {
                loading -> LoadingState()
                error != null && profile == null -> ErrorState(error.orEmpty())
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                ) {
                    if (!editing && profile == null) {
                        EmptyState("Aún no has creado tu perfil académico. Agrégale tu carrera, universidad y promedio.")
                        Spacer(Modifier.height(14.dp))
                        PrimaryButton(
                            text = "Crear perfil académico",
                            icon = Icons.Outlined.School,
                            onClick = ::startEdit,
                        )
                    } else if (!editing) {
                        val p = profile
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconTile(Icons.Outlined.School, SpaceTarget.ACADEMIC.gradient, size = 46.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p?.carrera?.ifBlank { "Aún sin carrera" } ?: "Aún sin carrera", color = StudyHubColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                    if (!(p?.universidad?.isBlank() ?: true)) {
                                        Text(p!!.universidad, color = StudyHubColors.TextTertiary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        GlassCard {
                            if (p?.universidad?.isNotBlank() == true) {
                                InfoRow(Icons.Outlined.MenuBook, "Universidad", p.universidad)
                                Spacer(Modifier.height(10.dp))
                            }
                            if (p?.facultad?.isNotBlank() == true) {
                                InfoRow(Icons.Outlined.AccountBalance, "Facultad", p.facultad)
                                Spacer(Modifier.height(10.dp))
                            }
                            if ((p?.semestreActual ?: 0) > 0) {
                                InfoRow(Icons.Outlined.WorkspacePremium, "Semestre", "${p!!.semestreActual}")
                                Spacer(Modifier.height(10.dp))
                            }
                            if ((p?.promedio ?: 0.0) > 0) {
                                InfoRow(Icons.Outlined.Star, "Promedio", p!!.promedio.toString())
                                Spacer(Modifier.height(10.dp))
                            }
                            InfoRow(Icons.Outlined.School, "Modalidad", modalidadLabel(p?.modalidad))
                        }
                        Spacer(Modifier.height(14.dp))
                        PrimaryButton(
                            text = "Editar perfil académico",
                            icon = Icons.Outlined.Edit,
                            onClick = ::startEdit,
                        )
                    } else {
                        GlassCard {
                            Text("Editar perfil académico", color = StudyHubColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(12.dp))
                            GlassTextField(value = universidad, onValueChange = { universidad = it }, placeholder = "Universidad")
                            Spacer(Modifier.height(10.dp))
                            GlassTextField(value = carrera, onValueChange = { carrera = it }, placeholder = "Carrera")
                            Spacer(Modifier.height(10.dp))
                            GlassTextField(value = facultad, onValueChange = { facultad = it }, placeholder = "Facultad")
                            Spacer(Modifier.height(10.dp))
                            GlassTextField(value = semestre, onValueChange = { semestre = it.filter(Char::isDigit) }, placeholder = "Semestre actual")
                            Spacer(Modifier.height(10.dp))
                            GlassTextField(value = promedio, onValueChange = { promedio = it.filter { c -> c.isDigit() || c == '.' } }, placeholder = "Promedio (0-100)")
                            Spacer(Modifier.height(10.dp))
                            Text("Modalidad", color = StudyHubColors.TextSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("ON_SITE" to "Presencial", "REMOTE" to "Remoto", "HYBRID" to "Híbrido").forEach { (value, label) ->
                                    val activeTech = modalidad == value
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (activeTech) StudyHubColors.Primary.copy(alpha = 0.22f) else StudyHubColors.Surface)
                                            .border(1.dp, if (activeTech) StudyHubColors.PrimaryLight else StudyHubColors.Border, RoundedCornerShape(12.dp))
                                            .clickable { modalidad = value }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    ) {
                                        Text(
                                            label,
                                            color = if (activeTech) StudyHubColors.PrimaryLight else StudyHubColors.TextSecondary,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            PrimaryButton(
                                text = "Guardar",
                                icon = Icons.Outlined.CheckCircle,
                                loading = saving,
                            ) {
                                saving = true
                                scope.launch {
                                    try {
                                        val modalidadFinal = if (modalidad.isBlank()) "ON_SITE" else modalidad
                                        val p = if (profile == null) {
                                            Api.client.createAcademicProfile(
                                                universidad = universidad.trim(),
                                                carrera = carrera.trim(),
                                                facultad = facultad.trim(),
                                                semestreActual = semestre.toIntOrNull() ?: 0,
                                                promedio = promedio.toDoubleOrNull() ?: 0.0,
                                                modalidad = modalidadFinal,
                                            )
                                        } else {
                                            Api.client.updateAcademicProfile(
                                                universidad = universidad.trim(),
                                                carrera = carrera.trim(),
                                                facultad = facultad.trim(),
                                                semestreActual = semestre.toIntOrNull() ?: 0,
                                                promedio = promedio.toDoubleOrNull() ?: 0.0,
                                                modalidad = modalidadFinal,
                                            )
                                        }
                                        profile = p
                                        error = null
                                        editing = false
                                    } catch (e: Exception) {
                                        error = e.message ?: "No se pudo guardar"
                                    } finally {
                                        saving = false
                                    }
                                }
                            }
                            if (error != null) {
                                Spacer(Modifier.height(10.dp))
                                Text(error.orEmpty(), color = StudyHubColors.DangerLight, fontSize = 12.5.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Cancelar",
                            color = StudyHubColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .clickable { editing = false }
                                .padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

// ─── Riesgo académico ───────────────────────────────────────────────────

@Composable
private fun RiskScreen(onBack: () -> Unit) {
    var risk by remember { mutableStateOf<RiskAnalysis?>(null) }
    var history by remember { mutableStateOf<List<RiskAnalysis>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            risk = Api.client.risk()
            history = Api.client.riskHistory()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el análisis de riesgo"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar("Riesgo Académico", "Análisis basado en tu actividad", onBack)
        Box(Modifier.weight(1f)) {
            when {
                loading -> LoadingState()
                error != null -> ErrorState(error.orEmpty())
                // Sin análisis previo el backend responde 200 con cuerpo vacío → null
                risk == null -> EmptyState("Aún no tienes un análisis de riesgo. Usa la app para que el sistema calcule tu nivel de riesgo académico.")
                else -> risk?.let { r ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp),
                    ) {
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ScoreRing(r.score, "riesgo", color = riskColor(r.level), size = 104.dp)
                                Spacer(Modifier.width(18.dp))
                                Column {
                                    Text("Nivel de riesgo", color = StudyHubColors.TextSecondary, fontSize = 12.sp)
                                    Text(r.level, color = riskColor(r.level), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                    Spacer(Modifier.height(8.dp))
                                    Badge("Actualizado ${formatDate(r.createdAt)}", StudyHubColors.TextTertiary, small = true)
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))

                        r.reasons?.let { reasons ->
                            reasons.factors?.let { factors ->
                                SectionTitle("Factores", icon = Icons.Outlined.Security, iconTint = StudyHubColors.DangerLight)
                                Spacer(Modifier.height(10.dp))
                                GlassCard {
                                    factors.knowledgeGaps?.let {
                                        FactorRow("Brechas de conocimiento", it.score, it.max, StudyHubColors.DangerLight)
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    factors.overdueTasks?.let {
                                        FactorRow("Tareas atrasadas", it.score, it.max, StudyHubColors.DangerLight)
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    factors.confidenceIA?.let {
                                        FactorRow("Confianza IA", it.score, it.max, StudyHubColors.InfoLight)
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    factors.roadmaps?.let {
                                        FactorRow("Roadmaps activos", it.score, it.max, StudyHubColors.PrimaryLight)
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    factors.engagement?.let {
                                        FactorRow("Participación general", it.score, it.max, StudyHubColors.SecondaryLight)
                                    }
                                }
                            }
                            if (reasons.summary.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    reasons.summary.forEach { summary ->
                                        Badge(summary, StudyHubColors.DangerLight, small = true)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))

                        if (history.size > 1) {
                            SectionTitle("Evolución", icon = Icons.Outlined.TrackChanges, iconTint = StudyHubColors.PrimaryLight)
                            Spacer(Modifier.height(12.dp))
                            GlassCard {
                                Row(
                                    Modifier.fillMaxWidth().height(120.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    history.takeLast(6).forEach { h ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${h.score}", color = StudyHubColors.TextSecondary, fontSize = 10.sp)
                                            Box(
                                                Modifier
                                                    .width(26.dp)
                                                    .height((h.score.coerceIn(0, 100) * 1.0f).dp * 0.9f)
                                                    .background(riskColor(h.level)),
                                            )
                                            Text(
                                                formatDate(h.createdAt).split(" ").firstOrNull() ?: "",
                                                color = StudyHubColors.TextTertiary,
                                                fontSize = 8.5.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FactorRow(label: String, value: Int, max: Int, color: Color) {
    val progress = if (max > 0) value.toFloat() / max else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = StudyHubColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        GlassProgress(progress = progress, color = color, modifier = Modifier.width(120.dp))
        Spacer(Modifier.width(10.dp))
        Text("$value/$max", color = StudyHubColors.TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── Logros / Gamificación ──────────────────────────────────────────────

@Composable
private fun AchievementsScreen(onBack: () -> Unit) {
    var progress by remember { mutableStateOf<org.studyhub.project.net.GamificationProgress?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            progress = Api.client.gamification()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar los logros"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar("Logros", "Gamificación de tu aprendizaje", onBack)
        Box(Modifier.weight(1f)) {
            when {
                loading -> LoadingState()
                error != null -> ErrorState(error.orEmpty())
                else -> progress?.let { g ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp),
                    ) {
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LeadingIcon(Icons.Outlined.WorkspacePremium, StudyHubColors.AccentLight, size = 48.dp, iconSize = 24.dp)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Nivel ${g.level}", color = StudyHubColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("${g.xp} XP de ${g.xpForNextLevel}", color = StudyHubColors.TextTertiary, fontSize = 12.5.sp)
                                }
                                Badge("🔥 ${g.streak}", StudyHubColors.AccentLight)
                            }
                            Spacer(Modifier.height(12.dp))
                            GlassProgress(
                                progress = if (g.xpForNextLevel > 0) g.xp.toFloat() / g.xpForNextLevel else 0f,
                                color = StudyHubColors.Accent,
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        SectionTitle("Logros desbloqueados", icon = Icons.Outlined.EmojiEvents, iconTint = StudyHubColors.AccentLight)
                        Spacer(Modifier.height(10.dp))
                        if (g.achievementsList.isEmpty()) {
                            EmptyState("Aún no tienes logros. Completa tareas, estudia en racha y crea tu CV para desbloquearlos.")
                        }
                        g.achievementsList.forEach { a ->
                            GlassCard(modifier = Modifier.padding(bottom = 8.dp), radius = 16.dp, contentPadding = 14.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LeadingIcon(achievementIcon(a.nombre), StudyHubColors.AccentLight)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(a.nombre.ifBlank { a.code }, color = StudyHubColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Desbloqueado ${formatDate(a.unlockedAt)}", color = StudyHubColors.TextTertiary, fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

private fun achievementIcon(name: String): ImageVector = when {
    name.contains("racha", ignoreCase = true) || name.contains("streak", ignoreCase = true) -> Icons.Outlined.LocalFireDepartment
    name.contains("tarea", ignoreCase = true) || name.contains("task", ignoreCase = true) -> Icons.Outlined.CheckCircle
    name.contains("cv", ignoreCase = true) || name.contains("hoja de vida", ignoreCase = true) -> Icons.Outlined.Description
    name.contains("quiz", ignoreCase = true) -> Icons.Outlined.Quiz
    else -> Icons.Outlined.EmojiEvents
}

// ─── Quizzes ────────────────────────────────────────────────────────────

@Composable
internal fun QuizzesScreen(onBack: (() -> Unit)? = null) {
    var quizzes by remember { mutableStateOf<List<AiResource>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var generating by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf<AiResource?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        try {
            // Solo los recursos tipo QUIZ son jugables en la app
            quizzes = Api.client.aiResources().filter { it.type.equals("QUIZ", ignoreCase = true) }
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar los quizzes"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(reload) { load() }

    // Back del sistema cierra el quiz en vez de la app
    PlatformBackHandler(enabled = playing != null) { playing = null }

    val current = playing
    if (current != null) {
        QuizPlayScreen(
            resourceId = current.id,
            title = current.title,
            onBack = { playing = null },
            onCompleted = {
                playing = null
                reload++
            },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        if (onBack != null) {
            FullScreenBackBar("Quizzes", "Generados por IA según tus brechas", onBack)
        }
        Box(Modifier.weight(1f)) {
            when {
                loading -> LoadingState()
                error != null -> ErrorState(error.orEmpty()) {
                    loading = true
                    error = null
                    reload++
                }
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                ) {
                    PrimaryButton(
                        text = "Generar quiz con IA",
                        icon = Icons.Outlined.Quiz,
                        loading = generating,
                    ) {
                        generating = true
                        scope.launch {
                            try {
                                Api.client.generateQuiz(topic = "", difficulty = "INTERMEDIATE", count = 5)
                                reload++
                            } catch (e: Exception) {
                                error = e.message ?: "No se pudo generar el quiz"
                            } finally {
                                generating = false
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    SectionTitle("Mis quizzes", icon = Icons.Outlined.Quiz, iconTint = StudyHubColors.PrimaryLight)
                    Spacer(Modifier.height(10.dp))
                    if (quizzes.isEmpty()) {
                        EmptyState("Aún no tienes quizzes generados. Toca el botón para crear uno con IA.")
                    }
                    quizzes.forEach { q ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .then(
                                    if (!q.completed) {
                                        Modifier.clickable { playing = q }
                                    } else {
                                        Modifier
                                    },
                                ),
                            radius = 16.dp,
                            contentPadding = 14.dp,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LeadingIcon(
                                    if (q.completed) Icons.Outlined.CheckCircle else Icons.Outlined.Quiz,
                                    if (q.completed) StudyHubColors.SecondaryLight else StudyHubColors.PrimaryLight,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(q.title.ifBlank { "Quiz de ${q.subject}" }, color = StudyHubColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (q.subject.isNotBlank()) {
                                            Badge(q.subject, StudyHubColors.InfoLight, small = true)
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        if (q.difficulty.isNotBlank()) {
                                            Badge(q.difficulty, difficultyColor(q.difficulty), small = true)
                                        }
                                    }
                                    if (!q.completed) {
                                        Spacer(Modifier.height(5.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "Toca para jugar",
                                                color = StudyHubColors.PrimaryLight,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            AppIcon(
                                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                                tint = StudyHubColors.PrimaryLight,
                                                size = 14.dp,
                                            )
                                        }
                                    }
                                }
                                if (q.completed && q.resultCorrect != null && q.resultTotal != null) {
                                    Badge("${q.resultCorrect}/${q.resultTotal}", StudyHubColors.SecondaryLight, small = true)
                                } else if (!q.completed) {
                                    Badge("Pendiente", StudyHubColors.AccentLight, small = true)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

// ─── Brechas de conocimiento ────────────────────────────────────────────

@Composable
private fun BreachesScreen(onBack: () -> Unit) {
    var gaps by remember { mutableStateOf<List<KnowledgeGap>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            gaps = Api.client.knowledgeGaps()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar las brechas"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar("Brechas de Conocimiento", "Detectadas por el tutor IA", onBack)
        Box(Modifier.weight(1f)) {
            when {
                loading -> LoadingState()
                error != null -> ErrorState(error.orEmpty())
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                ) {
                    if (gaps.isEmpty()) {
                        EmptyState("No hay brechas detectadas. ¡Excelente! Sigue así.")
                    }
                    gaps.forEach { gap ->
                        GlassCard(modifier = Modifier.padding(bottom = 10.dp), radius = 16.dp, contentPadding = 16.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(gap.topic.ifBlank { gap.subject }, color = StudyHubColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(8.dp))
                                        Badge(gap.subject, StudyHubColors.InfoLight, small = true)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    GlassProgress(
                                        progress = gap.confidence / 100f,
                                        color = confidenceColor(gap.confidence),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Confianza: ${gap.confidence}%",
                                        color = confidenceColor(gap.confidence),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

private fun confidenceColor(confidence: Int): Color = when {
    confidence < 50 -> StudyHubColors.DangerLight
    confidence < 80 -> StudyHubColors.AccentLight
    else -> StudyHubColors.SecondaryLight
}

// ─── Metas de aprendizaje ───────────────────────────────────────────────

@Composable
private fun GoalsScreen(onBack: () -> Unit) {
    var goals by remember { mutableStateOf<List<AiGoal>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        try {
            goals = Api.client.goals()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar las metas"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar("Metas de Aprendizaje", "IA · progreso y fechas objetivo", onBack)
        Box(Modifier.weight(1f)) {
            when {
                loading -> LoadingState()
                error != null -> ErrorState(error.orEmpty()) {
                    loading = true
                    error = null
                }
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                ) {
                    GlassCard {
                        Text("Nueva meta", color = StudyHubColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(10.dp))
                        GlassTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "Ej: Aprobar Cálculo I",
                        )
                        Spacer(Modifier.height(10.dp))
                        PrimaryButton(
                            text = "Crear meta",
                            icon = Icons.Outlined.TrackChanges,
                            enabled = title.isNotBlank() && !creating,
                            loading = creating,
                        ) {
                            creating = true
                            scope.launch {
                                try {
                                    Api.client.createGoal(title = title.trim(), description = "", targetDate = "")
                                    title = ""
                                    load()
                                } catch (e: Exception) {
                                    error = e.message ?: "No se pudo crear la meta"
                                } finally {
                                    creating = false
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    SectionTitle("Mis metas", icon = Icons.Outlined.TrackChanges, iconTint = StudyHubColors.PrimaryLight)
                    Spacer(Modifier.height(10.dp))
                    if (goals.isEmpty()) {
                        EmptyState("Aún no tienes metas. Crea una para empezar.")
                    }
                    goals.forEach { goal ->
                        GlassCard(modifier = Modifier.padding(bottom = 8.dp), radius = 16.dp, contentPadding = 16.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(goal.title, color = StudyHubColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    if (goal.subjectLabel.isNotBlank()) {
                                        Text(goal.subjectLabel, color = StudyHubColors.TextTertiary, fontSize = 11.5.sp)
                                    }
                                }
                                Badge(
                                    if (goal.status.equals("COMPLETADA", true)) "Completada" else "Activa",
                                    if (goal.status.equals("COMPLETADA", true)) StudyHubColors.SecondaryLight else StudyHubColors.AccentLight,
                                    small = true,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            GlassProgress(progress = goal.progress / 100f, color = StudyHubColors.SecondaryLight)
                            if (goal.targetDate.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Objetivo: ${formatDate(goal.targetDate)}", color = StudyHubColors.TextTertiary, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

internal fun difficultyColor(difficulty: String): Color = when (difficulty.uppercase()) {
    "ADVANCED", "AVANZADO" -> StudyHubColors.DangerLight
    "INTERMEDIATE", "INTERMEDIO" -> StudyHubColors.AccentLight
    else -> StudyHubColors.SecondaryLight
}
