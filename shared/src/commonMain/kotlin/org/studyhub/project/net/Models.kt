package org.studyhub.project.net

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Serializer tolerante: si el backend devuelve un número o null donde se espera un array
 * (p. ej. `"achievements": 0` en lugar de `[]`), devuelve una lista vacía.
 */
/**
 * Serializer tolerante: el backend manda `difficulty` a veces como número (`1`) y
 * otras como texto (`"INTERMEDIATE"`). Acepta ambos y devuelve el texto.
 */
class StringOrNumberSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrNumber", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        // JsonPrimitive.content ya devuelve el texto: "abc", "1" o "true"
        return (element as? JsonPrimitive)?.content ?: ""
    }
}

class FlexibleListSerializer<T>(private val elementSerializer: KSerializer<T>) : KSerializer<List<T>> {
    private val delegate = ListSerializer(elementSerializer)

    override val descriptor: SerialDescriptor get() = delegate.descriptor

    override fun serialize(encoder: Encoder, value: List<T>) = delegate.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): List<T> {
        val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonArray -> jsonDecoder.json.decodeFromJsonElement(delegate, element)
            else -> emptyList()
        }
    }
}

private object UpcomingClassList : KSerializer<List<UpcomingClass>> by FlexibleListSerializer(UpcomingClass.serializer())
private object UpcomingTaskList : KSerializer<List<UpcomingTask>> by FlexibleListSerializer(UpcomingTask.serializer())
private object RecentNoteList : KSerializer<List<RecentNote>> by FlexibleListSerializer(RecentNote.serializer())
private object ActiveGoalList : KSerializer<List<ActiveGoal>> by FlexibleListSerializer(ActiveGoal.serializer())
private object AchievementList : KSerializer<List<Achievement>> by FlexibleListSerializer(Achievement.serializer())
private object SubjectList : KSerializer<List<Subject>> by FlexibleListSerializer(Subject.serializer())
private object ScheduleList : KSerializer<List<Schedule>> by FlexibleListSerializer(Schedule.serializer())
private object SubjectTaskList : KSerializer<List<SubjectTask>> by FlexibleListSerializer(SubjectTask.serializer())
private object NoteList : KSerializer<List<Note>> by FlexibleListSerializer(Note.serializer())
private object NotificationList : KSerializer<List<NotificationItem>> by FlexibleListSerializer(NotificationItem.serializer())
private object RiskHistoryList : KSerializer<List<RiskAnalysis>> by FlexibleListSerializer(RiskAnalysis.serializer())
private object RoadmapList : KSerializer<List<Roadmap>> by FlexibleListSerializer(Roadmap.serializer())
private object StepList : KSerializer<List<RoadmapStep>> by FlexibleListSerializer(RoadmapStep.serializer())
private object GoalList : KSerializer<List<AiGoal>> by FlexibleListSerializer(AiGoal.serializer())
private object GapList : KSerializer<List<KnowledgeGap>> by FlexibleListSerializer(KnowledgeGap.serializer())
private object ResourceList : KSerializer<List<AiResource>> by FlexibleListSerializer(AiResource.serializer())
private object QuizList : KSerializer<List<QuizQuestion>> by FlexibleListSerializer(QuizQuestion.serializer())
private object ExerciseList : KSerializer<List<SandboxExercise>> by FlexibleListSerializer(SandboxExercise.serializer())
private object ExperienceList : KSerializer<List<ResumeExperience>> by FlexibleListSerializer(ResumeExperience.serializer())
private object EducationList : KSerializer<List<ResumeEducation>> by FlexibleListSerializer(ResumeEducation.serializer())
private object ProjectList : KSerializer<List<ResumeProject>> by FlexibleListSerializer(ResumeProject.serializer())
private object CertificateList : KSerializer<List<ResumeCertificate>> by FlexibleListSerializer(ResumeCertificate.serializer())
private object LanguageList : KSerializer<List<ResumeLanguage>> by FlexibleListSerializer(ResumeLanguage.serializer())

// ─── Auth ────────────────────────────────────────────────────────────────

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val nombre: String,
    val apellido: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String = "",
    val user: User? = null,
)

@Serializable
data class VerifyEmailRequest(val email: String, val code: String)

@Serializable
data class ResendCodeRequest(val email: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(val token: String, val password: String)

@Serializable
data class MessageResponse(val message: String = "")

@Serializable
data class User(
    val id: Int = 0,
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val foto: String? = null,
)

/** Respuesta de GET /profile/personal (incluye la foto de perfil). */
@Serializable
data class PersonalInfo(
    val id: Int = 0,
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val foto: String? = null,
    val ciudad: String? = null,
    val pais: String? = null,
)

// ─── Dashboard ──────────────────────────────────────────────────────────

@Serializable
data class DashboardSummary(
    val user: DashboardUser? = null,
    val stats: DashboardStats? = null,
    val gamification: GamificationProgress? = null,
    val academicRisk: RiskLevel? = null,
    @Serializable(with = UpcomingClassList::class)
    @SerialName("upcomingClasses") val upcomingClasses: List<UpcomingClass> = emptyList(),
    @Serializable(with = UpcomingTaskList::class)
    @SerialName("upcomingTasks") val upcomingTasks: List<UpcomingTask> = emptyList(),
    @Serializable(with = RecentNoteList::class)
    @SerialName("recentNotes") val recentNotes: List<RecentNote> = emptyList(),
    @Serializable(with = ActiveGoalList::class)
    @SerialName("activeGoals") val activeGoals: List<ActiveGoal> = emptyList(),
    @SerialName("completionRate") val completionRate: Double = 0.0,
)

@Serializable
data class DashboardUser(
    val id: Int = 0,
    val nombre: String = "",
    val foto: String? = null,
)

@Serializable
data class DashboardStats(
    val subjects: Int = 0,
    @SerialName("pendingTasks") val pendingTasks: Int = 0,
    @SerialName("completedTasks") val completedTasks: Int = 0,
    val notes: Int = 0,
)

@Serializable
data class RiskLevel(val score: Int = 0, val level: String = "")

@Serializable
data class UpcomingClass(
    val subject: String = "",
    val classroom: String = "",
    val profesor: String = "",
    @SerialName("startTime") val startTime: String = "",
    @SerialName("endTime") val endTime: String = "",
    val color: String = "#6366f1",
)

@Serializable
data class UpcomingTask(
    val id: Int = 0,
    val title: String = "",
    val subject: String = "",
    @SerialName("subjectColor") val subjectColor: String = "#6366f1",
    @SerialName("subjectId") val subjectId: Int = 0,
    @SerialName("dueDate") val dueDate: String = "",
    val priority: String = "MEDIUM",
)

@Serializable
data class RecentNote(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    @SerialName("isPinned") val pinned: Boolean = false,
    val subject: String = "",
    @SerialName("subjectColor") val subjectColor: String = "#6366f1",
)

@Serializable
data class ActiveGoal(val title: String = "", val progress: Int = 0)

// ─── Gamificación ───────────────────────────────────────────────────────

@Serializable
data class GamificationProgress(
    val level: Int = 1,
    val xp: Int = 0,
    @SerialName("totalXp") val totalXp: Int = 0,
    @SerialName("xpForNextLevel") val xpForNextLevel: Int = 1000,
    val streak: Int = 0,
    @SerialName("bestStreak") val bestStreak: Int = 0,
    /** El backend envía el conteo en `achievements` y la lista real en `achievementsList`. */
    val achievements: Int = 0,
    @Serializable(with = AchievementList::class)
    @SerialName("achievementsList") val achievementsList: List<Achievement> = emptyList(),
)

@Serializable
data class Achievement(
    val id: Int = 0,
    val code: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val category: String = "",
    val icon: String = "",
    @SerialName("unlockedAt") val unlockedAt: String = "",
)

// ─── Materias ───────────────────────────────────────────────────────────

@Serializable
data class CreateSubjectRequest(
    val nombre: String,
    val codigo: String? = null,
    val profesor: String? = null,
    val salon: String? = null,
    val creditos: Int? = null,
    val color: String,
    val descripcion: String? = null,
)

@Serializable
data class Subject(
    val id: Int = 0,
    val nombre: String = "",
    val codigo: String = "",
    val profesor: String = "",
    val salon: String = "",
    val creditos: Int = 0,
    val color: String = "#6366f1",
    val descripcion: String = "",
    @SerialName("_count") val count: SubjectCount? = null,
)


@Serializable
data class SubjectCount(
    val tasks: Int = 0,
    val notes: Int = 0,
    val schedules: Int = 0,
)

@Serializable
data class SubjectDetail(
    val id: Int = 0,
    val nombre: String = "",
    val codigo: String = "",
    val profesor: String = "",
    val salon: String = "",
    val creditos: Int = 0,
    val color: String = "#6366f1",
    val descripcion: String = "",
    @Serializable(with = ScheduleList::class) val schedules: List<Schedule> = emptyList(),
    @Serializable(with = SubjectTaskList::class) val tasks: List<SubjectTask> = emptyList(),
    @Serializable(with = NoteList::class) val notes: List<Note> = emptyList(),
)

@Serializable
data class Schedule(
    val id: Int = 0,
    // El backend usa dayOfWeek (0=Domingo … 6=Sábado) y classroom, no day/salon
    @SerialName("dayOfWeek") val dayOfWeek: Int = 1,
    val startTime: String = "",
    val endTime: String = "",
    @SerialName("classroom") val classroom: String = "",
)

/** Nombre del día según dayOfWeek del backend (0=Domingo, 1=Lunes … 6=Sábado). */
fun dayName(dayOfWeek: Int): String = when (dayOfWeek) {
    0 -> "Domingo"
    1 -> "Lunes"
    2 -> "Martes"
    3 -> "Miércoles"
    4 -> "Jueves"
    5 -> "Viernes"
    else -> "Sábado"
}

@Serializable
data class SubjectTask(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    @SerialName("dueDate") val dueDate: String = "",
    val priority: String = "MEDIUM",
    // El backend usa el enum status (PENDING/IN_PROGRESS/COMPLETED/OVERDUE), no un booleano
    @SerialName("status") val status: String = "PENDING",
    val subjectId: Int = 0,
) {
    val completed: Boolean get() = status == "COMPLETED"
}

@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val priority: String,
    val dueDate: String,
)

@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val dueDate: String? = null,
)

@Serializable
data class CreateScheduleRequest(
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val classroom: String? = null,
)

@Serializable
data class CreateNoteRequest(
    val title: String,
    val content: String,
    val isPinned: Boolean? = null,
)

@Serializable
data class UpdateNoteRequest(
    val title: String? = null,
    val content: String? = null,
    val isPinned: Boolean? = null,
)

@Serializable
data class Note(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    @SerialName("isPinned") val pinned: Boolean = false,
    val color: String = "#6366f1",
)

// ─── Notificaciones ─────────────────────────────────────────────────────

@Serializable
data class NotificationItem(
    val id: Int = 0,
    val title: String = "",
    val message: String = "",
    val type: String = "INFO",
    val read: Boolean = false,
    @SerialName("createdAt") val createdAt: String = "",
)

// ─── Riesgo académico ───────────────────────────────────────────────────

@Serializable
data class RiskAnalysis(
    val id: Int = 0,
    val score: Int = 0,
    val level: String = "BAJO",
    // Serializer tolerante: reasons puede venir como objeto {summary,factors} o array de strings (filas antiguas)
    @Serializable(with = RiskReasonsSerializer::class) val reasons: RiskReasons? = null,
    @SerialName("createdAt") val createdAt: String = "",
)


@Serializable
data class RiskReasons(
    // El backend envía { summary: [...], factors: { knowledgeGaps: {score,max,...}, ... } }
    @SerialName("summary") val summary: List<String> = emptyList(),
    @SerialName("factors") val factors: RiskFactors? = null,
)


/**
 * Serializer tolerante: el backend guarda filas antiguas donde `reasons` es un
 * array de strings ("20 gap(s) activo(s)", ...) en vez del objeto { summary, factors }.
 * Acepta ambos formatos; el de array se convierte a { summary = array, factors = null }.
 */
object RiskReasonsSerializer : KSerializer<RiskReasons> {
    private val delegate = RiskReasons.serializer()

    override val descriptor: SerialDescriptor get() = delegate.descriptor

    override fun serialize(encoder: Encoder, value: RiskReasons) = delegate.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): RiskReasons {
        val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonArray) {
            // Formato antiguo: reasons era un array de strings
            val summary = element.mapNotNull { (it as? JsonPrimitive)?.content }
            return RiskReasons(summary = summary, factors = null)
        }
        return jsonDecoder.json.decodeFromJsonElement(delegate, element)
    }
}


@Serializable
data class RiskFactors(
    @SerialName("knowledgeGaps") val knowledgeGaps: RiskFactor? = null,
    @SerialName("overdueTasks") val overdueTasks: RiskFactor? = null,
    @SerialName("confidenceIA") val confidenceIA: RiskFactor? = null,
    val roadmaps: RiskFactor? = null,
    val engagement: RiskFactor? = null,
)


@Serializable
data class RiskFactor(
    val score: Int = 0,
    val max: Int = 1,
)

// ─── Roadmaps / Rutas de aprendizaje ────────────────────────────────────

@Serializable
data class Roadmap(
    val id: Int = 0,
    val topic: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("totalLevels") val totalLevels: Int = 0,
    @SerialName("currentLevel") val currentLevel: Int = 1,
    val category: String = "",
    val difficulty: String = "",
    @SerialName("estimatedHours") val estimatedHours: Double = 0.0,
    val progress: Int = 0,
    @Serializable(with = StepList::class) val steps: List<RoadmapStep> = emptyList(),
)

/** Progreso real de un roadmap calculado desde sus pasos (el backend no lo envía). */
fun Roadmap.computedProgress(): Int =
    if (steps.isNotEmpty()) (steps.count { it.completed } * 100) / steps.size else progress

@Serializable
data class RoadmapStep(
    val id: Int = 0,
    val title: String = "",
    val skill: String = "",
    val level: Int = 1,
    val completed: Boolean = false,
    val order: Int = 0,
    val practice: List<PracticeQuestion> = emptyList(),
)


@Serializable
data class PracticeQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    @SerialName("correctIndex") val correctIndex: Int = 0,
    val explanation: String = "",
)

@Serializable
data class GenerateRoadmapRequest(
    val topic: String = "",
    val goal: String = "",
    val regenerate: Boolean = false,
)

// ─── Profesor IA ────────────────────────────────────────────────────────

@Serializable
data class ChatRequest(
    @SerialName("conversationId") val conversationId: String? = null,
    @SerialName("teacherId") val teacherId: String? = null,
    val message: String = "",
)

// ─── Análisis de imágenes del tutor IA ─────────────────────────────────

/** Fase 1: contenido que el modelo de visión extrae de una imagen. */
@Serializable
data class ImageContentPhase(
    val titulo: String = "",
    val tipoContenido: String = "",
    val tema: String = "",
    val nivelDificultad: String = "",
    val contenidoPrincipal: String = "",
    val textoDetectado: String = "",
    val elementosClave: List<String> = emptyList(),
    val advertencias: List<String> = emptyList(),
)

@Serializable
data class ImagePhaseMeta(
    val fase: Int = 1,
    val modelo: String = "",
    @SerialName("duracionMs") val duracionMs: Long = 0,
    @SerialName("tokensUsados") val tokensUsados: Int? = null,
)

/** Respuesta de POST /ai/images/content. */
@Serializable
data class ImageAnalysisResponse(
    val fase: Int = 1,
    val contenido: ImageContentPhase = ImageContentPhase(),
    val meta: ImagePhaseMeta? = null,
)

/** Cuerpo de POST /ai/chat/image (la imagen ya fue analizada por visión). */
@Serializable
data class ChatWithImageRequest(
    @SerialName("conversationId") val conversationId: String? = null,
    @SerialName("teacherId") val teacherId: String? = null,
    val message: String = "",
    val filename: String = "",
    val contenido: ImageContentPhase = ImageContentPhase(),
    val nivel: String = "default",
)

@Serializable
data class TeacherProfile(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val subjects: List<String> = emptyList(),
    @SerialName("teachingStyle") val teachingStyle: String = "",
    @SerialName("difficultyLevel") val difficultyLevel: String = "",
    val active: Boolean = true,
)

@Serializable
data class TeacherProfilesResponse(
    val profiles: List<TeacherProfile> = emptyList(),
)

@Serializable
data class ConversationMessage(
    val role: String = "",
    val content: String = "",
)

@Serializable
data class ConversationMessagesResponse(
    val messages: List<ConversationMessage> = emptyList(),
)

/** Conversación del tutor IA (GET /ai/conversations). */
@Serializable
data class Conversation(
    @SerialName("_id") val id: String = "",
    val title: String = "",
    @SerialName("lastMessageAt") val lastMessageAt: String = "",
    @SerialName("messageCount") val messageCount: Int = 0,
    @SerialName("isPinned") val isPinned: Boolean = false,
    @SerialName("isArchived") val isArchived: Boolean = false,
)

@Serializable
data class ConversationsResponse(
    val conversations: List<Conversation> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class ChatResponse(
    @SerialName("conversationId") val conversationId: String = "",
    /** El endpoint real devuelve la respuesta en `reply` (la doc mostraba `message`). */
    val reply: String = "",
)

// ─── Metas de aprendizaje (IA) ──────────────────────────────────────────

@Serializable
data class AiGoal(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "ACTIVA",
    val progress: Int = 0,
    @SerialName("targetDate") val targetDate: String = "",
    val subject: String = "",
    // El backend anida la materia en computedProgress.subject (no en el top-level)
    @SerialName("computedProgress") val computedProgress: GoalProgress? = null,
) {
    val subjectLabel: String
        get() = subject.ifBlank { computedProgress?.subject.orEmpty() }
}


@Serializable
data class GoalProgress(
    val subject: String = "",
)


@Serializable
data class GoalsResponse(
    val goals: List<AiGoal> = emptyList(),
)


@Serializable
data class CreateGoalRequest(
    val title: String = "",
    val description: String = "",
    @SerialName("targetDate") val targetDate: String = "",
)

// ─── Recursos generados por IA (quizzes, resúmenes...) ──────────────────

@Serializable
data class AiResource(
    val id: String = "",
    val subject: String = "",
    val type: String = "QUIZ",
    val title: String = "",
    @Serializable(with = StringOrNumberSerializer::class) val difficulty: String = "",
    val completed: Boolean = false,
    @SerialName("resultScore") val resultScore: Double? = null,
    @SerialName("resultCorrect") val resultCorrect: Int? = null,
    @SerialName("resultTotal") val resultTotal: Int? = null,
    @SerialName("createdAt") val createdAt: String = "",
    val content: QuizContent? = null,
)


@Serializable
data class QuizContent(
    val type: String = "QUIZ",
    val topic: String = "",
    val subject: String = "",
    @Serializable(with = QuizList::class) val quiz: List<QuizQuestion> = emptyList(),
)

@Serializable
data class QuizQuestion(
    val question: String = "",
    val choices: List<String> = emptyList(),
    val answer: String = "",
    val explanation: String = "",
    val difficulty: String = "medium",
)

@Serializable
data class CompleteResourceRequest(
    @SerialName("resultScore") val resultScore: Double,
    @SerialName("resultCorrect") val resultCorrect: Int,
    @SerialName("resultTotal") val resultTotal: Int,
)

@Serializable
data class GenerateQuizRequest(
    val topic: String = "",
    val difficulty: String = "INTERMEDIATE",
    val count: Int = 5,
)

@Serializable
data class ExplainAnswerRequest(
    val question: String = "",
    val choices: List<String> = emptyList(),
    val correctAnswer: String = "",
    val topic: String = "",
)

@Serializable
data class ExplainAnswerResponse(val explanation: String = "")

@Serializable
data class ResourceResponse(val resource: AiResource? = null)

@Serializable
data class ResourceListResponse(val resources: List<AiResource> = emptyList())

@Serializable
data class NotificationsResponse(
    val notifications: List<NotificationItem> = emptyList(),
    val total: Int = 0,
    val unreadCount: Int = 0,
    val page: Int = 1,
    val limit: Int = 50,
)

// ─── Brechas de conocimiento ────────────────────────────────────────────

@Serializable
data class KnowledgeGap(
    val id: String = "",
    val subject: String = "",
    val topic: String = "",
    val confidence: Int = 0,
    val status: String = "OPEN",
    val evidences: List<String> = emptyList(),
)


// ─── CV / Hoja de vida ──────────────────────────────────────────────────

@Serializable
data class ResumeMe(
    val id: Int = 0,
    val titulo: String = "",
    val resumen: String = "",
    val slug: String = "",
    @Serializable(with = ExperienceList::class) val experiences: List<ResumeExperience> = emptyList(),
    @Serializable(with = EducationList::class) val educations: List<ResumeEducation> = emptyList(),
    @Serializable(with = ProjectList::class) val projects: List<ResumeProject> = emptyList(),
    @Serializable(with = CertificateList::class) val certificates: List<ResumeCertificate> = emptyList(),
    @Serializable(with = LanguageList::class) val languages: List<ResumeLanguage> = emptyList(),
)

@Serializable
data class ResumeExperience(
    val id: Int = 0,
    val company: String = "",
    val position: String = "",
    val description: String = "",
    @SerialName("startDate") val startDate: String = "",
    @SerialName("endDate") val endDate: String = "",
    @SerialName("isCurrent") val isCurrent: Boolean = false,
)

@Serializable
data class ResumeEducation(
    val id: Int = 0,
    val institution: String = "",
    val degree: String = "",
    @SerialName("startDate") val startDate: String = "",
    @SerialName("endDate") val endDate: String = "",
)

@Serializable
data class ResumeProject(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val url: String = "",
)

@Serializable
data class ResumeCertificate(
    val id: Int = 0,
    val name: String = "",
    val issuer: String = "",
    val year: String = "",
)

@Serializable
data class ResumeLanguage(
    val id: Int = 0,
    val name: String = "",
    val level: String = "",
)

// ─── Laboratorio / Sandbox ──────────────────────────────────────────────

@Serializable
data class SandboxStats(
    val resolved: Int = 0,
    val accuracy: Int = 0,
    val streak: Int = 0,
    val totalAttempts: Int = 0,
)

@Serializable
data class SandboxExercise(
    val id: Int = 0,
    val title: String = "",
    val language: String = "",
    val difficulty: String = "BASIC",
    val description: String = "",
    val solved: Boolean = false,
)


// ─── Perfil ─────────────────────────────────────────────────────────────

@Serializable
data class ProfileAcademic(
    val universidad: String = "",
    val carrera: String = "",
    val facultad: String = "",
    @SerialName("semestreActual") val semestreActual: Int = 0,
    val promedio: Double = 0.0,
    val modalidad: String = "",
)

// ─── Calendario ─────────────────────────────────────────────────────────

@Serializable
data class CalendarSubject(
    val id: Int = 0,
    val nombre: String = "",
    val color: String = "",
)

@Serializable
data class CalendarEvent(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    @SerialName("startAt") val startAt: String = "",
    @SerialName("endAt") val endAt: String = "",
    @SerialName("allDay") val allDay: Boolean = false,
    val color: String = "",
    val type: String = "",
    val subject: CalendarSubject? = null,
)

/** Tareas que el calendario convierte en eventos (id viene como "task-<id>"). */
@Serializable
data class CalendarTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("startAt") val startAt: String = "",
    @SerialName("endAt") val endAt: String = "",
    @SerialName("allDay") val allDay: Boolean = false,
    val color: String = "",
    val type: String = "",
    val subject: CalendarSubject? = null,
    @SerialName("taskStatus") val taskStatus: String = "",
)

@Serializable
data class CalendarData(
    val events: List<CalendarEvent> = emptyList(),
    val tasks: List<CalendarTask> = emptyList(),
)


/** Cuerpo para crear/editar un evento (POST/PATCH /calendar/events). */
@Serializable
data class CalendarEventRequest(
    val title: String = "",
    val description: String = "",
    @SerialName("startAt") val startAt: String = "",
    @SerialName("endAt") val endAt: String = "",
    @SerialName("allDay") val allDay: Boolean = false,
    val color: String = "",
    val type: String = "EVENT",
    @SerialName("subjectId") val subjectId: Int? = null,
)

// ─── Temporizador de estudio ────────────────────────────────────────────

@Serializable
data class StudyTimerStats(
    @SerialName("totalHours") val totalHours: String = "0",
)

@Serializable
data class StudySession(
    val id: Int = 0,
    @SerialName("completedAt") val completedAt: String = "",
    @SerialName("durationMinutes") val durationMinutes: Int = 0,
    val technique: String = "",
    @SerialName("xpEarned") val xpEarned: Int = 0,
    @SerialName("subjectId") val subjectId: Int? = null,
)


/** Respuesta paginada de GET /study-timer/sessions: { sessions, total, page, limit }. */
@Serializable
data class StudySessionPage(
    val sessions: List<StudySession> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
)
