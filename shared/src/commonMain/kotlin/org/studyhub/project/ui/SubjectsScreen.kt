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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.studyhub.project.StudyHubColors
import org.studyhub.project.net.Api
import org.studyhub.project.net.DashboardSummary
import org.studyhub.project.net.Note
import org.studyhub.project.net.ProfileAcademic
import org.studyhub.project.net.Schedule
import org.studyhub.project.net.Subject
import org.studyhub.project.net.SubjectDetail
import org.studyhub.project.net.SubjectTask
import org.studyhub.project.net.TokenStore
import org.studyhub.project.net.dayName
import org.studyhub.project.ui.icons.AppIcon

// ─── Lista de materias ──────────────────────────────────────────────────

@Composable
fun SubjectsScreen(
    onOpenDetail: (Int, String, String) -> Unit,
    unread: Int,
    onBell: () -> Unit,
    userName: String,
    onOpenSpace: (SpaceTarget) -> Unit,
    userPhoto: String? = null,
) {
    val demo = remember { TokenStore.load() == null }
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var details by remember { mutableStateOf<Map<Int, SubjectDetail>>(emptyMap()) }
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var academic by remember { mutableStateOf<ProfileAcademic?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(reload) {
        loading = true
        if (demo) {
            subjects = DemoData.subjects
            details = DemoData.subjectDetails
            summary = DemoData.dashboard
            academic = DemoData.academic
            error = null
            loading = false
            return@LaunchedEffect
        }
        try {
            val loaded = Api.client.subjects()
            subjects = loaded
            summary = runCatching { Api.client.dashboard() }.getOrNull()
            academic = runCatching { Api.client.profileAcademic() }.getOrNull()
            details = coroutineScope {
                val pairs = loaded.map { s ->
                    async { s.id to runCatching { Api.client.subjectDetail(s.id) }.getOrNull() }
                }.map { it.await() }
                pairs.mapNotNull { (id, detail) -> detail?.let { id to it } }.toMap()
            }
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar las materias"
        } finally {
            loading = false
        }
    }

    // Back del sistema cierra el formulario de materia en vez de la app
    PlatformBackHandler(enabled = creating) { creating = false }

    if (creating) {
        SubjectFormScreen(
            onBack = { creating = false },
            onCreated = {
                creating = false
                reload++
            },
        )
        return
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
                val stats = summary?.stats
                val completed = stats?.completedTasks ?: details.values.sumOf { d -> d.tasks.count { it.completed } }
                val pending = stats?.pendingTasks ?: details.values.sumOf { d -> d.tasks.count { !it.completed } }
                val total = completed + pending
                val overall = if (total > 0) completed.toFloat() / total else 0f
                val completion = summary?.completionRate?.roundToInt()?.takeIf { it > 0 } ?: (overall * 100).roundToInt()
                val avg5: Double? = academic?.promedio?.takeIf { it > 0 }?.let { if (it > 5) it / 20 else it }
                    ?: (overall * 5).toDouble().takeIf { total > 0 }
                val semester = academic?.semestreActual?.takeIf { it > 0 }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    StudyHubTopBar(
                        eyebrow = "Tu espacio de aprendizaje",
                        title = "Mis materias",
                        unread = unread,
                        onBell = onBell,
                        userName = userName,
                        userPhoto = userPhoto ?: summary?.user?.foto,
                    )
                    Spacer(Modifier.height(18.dp))

                    // Tarjeta resumen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(StudyHubColors.Hero, StudyHubColors.HeroDark),
                                ),
                            )
                            .padding(20.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(150.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f)),
                        )
                        Column {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    text = semester?.let { "Semestre $it" } ?: "Tu semestre",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "${subjects.size} materias activas",
                                        color = Color.White,
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (-0.3).sp,
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Text(
                                        text = "Tu promedio general",
                                        color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 10.5.sp,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = avg5?.let { oneDecimal(it) } ?: "—",
                                            color = Color.White,
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 32.sp,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "/ 5.0",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(bottom = 5.dp),
                                        )
                                    }
                                }
                                HeroProgressRing(percent = completion, size = 82.dp)
                            }
                        }
                    }
                    Spacer(Modifier.height(22.dp))

                    SectionTitle(
                        text = "Resumen académico",
                        action = "Editar",
                        onAction = { onOpenSpace(SpaceTarget.ACADEMIC) },
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AcademicSummaryCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.CheckCircle,
                            tint = StudyHubColors.Primary,
                            value = "$completed",
                            label = "tareas completadas",
                        )
                        AcademicSummaryCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.AccessTime,
                            tint = StudyHubColors.Accent,
                            value = "$pending",
                            label = "por entregar",
                        )
                    }
                    Spacer(Modifier.height(22.dp))

                    SectionTitle(
                        text = "Tus materias",
                        action = "+ Añadir",
                        onAction = { creating = true },
                    )
                    Spacer(Modifier.height(10.dp))

                    if (subjects.isEmpty()) {
                        EmptyState("Aún no tienes materias registradas. Toca \"+ Añadir\" para crear la primera.")
                    } else {
                        subjects.sortedBy { it.nombre.lowercase() }.forEach { subject ->
                            val detail = details[subject.id]
                            val tasks = detail?.tasks.orEmpty()
                            val done = tasks.count { it.completed }
                            val metric = when {
                                tasks.isNotEmpty() -> oneDecimal(3.5 + 1.5 * (done.toDouble() / tasks.size))
                                subject.creditos > 0 -> "${subject.creditos} cr"
                                else -> "—"
                            }
                            SubjectRow(
                                subject = subject,
                                metric = metric,
                                onClick = { onOpenDetail(subject.id, subject.nombre, subject.color) },
                            )
                            Spacer(Modifier.height(9.dp))
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

@Composable
private fun AcademicSummaryCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    value: String,
    label: String,
) {
    GlassCard(modifier = modifier, radius = 18.dp, contentPadding = 14.dp) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(tint.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(icon, tint = tint, size = 17.dp)
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = value,
            color = StudyHubColors.TextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = StudyHubColors.TextSecondary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun SubjectRow(
    subject: Subject,
    metric: String,
    onClick: () -> Unit,
) {
    val color = parseColor(subject.color)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StudyHubColors.Surface)
            .border(1.dp, StudyHubColors.GlassBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = subject.nombre,
                color = StudyHubColors.TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subject.count?.let { "${it.tasks} tareas · ${it.notes} apuntes" }
                    ?: subject.codigo.ifBlank { "Materia" },
                color = StudyHubColors.TextTertiary,
                fontSize = 10.5.sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = metric,
            color = StudyHubColors.TextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(4.dp))
        AppIcon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            tint = StudyHubColors.TextTertiary,
            size = 17.dp,
        )
    }
}

private fun oneDecimal(value: Double): String {
    val rounded = (value * 10).roundToInt()
    return "${rounded / 10}.${rounded % 10}"
}

// ─── Detalle de materia ─────────────────────────────────────────────────

private enum class DetailTab { TASKS, SCHEDULE, NOTES }

private sealed interface DetailForm {
    data class TaskForm(val task: SubjectTask?) : DetailForm
    data class NoteForm(val note: Note?) : DetailForm
    data object ScheduleForm : DetailForm
}

@Composable
fun SubjectDetailScreen(
    subjectId: Int,
    subjectName: String,
    colorHex: String,
    onBack: () -> Unit,
) {
    var detail by remember { mutableStateOf<SubjectDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var tab by remember { mutableStateOf(DetailTab.TASKS) }
    var form by remember { mutableStateOf<DetailForm?>(null) }
    var deleting by remember { mutableStateOf<Any?>(null) }
    val scope = rememberCoroutineScope()
    val demo = remember { TokenStore.load() == null }

    LaunchedEffect(reload) {
        loading = true
        if (demo) {
            detail = DemoData.subjectDetails[subjectId]
            error = if (detail == null) "Materia no encontrada" else null
            loading = false
            return@LaunchedEffect
        }
        try {
            detail = Api.client.subjectDetail(subjectId)
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar la materia"
        } finally {
            loading = false
        }
    }

    fun toggleTask(taskId: Int) {
        scope.launch {
            runCatching { Api.client.toggleTask(subjectId, taskId) }
            reload++
        }
    }

    fun confirmDeleteTask(task: SubjectTask) {
        scope.launch {
            runCatching { Api.client.deleteTask(subjectId, task.id) }
            deleting = null
            reload++
        }
    }

    fun confirmDeleteNote(note: Note) {
        scope.launch {
            runCatching { Api.client.deleteNote(subjectId, note.id) }
            deleting = null
            reload++
        }
    }

    fun confirmDeleteSchedule(schedule: Schedule) {
        scope.launch {
            runCatching { Api.client.deleteSchedule(subjectId, schedule.id) }
            deleting = null
            reload++
        }
    }

    when (val f = form) {
        is DetailForm.TaskForm -> TaskFormScreen(
            subjectId = subjectId,
            task = f.task,
            onBack = { form = null },
            onDone = {
                form = null
                reload++
            },
        )
        is DetailForm.NoteForm -> NoteFormScreen(
            subjectId = subjectId,
            note = f.note,
            onBack = { form = null },
            onDone = {
                form = null
                reload++
            },
        )
        is DetailForm.ScheduleForm -> ScheduleFormScreen(
            subjectId = subjectId,
            onBack = { form = null },
            onDone = {
                form = null
                reload++
            },
        )
        null -> Column(Modifier.fillMaxSize()) {
            if (detail != null && error == null) {
                SubjectDetailHeader(subject = detail!!, accent = parseColor(colorHex), onBack = onBack)
                DetailTabs(current = tab, onSelect = { tab = it })
            } else {
                FullScreenBackBar(subjectName, "Detalle de la materia", onBack)
            }
            Box(Modifier.weight(1f)) {
                when {
                    loading -> LoadingState()
                    error != null -> ErrorState(error.orEmpty()) {
                        loading = true
                        error = null
                        reload++
                    }
                    else -> detail?.let { d ->
                        when (tab) {
                            DetailTab.TASKS -> TasksTab(
                                d,
                                onToggle = { toggleTask(it) },
                                onAdd = { form = DetailForm.TaskForm(null) },
                                onEdit = { form = DetailForm.TaskForm(it) },
                                onDelete = { deleting = it },
                            )
                            DetailTab.SCHEDULE -> ScheduleTab(
                                d,
                                accent = parseColor(colorHex),
                                onAdd = { form = DetailForm.ScheduleForm },
                                onDelete = { deleting = it },
                            )
                            DetailTab.NOTES -> NotesTab(
                                d,
                                onAdd = { form = DetailForm.NoteForm(null) },
                                onEdit = { form = DetailForm.NoteForm(it) },
                                onDelete = { deleting = it },
                                onPin = { note ->
                                    scope.launch {
                                        runCatching { Api.client.togglePinNote(subjectId, note.id) }
                                        reload++
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    val pendingDelete = deleting
    if (pendingDelete != null) {
        ConfirmDialog(
            title = when (pendingDelete) {
                is SubjectTask -> "¿Eliminar tarea?"
                is Note -> "¿Eliminar apunte?"
                else -> "¿Eliminar clase?"
            },
            message = "Esta acción no se puede deshacer.",
            onConfirm = {
                when (pendingDelete) {
                    is SubjectTask -> confirmDeleteTask(pendingDelete)
                    is Note -> confirmDeleteNote(pendingDelete)
                    is Schedule -> confirmDeleteSchedule(pendingDelete)
                }
            },
            onDismiss = { deleting = null },
        )
    }
}

// ─── Detalle: header con gradiente, stats y pestañas ────────────────────

@Composable
private fun SubjectDetailHeader(subject: SubjectDetail, accent: Color, onBack: () -> Unit) {
    val pending = subject.tasks.count { !it.completed }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent))),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(StudyHubColors.Surface)
                    .border(1.dp, StudyHubColors.Border, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(Icons.AutoMirrored.Outlined.ArrowBack, tint = StudyHubColors.TextPrimary, size = 20.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    subject.nombre,
                    color = StudyHubColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
                Text(
                    buildString {
                        append(subject.codigo.ifBlank { "SIN CÓDIGO" })
                        if (subject.creditos > 0) append(" · ${subject.creditos} cr")
                        if (subject.profesor.isNotBlank()) append(" · ${subject.profesor}")
                    },
                    color = StudyHubColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f)))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    subject.nombre.trim().firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailStat(modifier = Modifier.weight(1f), value = "${subject.tasks.size}", label = "Tareas")
            DetailStat(modifier = Modifier.weight(1f), value = "$pending", label = "Pendientes")
            DetailStat(modifier = Modifier.weight(1f), value = "${subject.schedules.size}", label = "Clases/sem")
            DetailStat(modifier = Modifier.weight(1f), value = "${subject.notes.size}", label = "Apuntes")
        }
    }
}

@Composable
private fun DetailStat(modifier: Modifier = Modifier, value: String, label: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StudyHubColors.Surface.copy(alpha = 0.85f))
            .border(1.dp, StudyHubColors.Border, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = StudyHubColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = StudyHubColors.TextTertiary, fontSize = 10.5.sp)
    }
}

@Composable
private fun DetailTabs(current: DetailTab, onSelect: (DetailTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StudyHubColors.Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DetailTab.entries.forEach { t ->
            val selected = t == current
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) StudyHubColors.Primary.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(t) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(
                    icon = when (t) {
                        DetailTab.TASKS -> Icons.Outlined.Assignment
                        DetailTab.SCHEDULE -> Icons.Outlined.CalendarMonth
                        DetailTab.NOTES -> Icons.Outlined.NoteAlt
                    },
                    tint = if (selected) StudyHubColors.PrimaryLight else StudyHubColors.TextTertiary,
                    size = 15.dp,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = when (t) {
                        DetailTab.TASKS -> "Tareas"
                        DetailTab.SCHEDULE -> "Horario"
                        DetailTab.NOTES -> "Apuntes"
                    },
                    color = if (selected) StudyHubColors.PrimaryLight else StudyHubColors.TextTertiary,
                    fontSize = 12.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun TasksTab(
    d: SubjectDetail,
    onToggle: (Int) -> Unit,
    onAdd: () -> Unit,
    onEdit: (SubjectTask) -> Unit,
    onDelete: (SubjectTask) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(6.dp))
        AddTabButton("Nueva tarea", onAdd)
        Spacer(Modifier.height(10.dp))
        if (d.tasks.isEmpty()) {
            EmptyState("No hay tareas en esta materia.")
        } else {
            val pending = d.tasks.filter { !it.completed }
            val completed = d.tasks.filter { it.completed }
            var showCompleted by remember { mutableStateOf(false) }
            if (pending.isEmpty()) {
                EmptyState("¡No quedan tareas pendientes!")
            } else {
                sortTasksByUrgency(pending).forEach { task ->
                    TaskCard(task = task, onToggle = onToggle, onEdit = { onEdit(task) }, onDelete = { onDelete(task) })
                }
            }
            if (completed.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCompleted = !showCompleted }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Completadas (${completed.size})",
                        color = StudyHubColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    AppIcon(
                        icon = if (showCompleted) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        tint = StudyHubColors.TextSecondary,
                        size = 20.dp,
                    )
                }
                if (showCompleted) {
                    completed.forEach { task ->
                        TaskCard(task = task, onToggle = onToggle, onEdit = { onEdit(task) }, onDelete = { onDelete(task) })
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AddTabButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StudyHubColors.Primary.copy(alpha = 0.12f))
            .border(1.dp, StudyHubColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(Icons.Outlined.Add, tint = StudyHubColors.PrimaryLight, size = 18.dp)
        Spacer(Modifier.width(6.dp))
        Text(text, color = StudyHubColors.PrimaryLight, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TaskCard(task: SubjectTask, onToggle: (Int) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val overdue = isOverdue(task) && !task.completed
    GlassCard(
        modifier = Modifier.padding(bottom = 8.dp),
        radius = 16.dp,
        contentPadding = 14.dp,
        background = if (overdue) parseColor("#1AF43F5E") else StudyHubColors.Glass,
        borderColor = if (overdue) parseColor("#FF6B7F") else StudyHubColors.GlassBorder,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            urgencyBarColor(task)?.let { barColor ->
                Box(
                    Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor),
                )
                Spacer(Modifier.width(12.dp))
            }
            AppIcon(
                icon = if (task.completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                tint = when {
                    task.completed -> StudyHubColors.SecondaryLight
                    overdue -> StudyHubColors.DangerLight
                    else -> StudyHubColors.TextTertiary
                },
                size = 22.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onToggle(task.id) }
                    .padding(2.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = when {
                        task.completed -> StudyHubColors.TextTertiary
                        overdue -> StudyHubColors.DangerLight
                        else -> StudyHubColors.TextPrimary
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                )
                if (task.dueDate.isNotBlank()) {
                    Text(
                        formatDate(task.dueDate),
                        color = if (overdue) StudyHubColors.DangerLight else StudyHubColors.TextTertiary,
                        fontSize = 11.5.sp,
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            DaysLeftBadge(task.dueDate)
            Spacer(Modifier.width(2.dp))
            Column {
                AppIcon(
                    icon = Icons.Outlined.Edit,
                    tint = StudyHubColors.TextTertiary,
                    size = 17.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onEdit)
                        .padding(4.dp),
                )
                Spacer(Modifier.height(2.dp))
                AppIcon(
                    icon = Icons.Outlined.Delete,
                    tint = StudyHubColors.DangerLight,
                    size = 17.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDelete)
                        .padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun ScheduleTab(d: SubjectDetail, accent: Color, onAdd: () -> Unit, onDelete: (Schedule) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(6.dp))
        AddTabButton("Nueva clase", onAdd)
        Spacer(Modifier.height(10.dp))
        if (d.schedules.isEmpty()) {
            EmptyState("No hay clases programadas para esta materia.")
        }
        d.schedules.forEach { s ->
            GlassCard(modifier = Modifier.padding(bottom = 8.dp), radius = 16.dp, contentPadding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dayName(s.dayOfWeek), color = StudyHubColors.TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${s.startTime} - ${s.endTime}", color = StudyHubColors.TextSecondary, fontSize = 12.5.sp)
                    Spacer(Modifier.width(8.dp))
                    Badge(s.classroom.ifBlank { d.salon }, accent, small = true)
                    Spacer(Modifier.width(2.dp))
                    AppIcon(
                        icon = Icons.Outlined.Delete,
                        tint = StudyHubColors.DangerLight,
                        size = 17.dp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onDelete(s) }
                            .padding(4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun NotesTab(
    d: SubjectDetail,
    onAdd: () -> Unit,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onPin: (Note) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(6.dp))
        AddTabButton("Nuevo apunte", onAdd)
        Spacer(Modifier.height(10.dp))
        if (d.notes.isEmpty()) {
            EmptyState("Aún no hay apuntes en esta materia.")
        }
        d.notes.sortedByDescending { it.pinned }.forEach { note ->
            GlassCard(
                modifier = Modifier.padding(bottom = 8.dp),
                radius = 16.dp,
                contentPadding = 14.dp,
                borderColor = if (note.pinned) StudyHubColors.Accent.copy(alpha = 0.55f) else StudyHubColors.GlassBorder,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        note.title,
                        color = StudyHubColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    AppIcon(
                        icon = if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        tint = if (note.pinned) StudyHubColors.Accent else StudyHubColors.TextTertiary,
                        size = 16.dp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onPin(note) }
                            .padding(4.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    AppIcon(
                        icon = Icons.Outlined.Edit,
                        tint = StudyHubColors.TextTertiary,
                        size = 16.dp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onEdit(note) }
                            .padding(4.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    AppIcon(
                        icon = Icons.Outlined.Delete,
                        tint = StudyHubColors.DangerLight,
                        size = 16.dp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onDelete(note) }
                            .padding(4.dp),
                    )
                }
                if (note.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        note.content,
                        color = StudyHubColors.TextSecondary,
                        fontSize = 12.5.sp,
                        maxLines = 3,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudyHubColors.Surface,
        titleContentColor = StudyHubColors.TextPrimary,
        textContentColor = StudyHubColors.TextSecondary,
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = { Text(message, fontSize = 13.5.sp) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text("Eliminar", color = StudyHubColors.DangerLight, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancelar", color = StudyHubColors.TextSecondary)
            }
        },
    )
}

// ─── Orden por urgencia ─────────────────────────────────────────────────

/** True si la tarea está atrasada: el backend la marca OVERDUE o su fecha límite ya pasó. */
fun isOverdue(task: SubjectTask): Boolean =
    task.status == "OVERDUE" || (daysUntilDue(task.dueDate) ?: 0) < 0

/** Color de la barra de urgencia: rojo (atrasada) → ámbar (hoy/mañana) → azul → verde (holgura). Sin fecha o completada: sin barra. */
fun urgencyBarColor(task: SubjectTask): Color? {
    if (task.completed) return null
    if (isOverdue(task)) return StudyHubColors.Danger
    val days = daysUntilDue(task.dueDate) ?: return null
    return when {
        days == 0 -> StudyHubColors.AccentDark
        days == 1 -> StudyHubColors.Accent
        days <= 3 -> StudyHubColors.AccentLight
        days <= 7 -> StudyHubColors.Info
        else -> StudyHubColors.Secondary
    }
}

/** Ordena tareas por urgencia: atrasadas primero → hoy → mañana → próximas; sin fecha y completadas al final. */
fun sortTasksByUrgency(tasks: List<SubjectTask>): List<SubjectTask> =
    tasks.sortedWith(
        compareBy(
            { t -> if (t.completed) 1 else 0 },
            { t -> if (isOverdue(t)) 0 else 1 },
            { t -> daysUntilDue(t.dueDate) ?: Int.MAX_VALUE },
        ),
    )

// ─── Formulario de nueva materia ────────────────────────────────────────

private val SUBJECT_COLORS = listOf(
    "#6366F1", "#3B82F6", "#10B981", "#F59E0B",
    "#F43F5E", "#8B5CF6", "#06B6D4", "#EC4899",
)

@Composable
private fun SubjectFormScreen(onBack: () -> Unit, onCreated: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var profesor by remember { mutableStateOf("") }
    var salon by remember { mutableStateOf("") }
    var creditos by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(SUBJECT_COLORS.first()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar("Nueva materia", "Se agregará a tu lista", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            GlassTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = "Nombre *",
                placeholder = "Ej: Cálculo diferencial",
            )
            Spacer(Modifier.height(10.dp))
            GlassTextField(
                value = codigo,
                onValueChange = { codigo = it },
                label = "Código",
                placeholder = "Ej: MAT-101",
            )
            Spacer(Modifier.height(10.dp))
            GlassTextField(
                value = profesor,
                onValueChange = { profesor = it },
                label = "Profesor",
                placeholder = "Ej: Dra. Laura Gómez",
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassTextField(
                    value = salon,
                    onValueChange = { salon = it },
                    label = "Salón",
                    placeholder = "Ej: B-301",
                    modifier = Modifier.weight(1f),
                )
                GlassTextField(
                    value = creditos,
                    onValueChange = { creditos = it.filter { c -> c.isDigit() }.take(2) },
                    label = "Créditos",
                    placeholder = "3",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            GlassTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = "Descripción",
                placeholder = "Breve descripción de la materia",
                singleLine = false,
            )
            Spacer(Modifier.height(16.dp))
            Text("Color", color = StudyHubColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SUBJECT_COLORS.forEach { c ->
                    val selected = c == color
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(parseColor(c))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = if (selected) StudyHubColors.TextPrimary else androidx.compose.ui.graphics.Color.Transparent,
                                shape = androidx.compose.foundation.shape.CircleShape,
                            )
                            .clickable { color = c }
                            .padding(2.dp),
                    )
                }
            }
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error.orEmpty(), color = StudyHubColors.DangerLight, fontSize = 12.5.sp)
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "Guardar materia",
                icon = Icons.Outlined.Add,
                enabled = nombre.isNotBlank() && !saving,
                loading = saving,
            ) {
                saving = true
                scope.launch {
                    try {
                        Api.client.createSubject(
                            nombre = nombre.trim(),
                            codigo = codigo.trim().ifBlank { null },
                            profesor = profesor.trim().ifBlank { null },
                            salon = salon.trim().ifBlank { null },
                            creditos = creditos.trim().toIntOrNull(),
                            color = color,
                            descripcion = descripcion.trim().ifBlank { null },
                        )
                        onCreated()
                    } catch (e: Exception) {
                        error = e.message ?: "No se pudo crear la materia"
                    } finally {
                        saving = false
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Formulario de nueva tarea ──────────────────────────────────────────

private val TASK_PRIORITIES = listOf(
    "LOW" to "Baja",
    "MEDIUM" to "Media",
    "HIGH" to "Alta",
)

private fun priorityColor(priority: String): Color = when (priority) {
    "HIGH" -> StudyHubColors.Danger
    "MEDIUM" -> StudyHubColors.Accent
    else -> StudyHubColors.Secondary
}

/** Valida una fecha AAAA-MM-DD estricta (días reales del mes, incl. bisiestos). */
fun isValidIsoDate(iso: String): Boolean {
    val parts = iso.split("-")
    if (parts.size != 3) return false
    if (parts[0].length != 4 || parts[1].length != 2 || parts[2].length != 2) return false
    val y = parts[0].toIntOrNull() ?: return false
    val m = parts[1].toIntOrNull() ?: return false
    val d = parts[2].toIntOrNull() ?: return false
    if (m < 1 || m > 12 || d < 1) return false
    val daysInMonth = when (m) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) 29 else 28
    }
    return d <= daysInMonth
}

@Composable
private fun TaskFormScreen(
    subjectId: Int,
    task: SubjectTask? = null,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val editing = task != null
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var priority by remember { mutableStateOf(task?.priority?.takeIf { it in listOf("LOW", "MEDIUM", "HIGH") } ?: "MEDIUM") }
    var dueDate by remember { mutableStateOf(task?.dueDate?.substringBefore("T") ?: todayIso()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar(if (editing) "Editar tarea" else "Nueva tarea", if (editing) "Modifica los datos de la tarea" else "Se agregará a esta materia", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            GlassTextField(
                value = title,
                onValueChange = { title = it },
                label = "Título *",
                placeholder = "Ej: Resolver taller de límites",
            )
            Spacer(Modifier.height(10.dp))
            GlassTextField(
                value = description,
                onValueChange = { description = it },
                label = "Descripción",
                placeholder = "Detalles de la tarea (opcional)",
                singleLine = false,
            )
            Spacer(Modifier.height(16.dp))
            Text("Prioridad", color = StudyHubColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TASK_PRIORITIES.forEach { (key, label) ->
                    val selected = key == priority
                    val color = priorityColor(key)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) color.copy(alpha = 0.16f) else StudyHubColors.Surface)
                            .border(1.dp, if (selected) color else StudyHubColors.Border, RoundedCornerShape(12.dp))
                            .clickable { priority = key }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (selected) color else StudyHubColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Fecha límite", color = StudyHubColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            GlassTextField(
                value = dueDate,
                onValueChange = { dueDate = it.filter { c -> c.isDigit() || c == '-' }.take(10) },
                label = "AAAA-MM-DD",
                placeholder = "2026-08-15",
            )
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error.orEmpty(), color = StudyHubColors.DangerLight, fontSize = 12.5.sp)
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = if (editing) "Guardar cambios" else "Guardar tarea",
                icon = if (editing) Icons.Outlined.Edit else Icons.Outlined.Add,
                enabled = title.isNotBlank() && !saving && isValidIsoDate(dueDate),
                loading = saving,
            ) {
                saving = true
                scope.launch {
                    try {
                        if (editing) {
                            Api.client.updateTask(
                                subjectId = subjectId,
                                taskId = task!!.id,
                                title = title.trim(),
                                description = description.trim().ifBlank { null },
                                priority = priority,
                                dueDate = "${dueDate}T23:59:00.000Z",
                            )
                        } else {
                            Api.client.createTask(
                                subjectId = subjectId,
                                title = title.trim(),
                                description = description.trim().ifBlank { null },
                                priority = priority,
                                dueDate = "${dueDate}T23:59:00.000Z",
                            )
                        }
                        onDone()
                    } catch (e: Exception) {
                        error = e.message ?: "No se pudo guardar la tarea"
                    } finally {
                        saving = false
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Formulario de nueva clase ──────────────────────────────────────────

private val WEEK_DAYS = listOf(
    1 to "Lunes",
    2 to "Martes",
    3 to "Miércoles",
    4 to "Jueves",
    5 to "Viernes",
    6 to "Sábado",
    0 to "Domingo",
)

/** Valida una hora HH:mm (00:00–23:59). */
fun isValidTime(hhmm: String): Boolean {
    val parts = hhmm.split(":")
    if (parts.size != 2) return false
    val h = parts[0].toIntOrNull() ?: return false
    val m = parts[1].toIntOrNull() ?: return false
    return h in 0..23 && m in 0..59 && parts[0].length == 2 && parts[1].length == 2
}

/** Minutos desde medianoche de una hora HH:mm; null si es inválida. */
fun timeToMinutes(hhmm: String): Int? {
    if (!isValidTime(hhmm)) return null
    val (h, m) = hhmm.split(":").map { it.toInt() }
    return h * 60 + m
}

@Composable
private fun ScheduleFormScreen(
    subjectId: Int,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    var dayOfWeek by remember { mutableStateOf(1) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val timesValid = isValidTime(startTime) && isValidTime(endTime) &&
        (timeToMinutes(endTime) ?: 0) > (timeToMinutes(startTime) ?: 0)

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar("Nueva clase", "Se agregará al horario de la materia", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Text("Día de la semana", color = StudyHubColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WEEK_DAYS.forEach { (day, label) ->
                    val selected = day == dayOfWeek
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) StudyHubColors.Primary.copy(alpha = 0.18f) else StudyHubColors.Surface)
                            .border(1.dp, if (selected) StudyHubColors.Primary else StudyHubColors.Border, RoundedCornerShape(12.dp))
                            .clickable { dayOfWeek = day }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (selected) StudyHubColors.PrimaryLight else StudyHubColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Horario", color = StudyHubColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
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
                    placeholder = "10:00",
                    modifier = Modifier.weight(1f),
                )
            }
            if (startTime.isNotBlank() && endTime.isNotBlank() && !timesValid) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "La hora de fin debe ser posterior a la de inicio (formato HH:mm).",
                    color = StudyHubColors.DangerLight,
                    fontSize = 11.5.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
            GlassTextField(
                value = classroom,
                onValueChange = { classroom = it },
                label = "Salón",
                placeholder = "Ej: Aula 204 (opcional)",
            )
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error.orEmpty(), color = StudyHubColors.DangerLight, fontSize = 12.5.sp)
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "Guardar clase",
                icon = Icons.Outlined.Add,
                enabled = timesValid && !saving,
                loading = saving,
            ) {
                saving = true
                scope.launch {
                    try {
                        Api.client.createSchedule(
                            subjectId = subjectId,
                            dayOfWeek = dayOfWeek,
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            classroom = classroom.trim().ifBlank { null },
                        )
                        onDone()
                    } catch (e: Exception) {
                        error = e.message ?: "No se pudo crear la clase"
                    } finally {
                        saving = false
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Formulario de nuevo apunte ─────────────────────────────────────────

@Composable
private fun NoteFormScreen(
    subjectId: Int,
    note: Note? = null,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val editing = note != null
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var pinned by remember { mutableStateOf(note?.pinned ?: false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar(if (editing) "Editar apunte" else "Nuevo apunte", if (editing) "Modifica el contenido del apunte" else "Se agregará a esta materia", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            GlassTextField(
                value = title,
                onValueChange = { title = it },
                label = "Título *",
                placeholder = "Ej: Resumen de derivadas",
            )
            Spacer(Modifier.height(10.dp))
            GlassTextField(
                value = content,
                onValueChange = { content = it },
                label = "Contenido *",
                placeholder = "Escribe el contenido del apunte…",
                singleLine = false,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (pinned) StudyHubColors.Accent.copy(alpha = 0.12f) else StudyHubColors.Surface)
                    .border(1.dp, if (pinned) StudyHubColors.Accent.copy(alpha = 0.5f) else StudyHubColors.Border, RoundedCornerShape(14.dp))
                    .clickable { pinned = !pinned }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(
                    icon = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    tint = if (pinned) StudyHubColors.Accent else StudyHubColors.TextTertiary,
                    size = 17.dp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (pinned) "Apunte fijado" else "Fijar apunte",
                    color = if (pinned) StudyHubColors.Accent else StudyHubColors.TextSecondary,
                    fontSize = 13.5.sp,
                    fontWeight = if (pinned) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (pinned) "Desfijar" else "Al inicio del listado",
                    color = if (pinned) StudyHubColors.AccentLight else StudyHubColors.TextTertiary,
                    fontSize = 11.5.sp,
                )
            }
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error.orEmpty(), color = StudyHubColors.DangerLight, fontSize = 12.5.sp)
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = if (editing) "Guardar cambios" else "Guardar apunte",
                icon = if (editing) Icons.Outlined.Edit else Icons.Outlined.Add,
                enabled = title.isNotBlank() && content.isNotBlank() && !saving,
                loading = saving,
            ) {
                saving = true
                scope.launch {
                    try {
                        if (editing) {
                            Api.client.updateNote(
                                subjectId = subjectId,
                                noteId = note!!.id,
                                title = title.trim(),
                                content = content.trim(),
                                isPinned = pinned,
                            )
                        } else {
                            Api.client.createNote(
                                subjectId = subjectId,
                                title = title.trim(),
                                content = content.trim(),
                                isPinned = pinned,
                            )
                        }
                        onDone()
                    } catch (e: Exception) {
                        error = e.message ?: "No se pudo guardar el apunte"
                    } finally {
                        saving = false
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
