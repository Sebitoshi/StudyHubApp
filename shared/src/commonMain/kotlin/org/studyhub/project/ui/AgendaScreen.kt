package org.studyhub.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Timer
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.studyhub.project.StudyHubColors
import org.studyhub.project.net.Api
import org.studyhub.project.net.CalendarData
import org.studyhub.project.net.CalendarEvent
import org.studyhub.project.net.SubjectDetail
import org.studyhub.project.net.TokenStore
import org.studyhub.project.ui.icons.AppIcon

// ─── Agenda semanal ─────────────────────────────────────────────────────

private val WEEKDAYS_SHORT = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE")
private val WEEKDAYS_LONG = listOf("LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO")
private val MONTHS_LONG = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
)
private val AGENDA_MONTHS_SHORT = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")

private enum class AgendaKind(val label: String, val color: Color) {
    CLASS("CLASE", StudyHubColors.Primary),
    STUDY("ESTUDIO", StudyHubColors.Accent),
    TASK("TAREA", StudyHubColors.Info),
    EXAM("EXAMEN", StudyHubColors.Danger),
    EVENT("EVENTO", StudyHubColors.Secondary),
}

private data class AgendaItem(
    val dateIso: String,
    val start: String,
    val end: String,
    val allDay: Boolean = false,
    val title: String,
    val detail: String,
    val kind: AgendaKind,
    val color: Color,
)

@Composable
fun AgendaScreen(
    unread: Int = 0,
    onBell: () -> Unit = {},
    userName: String = "Estudiante",
    userPhoto: String? = null,
) {
    val demo = remember { TokenStore.load() == null }
    val today = remember { todayIso() }
    val todayEpoch = remember { epochDayFromIso(today) ?: 0L }
    val todayMonday0 = remember { ((todayEpoch + 3) % 7 + 7) % 7 }
    val weekend = todayMonday0 > 4
    val baseWeekStart = remember { todayEpoch - todayMonday0 + if (weekend) 7 else 0 }
    val baseSelected = remember { if (demo) baseWeekStart + 2 else if (weekend) baseWeekStart else todayEpoch }

    var weekStart by remember { mutableStateOf(baseWeekStart) }
    var selectedIso by remember { mutableStateOf(isoFromEpochDay(baseSelected)) }
    var calendar by remember { mutableStateOf<CalendarData?>(null) }
    var exams by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var details by remember { mutableStateOf<List<SubjectDetail>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<CalendarEvent?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        if (demo) {
            calendar = DemoData.calendar
            exams = DemoData.exams
            details = DemoData.subjectDetails.values.toList()
            error = null
            loading = false
            return@LaunchedEffect
        }
        val calendarResult = runCatching { Api.client.calendarEvents() }
        val examsResult = runCatching { Api.client.upcomingExams() }
        val subjectsResult = runCatching { Api.client.subjects() }
        calendar = calendarResult.getOrNull()
        exams = examsResult.getOrDefault(emptyList())
        // Mostramos eventos y tareas de inmediato; las clases llegan después.
        loading = false
        val loadedDetails = coroutineScope {
            subjectsResult.getOrDefault(emptyList()).map { subject ->
                async { runCatching { Api.client.subjectDetail(subject.id) }.getOrNull() }
            }.awaitAll().filterNotNull()
        }
        details = loadedDetails
        error = if (calendar == null && loadedDetails.isEmpty() && subjectsResult.isFailure) {
            calendarResult.exceptionOrNull()?.message ?: "No se pudo cargar la agenda"
        } else {
            null
        }
        loading = false
    }

    val baseItems = remember(weekStart, calendar, exams, details) {
        buildAgendaItems(
            weekStart = weekStart,
            calendar = calendar,
            exams = exams,
            details = details,
        )
    }
    val dayItems = remember(baseItems, selectedIso, demo) {
        val fromData = baseItems.filter { it.dateIso == selectedIso }
        val extras = if (demo) demoExtras(selectedIso) else emptyList()
        (fromData + extras)
            .distinctBy { "${it.dateIso}|${it.start}|${it.title}" }
            .sortedBy { it.start.ifBlank { "00:00" } }
    }
    val upcoming = remember(baseItems, today) {
        baseItems
            .filter { it.dateIso >= today }
            .sortedWith(compareBy({ it.dateIso }, { it.start.ifBlank { "00:00" } }))
            .take(4)
    }
    val showUpcoming = dayItems.isEmpty() && upcoming.isNotEmpty()
    val listItems = if (showUpcoming) upcoming else dayItems

    Box(Modifier.fillMaxSize()) {
        when {
            loading && calendar == null && details.isEmpty() -> LoadingState()
            error != null -> ErrorState(error.orEmpty()) {
                loading = true
                error = null
                reload++
            }

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                StudyHubTopBar(
                    eyebrow = "Organiza tu semana",
                    title = "Mi agenda",
                    unread = unread,
                    onBell = onBell,
                    userName = userName,
                    userPhoto = userPhoto,
                )
                Spacer(Modifier.height(16.dp))

                MonthNavigator(
                    title = monthTitle(selectedIso),
                    onPrevious = {
                        weekStart -= 7
                        selectedIso = isoFromEpochDay((epochDayFromIso(selectedIso) ?: weekStart) - 7)
                    },
                    onNext = {
                        weekStart += 7
                        selectedIso = isoFromEpochDay((epochDayFromIso(selectedIso) ?: weekStart) + 7)
                    },
                )
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WEEKDAYS_SHORT.forEachIndexed { index, label ->
                        val iso = isoFromEpochDay(weekStart + index)
                        val selected = iso == selectedIso
                        val isToday = iso == today
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) StudyHubColors.Primary else StudyHubColors.Surface)
                                .border(
                                    1.dp,
                                    if (selected) StudyHubColors.Primary else StudyHubColors.Border,
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable { selectedIso = iso }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White.copy(alpha = 0.82f) else StudyHubColors.TextTertiary,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = iso.substringAfterLast("-").trimStart('0').ifBlank { "0" },
                                color = if (selected) Color.White else StudyHubColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Box(
                                Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isToday && selected -> Color.White
                                            isToday -> StudyHubColors.Primary
                                            else -> Color.Transparent
                                        },
                                    ),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                DaySummaryCard(
                    dateIso = selectedIso,
                    count = dayItems.size,
                )
                Spacer(Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Próximas actividades",
                        color = StudyHubColors.TextPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .clickable {
                                editing = CalendarEvent(
                                    title = "",
                                    startAt = selectedIso + "T09:00:00.000Z",
                                    endAt = selectedIso + "T10:00:00.000Z",
                                    type = "EVENT",
                                )
                            }
                            .padding(horizontal = 4.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(Icons.Outlined.Add, tint = StudyHubColors.Primary, size = 14.dp)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "Añadir",
                            color = StudyHubColors.Primary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (listItems.isEmpty()) {
                    EmptyAgendaCard()
                } else {
                    listItems.forEachIndexed { index, item ->
                        AgendaTimelineRow(
                            item = item,
                            showDate = showUpcoming,
                            isLast = index == listItems.lastIndex,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                TipCard()
                Spacer(Modifier.height(20.dp))
            }
        }

        editing?.let { event ->
            Box(Modifier.fillMaxSize().background(StudyHubColors.Bg)) {
                EditEventScreen(
                    event = event,
                    onBack = { editing = null },
                    onDone = {
                        editing = null
                        reload++
                    },
                )
            }
        }
    }
}

@Composable
private fun MonthNavigator(title: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onPrevious),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                tint = StudyHubColors.TextSecondary,
                size = 20.dp,
            )
        }
        Text(
            text = title,
            color = StudyHubColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                tint = StudyHubColors.TextSecondary,
                size = 20.dp,
            )
        }
    }
}

@Composable
private fun DaySummaryCard(dateIso: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StudyHubColors.SageSoft)
            .border(1.dp, StudyHubColors.Border, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = dayHeader(dateIso),
                color = StudyHubColors.TextSecondary,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (count == 0) "Tu día libre, paso a paso" else "Tu día, paso a paso",
                color = StudyHubColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = when (count) {
                    0 -> "Sin actividades por ahora"
                    1 -> "1 actividad programada"
                    else -> "$count actividades programadas"
                },
                color = StudyHubColors.TextTertiary,
                fontSize = 10.5.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(StudyHubColors.Surface)
                .border(1.dp, StudyHubColors.Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(Icons.Outlined.CheckCircle, tint = StudyHubColors.Primary, size = 20.dp)
        }
    }
}

@Composable
private fun AgendaTimelineRow(item: AgendaItem, showDate: Boolean, isLast: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Column(
            modifier = Modifier
                .width(42.dp)
                .padding(top = 1.dp),
            horizontalAlignment = Alignment.End,
        ) {
            if (item.allDay) {
                Text("Todo", color = StudyHubColors.TextSecondary, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                Text("el día", color = StudyHubColors.TextTertiary, fontSize = 8.5.sp)
            } else {
                Text(
                    text = item.start,
                    color = StudyHubColors.TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (item.end.isNotBlank()) {
                    Text(item.end, color = StudyHubColors.TextTertiary, fontSize = 8.5.sp)
                }
            }
        }
        Spacer(Modifier.width(9.dp))
        Box(
            modifier = Modifier
                .width(12.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(top = 12.dp)
                        .background(StudyHubColors.Primary.copy(alpha = 0.22f)),
                )
            }
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(item.kind.color),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(item.kind.label, item.kind.color, small = true)
                if (showDate) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = shortDate(item.dateIso),
                        color = StudyHubColors.TextTertiary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = item.title,
                color = StudyHubColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            if (item.detail.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(detailIcon(item.kind), tint = StudyHubColors.TextTertiary, size = 12.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = item.detail,
                        color = StudyHubColors.TextTertiary,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAgendaCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StudyHubColors.Surface)
            .border(1.dp, StudyHubColors.Border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(StudyHubColors.Sky),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(Icons.Outlined.CalendarMonth, tint = StudyHubColors.Info, size = 19.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Nada pendiente por aquí",
                color = StudyHubColors.TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Añade un evento o revisa otra fecha de la semana.",
                color = StudyHubColors.TextTertiary,
                fontSize = 10.5.sp,
            )
        }
    }
}

@Composable
private fun TipCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StudyHubColors.Peach)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(StudyHubColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(Icons.Outlined.Timer, tint = StudyHubColors.Accent, size = 17.dp)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "Un espacio para todo.",
                color = StudyHubColors.AccentDark,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Deja tiempo para descansar entre tus bloques.",
                color = StudyHubColors.TextSecondary,
                fontSize = 10.5.sp,
            )
        }
    }
}

private fun detailIcon(kind: AgendaKind) = when (kind) {
    AgendaKind.CLASS -> Icons.Outlined.LocationOn
    AgendaKind.STUDY -> Icons.Outlined.Timer
    AgendaKind.TASK -> Icons.Outlined.Flag
    AgendaKind.EXAM, AgendaKind.EVENT -> Icons.Outlined.Event
}

private fun buildAgendaItems(
    weekStart: Long,
    calendar: CalendarData?,
    exams: List<CalendarEvent>,
    details: List<SubjectDetail>,
): List<AgendaItem> {
    val items = mutableListOf<AgendaItem>()

    details.forEach { subject ->
        subject.schedules.forEach { schedule ->
            val monday0 = ((schedule.dayOfWeek + 6) % 7).coerceIn(0, 6)
            items += AgendaItem(
                dateIso = isoFromEpochDay(weekStart + monday0),
                start = schedule.startTime,
                end = schedule.endTime,
                title = subject.nombre,
                detail = listOf(
                    schedule.classroom.ifBlank { subject.salon },
                    subject.profesor,
                ).filter { it.isNotBlank() }.joinToString(" · "),
                kind = AgendaKind.CLASS,
                color = parseColor(subject.color),
            )
        }
    }

    val allEvents = (calendar?.events.orEmpty() + exams)
        .distinctBy { "${it.startAt}|${it.title}" }
    allEvents.forEach { event ->
        val date = event.startAt.substringBefore("T")
        if (date.isBlank()) return@forEach
        val colorHex = event.color.ifBlank { event.subject?.color.orEmpty() }
        items += AgendaItem(
            dateIso = date,
            start = timeOfIso(event.startAt),
            end = timeOfIso(event.endAt),
            allDay = event.allDay,
            title = event.title.ifBlank { "Evento" },
            detail = event.subject?.nombre?.takeIf { it.isNotBlank() }
                ?: event.description.take(48).takeIf { it.isNotBlank() }
                ?: "Evento del calendario",
            kind = if (event.type == "EXAM") AgendaKind.EXAM else AgendaKind.EVENT,
            color = if (colorHex.isBlank()) StudyHubColors.Secondary else parseColor(colorHex),
        )
    }

    calendar?.tasks.orEmpty()
        .filter { it.taskStatus != "COMPLETED" }
        .forEach { task ->
            val date = task.startAt.substringBefore("T")
            if (date.isBlank()) return@forEach
            val colorHex = task.color.ifBlank { task.subject?.color.orEmpty() }
            val status = when (task.taskStatus.uppercase()) {
                "IN_PROGRESS" -> "En progreso"
                "OVERDUE" -> "Atrasada"
                else -> "Pendiente"
            }
            items += AgendaItem(
                dateIso = date,
                start = timeOfIso(task.startAt),
                end = timeOfIso(task.endAt),
                allDay = task.allDay,
                title = task.title.ifBlank { "Tarea" },
                detail = listOf(task.subject?.nombre.orEmpty(), status)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                kind = AgendaKind.TASK,
                color = if (colorHex.isBlank()) StudyHubColors.Info else parseColor(colorHex),
            )
        }

    return items
}

private fun demoExtras(dateIso: String): List<AgendaItem> = listOf(
    AgendaItem(
        dateIso = dateIso,
        start = "12:30",
        end = "13:15",
        title = "Repaso de límites",
        detail = "45 minutos · Enfoque",
        kind = AgendaKind.STUDY,
        color = StudyHubColors.Accent,
    ),
    AgendaItem(
        dateIso = dateIso,
        start = "10:00",
        end = "17:00",
        title = "Entrega programación web",
        detail = "Para hoy · Prioridad alta",
        kind = AgendaKind.TASK,
        color = StudyHubColors.Info,
    ),
)

private fun timeOfIso(iso: String): String {
    if (iso.isBlank()) return ""
    val value = iso.substringAfter("T", "").substringBefore("Z").substringBefore("+")
    return value.split(":").take(2).joinToString(":")
}

private fun monthTitle(iso: String): String {
    val parts = iso.split("-")
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return iso
    val year = parts.getOrNull(0) ?: ""
    return "${MONTHS_LONG.getOrElse(month - 1) { "" }} $year"
}

private fun dayHeader(iso: String): String {
    val epoch = epochDayFromIso(iso) ?: return iso
    val monday0 = (((epoch + 3) % 7 + 7) % 7).toInt()
    val parts = iso.split("-")
    val day = parts.getOrNull(2)?.toIntOrNull() ?: 0
    val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
    return "${WEEKDAYS_LONG.getOrElse(monday0) { "" }}, $day ${AGENDA_MONTHS_SHORT.getOrElse(month - 1) { "" }}"
}

private fun shortDate(iso: String): String {
    val epoch = epochDayFromIso(iso) ?: return iso
    val monday0 = (((epoch + 3) % 7 + 7) % 7).toInt()
    val parts = iso.split("-")
    val day = parts.getOrNull(2)?.toIntOrNull() ?: 0
    val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
    val weekday = if (monday0 <= 4) WEEKDAYS_SHORT[monday0] else "FIN"
    return "$weekday $day ${AGENDA_MONTHS_SHORT.getOrElse(month - 1) { "" }}"
}

internal fun epochDayFromIso(iso: String): Long? {
    val parts = iso.substringBefore("T").split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return dateToEpochDay(year, month, day)
}

internal fun isoFromEpochDay(epochDay: Long): String {
    var z = epochDay + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val year = if (m <= 2) y + 1 else y
    fun p2(n: Long) = n.toString().padStart(2, '0')
    return "$year-${p2(m)}-${p2(d)}"
}
