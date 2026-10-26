package org.studyhub.project.ui

import org.studyhub.project.net.ActiveGoal
import org.studyhub.project.net.CalendarData
import org.studyhub.project.net.CalendarEvent
import org.studyhub.project.net.CalendarSubject
import org.studyhub.project.net.CalendarTask
import org.studyhub.project.net.DashboardStats
import org.studyhub.project.net.DashboardSummary
import org.studyhub.project.net.DashboardUser
import org.studyhub.project.net.GamificationProgress
import org.studyhub.project.net.Note
import org.studyhub.project.net.ProfileAcademic
import org.studyhub.project.net.RecentNote
import org.studyhub.project.net.Schedule
import org.studyhub.project.net.Subject
import org.studyhub.project.net.SubjectCount
import org.studyhub.project.net.SubjectDetail
import org.studyhub.project.net.SubjectTask
import org.studyhub.project.net.UpcomingClass
import org.studyhub.project.net.UpcomingTask

/**
 * Datos de muestra para el "Modo demo".
 *
 * Permiten recorrer el rediseño sin cuenta ni conexión. Solo se usan cuando
 * no hay sesión iniciada (TokenStore.load() == null).
 */
internal object DemoData {
    const val userName = "María González"
    const val unread = 3

    private val subjectsSeed = listOf(
        Subject(
            id = 1,
            nombre = "Cálculo II",
            codigo = "MAT-201",
            profesor = "Dra. Elena Ríos",
            salon = "Aula 204",
            creditos = 4,
            color = "#8FAE7E",
            count = SubjectCount(tasks = 4, notes = 2, schedules = 2),
        ),
        Subject(
            id = 2,
            nombre = "Programación Web",
            codigo = "SIS-305",
            profesor = "Ing. Marcos Peña",
            salon = "Lab 3",
            creditos = 4,
            color = "#D9A441",
            count = SubjectCount(tasks = 2, notes = 5, schedules = 2),
        ),
        Subject(
            id = 3,
            nombre = "Física Mecánica",
            codigo = "FIS-102",
            profesor = "Dr. Julián Soto",
            salon = "Aula 108",
            creditos = 4,
            color = "#C97B63",
            count = SubjectCount(tasks = 3, notes = 1, schedules = 1),
        ),
        Subject(
            id = 4,
            nombre = "Bases de Datos",
            codigo = "SIS-402",
            profesor = "Ing. Paula Vega",
            salon = "Lab 1",
            creditos = 3,
            color = "#8E9FD4",
            count = SubjectCount(tasks = 5, notes = 3, schedules = 2),
        ),
        Subject(
            id = 5,
            nombre = "Inglés Técnico",
            codigo = "IDI-110",
            profesor = "Mg. Laura King",
            salon = "Aula 302",
            creditos = 2,
            color = "#B58BB5",
            count = SubjectCount(tasks = 1, notes = 2, schedules = 1),
        ),
        Subject(
            id = 6,
            nombre = "Estadística",
            codigo = "MAT-210",
            profesor = "Dr. Andrés Lozano",
            salon = "Aula 205",
            creditos = 3,
            color = "#7FB3A8",
            count = SubjectCount(tasks = 4, notes = 2, schedules = 2),
        ),
    )

    val subjects: List<Subject> = subjectsSeed

    private val tasksBySubject: Map<Int, List<SubjectTask>> = mapOf(
        1 to listOf(
            task(101, "Taller de derivadas", 1, "COMPLETED"),
            task(102, "Quiz de integrales", 1, "COMPLETED"),
            task(103, "Ejercicios capítulo 4", 1, "COMPLETED"),
            task(104, "Parcial final", 1, "PENDING"),
        ),
        2 to listOf(
            task(201, "Landing page del proyecto", 2, "COMPLETED"),
            task(202, "Entregar proyecto final", 2, "PENDING"),
        ),
        3 to listOf(
            task(301, "Informe de laboratorio", 3, "COMPLETED"),
            task(302, "Taller de cinemática", 3, "PENDING"),
            task(303, "Preparar exposición", 3, "PENDING"),
        ),
        4 to listOf(
            task(401, "Modelo entidad-relación", 4, "COMPLETED"),
            task(402, "Consultas SQL", 4, "COMPLETED"),
            task(403, "Normalización", 4, "COMPLETED"),
            task(404, "Taller de índices", 4, "COMPLETED"),
            task(405, "Proyecto integrador", 4, "PENDING"),
        ),
        5 to listOf(
            task(501, "Listening exercise", 5, "COMPLETED"),
        ),
        6 to listOf(
            task(601, "Taller de probabilidad", 6, "COMPLETED"),
            task(602, "Hoja de ejercicios", 6, "COMPLETED"),
            task(603, "Quiz de regresión", 6, "PENDING"),
            task(604, "Proyecto de datos", 6, "PENDING"),
        ),
    )

    private val notesBySubject: Map<Int, List<Note>> = mapOf(
        1 to listOf(
            Note(id = 1, title = "Fórmulas clave", content = "Derivadas, integrales por partes y regla de la cadena.", pinned = true, color = "#8FAE7E"),
            Note(id = 2, title = "Dudas del parcial", content = "Repasar límites laterales y continuidad.", color = "#8FAE7E"),
        ),
        2 to listOf(
            Note(id = 3, title = "Stack del proyecto", content = "Kotlin + Compose Multiplatform + Ktor.", color = "#D9A441"),
            Note(id = 4, title = "Checklist de entrega", content = "Responsive, accesibilidad y README.", color = "#D9A441"),
        ),
        3 to listOf(
            Note(id = 5, title = "Laboratorio 3", content = "Mediciones de aceleración y fricción.", color = "#C97B63"),
        ),
        4 to listOf(
            Note(id = 6, title = "Comandos SQL", content = "JOIN, GROUP BY y subconsultas.", color = "#8E9FD4"),
        ),
        5 to listOf(
            Note(id = 7, title = "Vocabulario", content = "Phrasal verbs para presentaciones.", color = "#B58BB5"),
        ),
        6 to listOf(
            Note(id = 8, title = "Distribuciones", content = "Normal, binomial y Poisson.", color = "#7FB3A8"),
        ),
    )

    private val schedulesBySubject: Map<Int, List<Schedule>> = mapOf(
        1 to listOf(
            Schedule(id = 1, dayOfWeek = 1, startTime = "10:00", endTime = "11:30", classroom = "Aula 204"),
            Schedule(id = 2, dayOfWeek = 3, startTime = "08:00", endTime = "09:30", classroom = "Aula 204"),
        ),
        2 to listOf(
            Schedule(id = 3, dayOfWeek = 2, startTime = "14:00", endTime = "16:00", classroom = "Lab 3"),
            Schedule(id = 4, dayOfWeek = 4, startTime = "14:00", endTime = "16:00", classroom = "Lab 3"),
        ),
        3 to listOf(
            Schedule(id = 5, dayOfWeek = 2, startTime = "10:00", endTime = "12:00", classroom = "Aula 108"),
        ),
        4 to listOf(
            Schedule(id = 6, dayOfWeek = 3, startTime = "16:00", endTime = "18:00", classroom = "Lab 1"),
            Schedule(id = 7, dayOfWeek = 5, startTime = "10:00", endTime = "12:00", classroom = "Lab 1"),
        ),
        5 to listOf(
            Schedule(id = 8, dayOfWeek = 4, startTime = "08:00", endTime = "09:00", classroom = "Aula 302"),
        ),
        6 to listOf(
            Schedule(id = 9, dayOfWeek = 1, startTime = "16:00", endTime = "18:00", classroom = "Aula 205"),
            Schedule(id = 10, dayOfWeek = 5, startTime = "08:00", endTime = "10:00", classroom = "Aula 205"),
        ),
    )

    val subjectDetails: Map<Int, SubjectDetail> = subjectsSeed.associate { s ->
        s.id to SubjectDetail(
            id = s.id,
            nombre = s.nombre,
            codigo = s.codigo,
            profesor = s.profesor,
            salon = s.salon,
            creditos = s.creditos,
            color = s.color,
            schedules = schedulesBySubject[s.id].orEmpty(),
            tasks = tasksBySubject[s.id].orEmpty(),
            notes = notesBySubject[s.id].orEmpty(),
        )
    }

    val academic = ProfileAcademic(
        universidad = "Universidad Nacional",
        carrera = "Ingeniería de Sistemas",
        facultad = "Ingeniería",
        semestreActual = 5,
        promedio = 84.0,
        modalidad = "Presencial",
    )

    val exams: List<CalendarEvent> = listOf(
        CalendarEvent(
            id = 1,
            title = "Parcial de Cálculo II",
            description = "Unidades 3 y 4",
            startAt = isoInDays(2) + "T10:00:00.000Z",
            endAt = isoInDays(2) + "T12:00:00.000Z",
            type = "EXAM",
            color = "#C97B63",
            subject = CalendarSubject(id = 1, nombre = "Cálculo II", color = "#8FAE7E"),
        ),
        CalendarEvent(
            id = 2,
            title = "Entrega proyecto web",
            description = "Deploy y documentación",
            startAt = isoInDays(5) + "T23:59:00.000Z",
            endAt = isoInDays(5) + "T23:59:00.000Z",
            type = "TASK",
            color = "#D9A441",
            subject = CalendarSubject(id = 2, nombre = "Programación Web", color = "#D9A441"),
        ),
        CalendarEvent(
            id = 3,
            title = "Exposición de Física",
            description = "Laboratorio de mecánica",
            startAt = isoInDays(9) + "T14:00:00.000Z",
            endAt = isoInDays(9) + "T15:00:00.000Z",
            type = "EXAM",
            color = "#C97B63",
            subject = CalendarSubject(id = 3, nombre = "Física Mecánica", color = "#C97B63"),
        ),
    )

    val calendar: CalendarData = CalendarData(
        events = exams,
        tasks = listOf(
            CalendarTask(
                id = "task-202",
                title = "Entregar proyecto final",
                startAt = isoInDays(5) + "T23:59:00.000Z",
                endAt = isoInDays(5) + "T23:59:00.000Z",
                color = "#D9A441",
                type = "TASK",
                subject = CalendarSubject(id = 2, nombre = "Programación Web", color = "#D9A441"),
                taskStatus = "PENDING",
            ),
        ),
    )

    val dashboard: DashboardSummary = DashboardSummary(
        user = DashboardUser(id = 1, nombre = "María González"),
        stats = DashboardStats(subjects = 6, pendingTasks = 4, completedTasks = 12, notes = 8),
        gamification = GamificationProgress(
            level = 7,
            xp = 315,
            totalXp = 2015,
            xpForNextLevel = 2300,
            streak = 5,
            bestStreak = 12,
            achievements = 3,
        ),
        upcomingClasses = listOf(
            UpcomingClass(
                subject = "Cálculo II",
                classroom = "Aula 204",
                profesor = "Dra. Elena Ríos",
                startTime = "10:00",
                endTime = "11:30",
                color = "#8FAE7E",
            ),
            UpcomingClass(
                subject = "Programación Web",
                classroom = "Lab 3",
                profesor = "Ing. Marcos Peña",
                startTime = "14:00",
                endTime = "16:00",
                color = "#D9A441",
            ),
        ),
        upcomingTasks = listOf(
            UpcomingTask(
                id = 202,
                title = "Entregar proyecto final",
                subject = "Programación Web",
                subjectColor = "#D9A441",
                subjectId = 2,
                dueDate = isoInDays(5),
                priority = "HIGH",
            ),
            UpcomingTask(
                id = 104,
                title = "Parcial final",
                subject = "Cálculo II",
                subjectColor = "#8FAE7E",
                subjectId = 1,
                dueDate = isoInDays(2),
                priority = "HIGH",
            ),
            UpcomingTask(
                id = 603,
                title = "Quiz de regresión",
                subject = "Estadística",
                subjectColor = "#7FB3A8",
                subjectId = 6,
                dueDate = isoInDays(7),
                priority = "MEDIUM",
            ),
        ),
        recentNotes = listOf(
            RecentNote(
                id = 1,
                title = "Fórmulas clave",
                content = "Derivadas, integrales por partes y regla de la cadena.",
                pinned = true,
                subject = "Cálculo II",
                subjectColor = "#8FAE7E",
            ),
            RecentNote(
                id = 3,
                title = "Stack del proyecto",
                content = "Kotlin + Compose Multiplatform + Ktor.",
                pinned = false,
                subject = "Programación Web",
                subjectColor = "#D9A441",
            ),
        ),
        activeGoals = listOf(
            ActiveGoal(title = "Dominar integrales por partes", progress = 65),
            ActiveGoal(title = "Terminar el proyecto web", progress = 40),
        ),
        completionRate = 68.0,
    )

    fun replyFor(prompt: String): String = buildString {
        append("¡Buena pregunta! ")
        when {
            prompt.contains("flashcard", ignoreCase = true) ->
                append("Puedo convertir tus apuntes de Cálculo II y Programación Web en tarjetas de repaso. Empieza con 5 tarjetas por tema y repásalas en intervalos de 1, 3 y 7 días.")
            prompt.contains("plan", ignoreCase = true) ->
                append("Te propongo un plan de 45 minutos: 10 min para repasar apuntes, 25 min de práctica activa y 10 min para explicar lo aprendido con tus propias palabras.")
            prompt.contains("examen", ignoreCase = true) ->
                append("Para tu parcial de Cálculo II, prioriza derivadas, integrales por partes y límites. Haz un simulacro corto y revisa solo los errores.")
            else ->
                append("Puedo explicarte el tema paso a paso, crear un plan de estudio o preparar un simulacro. Dime qué materia quieres repasar y empezamos.")
        }
    }

    private fun task(id: Int, title: String, subjectId: Int, status: String): SubjectTask =
        SubjectTask(
            id = id,
            title = title,
            dueDate = isoInDays(if (status == "COMPLETED") -3 else 4),
            priority = if (status == "COMPLETED") "MEDIUM" else "HIGH",
            status = status,
            subjectId = subjectId,
        )

    private fun isoInDays(days: Int): String {
        val parts = todayIso().split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val d = parts.getOrNull(2)?.toIntOrNull() ?: 1
        val epochDay = (dateToEpochDay(y, m, d) ?: 0L) + days
        return isoFromEpochDay(epochDay)
    }

    private fun isoFromEpochDay(epochDay: Long): String {
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
}
