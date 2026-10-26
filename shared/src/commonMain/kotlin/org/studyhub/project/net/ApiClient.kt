package org.studyhub.project.net

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

/** URL de producción del backend (ver API-DOCS.md del repo study-hub-backend). */
const val BASE_URL = "https://study-hub-backend-sigma.vercel.app"

class ApiException(message: String) : Exception(message)

/**
 * Cliente HTTP de StudyHub. Usa la URL de producción y adjunta el JWT
 * guardado en [TokenStore] a cada petición protegida.
 */
class ApiClient(engine: HttpClientEngine? = null) {

    // OJO: `json` DEBE inicializarse ANTES que `client`: `configure()` lo usa al crear el
    // HttpClient, y si está pendiente de inicializar lanza NPE al arrancar (el login fallaba
    // siempre con "No se pudo conectar" aunque el backend estuviera bien).
    /** Misma config que el ContentNegotiation; se usa para cachear y re-hidratar respuestas. */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        // El backend envía null en campos opcionales (codigo, profesor, salon…):
        // sin esto, el parseo de una materia incompleta rompía toda la lista.
        coerceInputValues = true
    }

    private val client = if (engine != null) {
        HttpClient(engine) { configure() }
    } else {
        HttpClient { configure() }
    }

    /** Serializa con un serializer explícito (el miembro Json.encodeToString oculta la extensión reified). */
    private fun <T> jsonString(serializer: kotlinx.serialization.KSerializer<T>, value: T): String =
        json.encodeToString(serializer, value)

    private fun HttpClientConfig<*>.configure() {
        expectSuccess = true
        install(ContentNegotiation) {
            json(this@ApiClient.json)
        }
        install(HttpTimeout) {
            // Los endpoints de IA (chat, generar quiz/ruta) tardan 20-40s en generar.
            // socketTimeout cubre lecturas sin bytes (OkHttp por defecto corta a los 10s).
            requestTimeoutMillis = 120_000
            socketTimeoutMillis = 110_000
            connectTimeoutMillis = 15_000
        }
        defaultRequest {
            url(BASE_URL)
        }
    }

    private fun auth() = TokenStore.load()?.let { "Bearer $it" }

    /**
     * Ejecuta una petición HTTP reintentando una vez ante fallos de red o timeouts.
     * El backend corre en serverless (Vercel): a veces el primer intento falla por cold
     * start o un error transitorio (`FUNCTION_INVOCATION_FAILED`); un reintento lo absorbe.
     * No reintenta errores 4xx/5xx (el servidor respondió) ni ApiException.
     */
    private suspend fun <T> withNetworkRetry(block: suspend () -> T): T {
        var lastError: Exception? = null
        repeat(2) {
            try {
                return block()
            } catch (e: ResponseException) {
                throw e
            } catch (e: ApiException) {
                throw e
            } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
                logError("ApiClient", "Intento falló (timeout), reintentando: ${e.message}")
                lastError = e
            } catch (e: io.ktor.utils.io.errors.IOException) {
                logError("ApiClient", "Intento falló (red), reintentando: ${e::class.simpleName}: ${e.message}")
                lastError = e
            }
            delay(800)
        }
        throw lastError ?: ApiException("No se pudo conectar con el servidor")
    }

    /**
     * GET con soporte offline: guarda la respuesta en [OfflineCache] y, si la red falla,
     * devuelve la última copia guardada (marcando el estado como sin conexión).
     */
    private suspend inline fun <reified T> request(
        cacheKey: String? = null,
        nullOnEmpty: Boolean = false,
        crossinline block: suspend () -> HttpResponse,
    ): T {
        var gotResponse = false
        try {
            val response = withNetworkRetry { block() }
            val text = response.bodyAsText()
            gotResponse = true
            if (nullOnEmpty && text.isBlank()) {
                // El backend responde 200 con cuerpo vacío (null) cuando aún no hay datos
                // (ej. GET /risk sin análisis previo).
                @Suppress("UNCHECKED_CAST")
                return null as T
            }
            val parsed: T = json.decodeFromString(text)
            if (cacheKey != null) OfflineCache.save(cacheKey, text)
            OfflineState.setOnline(true)
            flushPending()
            return parsed
        } catch (e: ResponseException) {
            val backendMessage = runCatching { e.response.body<ErrorBody>().message }.getOrNull()
            logError("ApiClient", "HTTP ${e.response.status.value} en $cacheKey: ${e.message}")
            throw ApiException(
                backendMessage?.takeIf { it.isNotBlank() }
                    ?: "Error ${e.response.status.value}",
            )
        } catch (e: ApiException) {
            throw e
        } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
            logError("ApiClient", "Timeout en request ($cacheKey): ${e.message}")
            throw ApiException("El servidor tardó demasiado en responder")
        } catch (e: io.ktor.utils.io.errors.IOException) {
            logError("ApiClient", "Error de red en request ($cacheKey): ${e::class.simpleName}: ${e.message}")
            if (!gotResponse) {
                val cached = cacheKey?.let { OfflineCache.load(it) }
                if (cached != null) {
                    OfflineState.setOnline(false)
                    return json.decodeFromString(cached)
                }
            }
            throw ApiException("No se pudo conectar con el servidor")
        }
    }

    /**
     * Escritura (POST/PUT/DELETE) con soporte offline: si la red falla, encola la acción
     * en [PendingQueue] y ejecuta [onOffline] (actualización optimista de la caché local).
     */
    private suspend inline fun <reified T> mutate(
        offlineMessage: String = "Sin conexión. Revisa tu internet e inténtalo de nuevo.",
        crossinline block: suspend () -> HttpResponse,
        crossinline onOffline: () -> Unit = {},
    ): T {
        try {
            val response = block()
            val text = response.bodyAsText()
            val parsed: T = json.decodeFromString(text)
            OfflineState.setOnline(true)
            flushPending()
            return parsed
        } catch (e: ResponseException) {
            val backendMessage = runCatching { e.response.body<ErrorBody>().message }.getOrNull()
            logError("ApiClient", "HTTP ${e.response.status.value}: ${e.message}")
            throw ApiException(
                backendMessage?.takeIf { it.isNotBlank() }
                    ?: "Error ${e.response.status.value}",
            )
        } catch (e: ApiException) {
            throw e
        } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
            logError("ApiClient", "Timeout en mutate: ${e.message}")
            throw ApiException("El servidor tardó demasiado en responder")
        } catch (e: io.ktor.utils.io.errors.IOException) {
            logError("ApiClient", "Error de red en mutate: ${e::class.simpleName}: ${e.message}")
            OfflineState.setOnline(false)
            onOffline()
            throw ApiException(offlineMessage)
        }
    }

    /** Reproduce la cola de acciones pendientes en orden; ante un fallo de red detiene y conserva el resto. */
    suspend fun flushPending() {
        val actions = PendingQueue.all()
        if (actions.isEmpty()) return
        val token = TokenStore.load() ?: run {
            PendingQueue.clear()
            return
        }
        var remaining = actions
        for (action in actions) {
            try {
                client.request(action.url) {
                    method = HttpMethod.parse(action.method)
                    header("Authorization", "Bearer $token")
                    if (action.body != null) {
                        contentType(ContentType.Application.Json)
                        setBody(action.body)
                    }
                }
                remaining = remaining.drop(1)
            } catch (_: Exception) {
                break
            }
        }
        PendingQueue.replace(remaining)
        if (remaining.isEmpty()) OfflineState.setOnline(true)
    }

    /** Actualiza optimísticamente la copia local (caché) del detalle de una materia. */
    private fun updateCachedDetail(subjectId: Int, transform: (SubjectDetail) -> SubjectDetail) {
        val cached = OfflineCache.load("subjects/$subjectId") ?: return
        val detail = runCatching { json.decodeFromString<SubjectDetail>(cached) }.getOrNull() ?: return
        OfflineCache.save("subjects/$subjectId", jsonString(SubjectDetail.serializer(), transform(detail)))
    }

    // ─── Auth (sin caché: requiere red) ──────────────────────────────────
    suspend fun login(email: String, password: String): AuthResponse =
        request {
            client.post("/auth/login") {
                // El login no debe tardar 2 min: si el serverless tarda, se reintenta y se falla rápido
                timeout { requestTimeoutMillis = 30_000 }
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email.trim(), password))
            }
        }

    suspend fun register(nombre: String, apellido: String, email: String, password: String): AuthResponse =
        request {
            client.post("/auth/register") {
                timeout { requestTimeoutMillis = 30_000 }
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(nombre.trim(), apellido.trim(), email.trim(), password, password))
            }
        }

    suspend fun logout() {
        try {
            client.post("/auth/logout") { header("Authorization", auth()) }
        } catch (_: Exception) {
            // El logout local es lo importante
        }
        TokenStore.clearAll()
        OfflineState.setOnline(true)
    }

    suspend fun verifyEmail(email: String, code: String): String {
        val response: MessageResponse = request {
            client.post("/auth/verify-email") {
                contentType(ContentType.Application.Json)
                setBody(VerifyEmailRequest(email.trim(), code.trim()))
            }
        }
        return response.message
    }

    suspend fun resendCode(email: String): String {
        val response: MessageResponse = request {
            client.post("/auth/resend-code") {
                contentType(ContentType.Application.Json)
                setBody(ResendCodeRequest(email.trim()))
            }
        }
        return response.message
    }

    suspend fun forgotPassword(email: String): String {
        val response: MessageResponse = request {
            client.post("/auth/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody(ForgotPasswordRequest(email.trim()))
            }
        }
        return response.message
    }

    suspend fun resetPassword(token: String, password: String): String {
        val response: MessageResponse = request {
            client.post("/auth/reset-password") {
                contentType(ContentType.Application.Json)
                setBody(ResetPasswordRequest(token.trim(), password))
            }
        }
        return response.message
    }

    // ─── Dashboard ───────────────────────────────────────────────────────
    suspend fun dashboard(): DashboardSummary = request("dashboard") {
        client.get("/dashboard/summary") { header("Authorization", auth()) }
    }

    suspend fun personalInfo(): PersonalInfo = request("profile/personal") {
        client.get("/profile/personal") { header("Authorization", auth()) }
    }

    // ─── Materias ────────────────────────────────────────────────────────
    suspend fun subjects(): List<Subject> = request("subjects") {
        client.get("/subjects") { header("Authorization", auth()) }
    }

    suspend fun createSubject(
        nombre: String,
        codigo: String? = null,
        profesor: String? = null,
        salon: String? = null,
        creditos: Int? = null,
        color: String,
        descripcion: String? = null,
    ): Subject = mutate(
        offlineMessage = "Sin conexión: la materia se guardó y se sincronizará después",
        block = {
            client.post("/subjects") {
                header("Authorization", auth())
                contentType(ContentType.Application.Json)
                setBody(
                    CreateSubjectRequest(
                        nombre = nombre,
                        codigo = codigo,
                        profesor = profesor,
                        salon = salon,
                        creditos = creditos,
                        color = color,
                        descripcion = descripcion,
                    ),
                )
            }
        },
        onOffline = {
            PendingQueue.add(
                PendingAction(
                    method = "POST",
                    url = "/subjects",
                    body = jsonString(
                        CreateSubjectRequest.serializer(),
                        CreateSubjectRequest(
                            nombre = nombre,
                            codigo = codigo,
                            profesor = profesor,
                            salon = salon,
                            creditos = creditos,
                            color = color,
                            descripcion = descripcion,
                        ),
                    ),
                ),
            )
            // Optimista: la materia aparece de inmediato en la lista local.
            val cached = OfflineCache.load("subjects")
            if (cached != null) {
                val list = runCatching { json.decodeFromString<List<Subject>>(cached) }.getOrNull() ?: emptyList()
                val temp = Subject(
                    id = -list.size - 1,
                    nombre = nombre,
                    codigo = codigo ?: "",
                    profesor = profesor ?: "",
                    salon = salon ?: "",
                    creditos = creditos ?: 0,
                    color = color,
                    descripcion = descripcion ?: "",
                )
                OfflineCache.save("subjects", jsonString(kotlinx.serialization.builtins.ListSerializer(Subject.serializer()), list + temp))
            }
        },
    )

    suspend fun subjectDetail(id: Int): SubjectDetail = request("subjects/$id") {
        client.get("/subjects/$id") { header("Authorization", auth()) }
    }

    suspend fun createTask(
        subjectId: Int,
        title: String,
        description: String? = null,
        priority: String,
        dueDate: String,
    ): SubjectTask = mutate(
        offlineMessage = "Sin conexión: la tarea se guardó y se sincronizará después",
        block = {
            client.post("/subjects/$subjectId/tasks") {
                header("Authorization", auth())
                contentType(ContentType.Application.Json)
                setBody(
                    CreateTaskRequest(
                        title = title,
                        description = description,
                        priority = priority,
                        dueDate = dueDate,
                    ),
                )
            }
        },
        onOffline = {
            PendingQueue.add(
                PendingAction(
                    method = "POST",
                    url = "/subjects/$subjectId/tasks",
                    body = jsonString(CreateTaskRequest.serializer(), CreateTaskRequest(title, description, priority, dueDate)),
                ),
            )
            updateCachedDetail(subjectId) { d ->
                d.copy(
                    tasks = d.tasks + SubjectTask(
                        id = -(d.tasks.size + 1),
                        title = title,
                        description = description ?: "",
                        dueDate = dueDate,
                        priority = priority,
                        status = "PENDING",
                        subjectId = subjectId,
                    ),
                )
            }
        },
    )

    suspend fun createNote(
        subjectId: Int,
        title: String,
        content: String,
        isPinned: Boolean? = null,
    ): Note = mutate(
        offlineMessage = "Sin conexión: el apunte se guardó y se sincronizará después",
        block = {
            client.post("/subjects/$subjectId/notes") {
                header("Authorization", auth())
                contentType(ContentType.Application.Json)
                setBody(
                    CreateNoteRequest(
                        title = title,
                        content = content,
                        isPinned = isPinned,
                    ),
                )
            }
        },
        onOffline = {
            PendingQueue.add(
                PendingAction(
                    method = "POST",
                    url = "/subjects/$subjectId/notes",
                    body = jsonString(CreateNoteRequest.serializer(), CreateNoteRequest(title, content, isPinned)),
                ),
            )
            updateCachedDetail(subjectId) { d ->
                d.copy(
                    notes = d.notes + Note(
                        id = -(d.notes.size + 1),
                        title = title,
                        content = content,
                        pinned = isPinned ?: false,
                    ),
                )
            }
        },
    )

    suspend fun createSchedule(
        subjectId: Int,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        classroom: String? = null,
    ): Schedule = mutate(
        offlineMessage = "Sin conexión: la clase se guardó y se sincronizará después",
        block = {
            client.post("/subjects/$subjectId/schedules") {
                header("Authorization", auth())
                contentType(ContentType.Application.Json)
                setBody(
                    CreateScheduleRequest(
                        dayOfWeek = dayOfWeek,
                        startTime = startTime,
                        endTime = endTime,
                        classroom = classroom,
                    ),
                )
            }
        },
        onOffline = {
            PendingQueue.add(
                PendingAction(
                    method = "POST",
                    url = "/subjects/$subjectId/schedules",
                    body = jsonString(CreateScheduleRequest.serializer(), CreateScheduleRequest(dayOfWeek, startTime, endTime, classroom)),
                ),
            )
            updateCachedDetail(subjectId) { d ->
                d.copy(
                    schedules = d.schedules + Schedule(
                        id = -(d.schedules.size + 1),
                        dayOfWeek = dayOfWeek,
                        startTime = startTime,
                        endTime = endTime,
                        classroom = classroom ?: "",
                    ),
                )
            }
        },
    )

    suspend fun deleteSchedule(subjectId: Int, scheduleId: Int): MessageResponse = mutate(
        offlineMessage = "Sin conexión: la eliminación se guardó y se sincronizará después",
        block = {
            client.delete("/subjects/$subjectId/schedules/$scheduleId") { header("Authorization", auth()) }
        },
        onOffline = {
            PendingQueue.add(PendingAction("DELETE", "/subjects/$subjectId/schedules/$scheduleId"))
            updateCachedDetail(subjectId) { d -> d.copy(schedules = d.schedules.filterNot { it.id == scheduleId }) }
        },
    )

    suspend fun updateTask(
        subjectId: Int,
        taskId: Int,
        title: String,
        description: String? = null,
        priority: String,
        dueDate: String,
    ): SubjectTask = mutate(
        offlineMessage = "Sin conexión: los cambios se guardaron y se sincronizarán después",
        block = {
            client.put("/subjects/$subjectId/tasks/$taskId") {
                header("Authorization", auth())
                contentType(ContentType.Application.Json)
                setBody(
                    UpdateTaskRequest(
                        title = title,
                        description = description,
                        priority = priority,
                        dueDate = dueDate,
                    ),
                )
            }
        },
        onOffline = {
            PendingQueue.add(
                PendingAction(
                    method = "PUT",
                    url = "/subjects/$subjectId/tasks/$taskId",
                    body = jsonString(UpdateTaskRequest.serializer(), UpdateTaskRequest(title, description, priority, dueDate)),
                ),
            )
            updateCachedDetail(subjectId) { d ->
                d.copy(
                    tasks = d.tasks.map { t ->
                        if (t.id == taskId) t.copy(title = title, description = description ?: "", dueDate = dueDate, priority = priority) else t
                    },
                )
            }
        },
    )

    suspend fun deleteTask(subjectId: Int, taskId: Int): MessageResponse = mutate(
        offlineMessage = "Sin conexión: la eliminación se guardó y se sincronizará después",
        block = {
            client.delete("/subjects/$subjectId/tasks/$taskId") { header("Authorization", auth()) }
        },
        onOffline = {
            PendingQueue.add(PendingAction("DELETE", "/subjects/$subjectId/tasks/$taskId"))
            updateCachedDetail(subjectId) { d -> d.copy(tasks = d.tasks.filterNot { it.id == taskId }) }
        },
    )

    suspend fun updateNote(
        subjectId: Int,
        noteId: Int,
        title: String,
        content: String,
        isPinned: Boolean? = null,
    ): Note = mutate(
        offlineMessage = "Sin conexión: los cambios se guardaron y se sincronizarán después",
        block = {
            client.put("/subjects/$subjectId/notes/$noteId") {
                header("Authorization", auth())
                contentType(ContentType.Application.Json)
                setBody(
                    UpdateNoteRequest(
                        title = title,
                        content = content,
                        isPinned = isPinned,
                    ),
                )
            }
        },
        onOffline = {
            PendingQueue.add(
                PendingAction(
                    method = "PUT",
                    url = "/subjects/$subjectId/notes/$noteId",
                    body = jsonString(UpdateNoteRequest.serializer(), UpdateNoteRequest(title, content, isPinned)),
                ),
            )
            updateCachedDetail(subjectId) { d ->
                d.copy(
                    notes = d.notes.map { n ->
                        if (n.id == noteId) n.copy(title = title, content = content, pinned = isPinned ?: n.pinned) else n
                    },
                )
            }
        },
    )

    suspend fun deleteNote(subjectId: Int, noteId: Int): MessageResponse = mutate(
        offlineMessage = "Sin conexión: la eliminación se guardó y se sincronizará después",
        block = {
            client.delete("/subjects/$subjectId/notes/$noteId") { header("Authorization", auth()) }
        },
        onOffline = {
            PendingQueue.add(PendingAction("DELETE", "/subjects/$subjectId/notes/$noteId"))
            updateCachedDetail(subjectId) { d -> d.copy(notes = d.notes.filterNot { it.id == noteId }) }
        },
    )

    suspend fun togglePinNote(subjectId: Int, noteId: Int): Note = mutate(
        offlineMessage = "Sin conexión: el cambio se guardó y se sincronizará después",
        block = {
            client.post("/subjects/$subjectId/notes/$noteId/pin") { header("Authorization", auth()) }
        },
        onOffline = {
            PendingQueue.add(PendingAction("POST", "/subjects/$subjectId/notes/$noteId/pin"))
            updateCachedDetail(subjectId) { d ->
                d.copy(notes = d.notes.map { n -> if (n.id == noteId) n.copy(pinned = !n.pinned) else n })
            }
        },
    )

    suspend fun toggleTask(subjectId: Int, taskId: Int): SubjectTask = mutate(
        offlineMessage = "Sin conexión: el cambio se guardó y se sincronizará después",
        block = {
            client.post("/subjects/$subjectId/tasks/$taskId/toggle") { header("Authorization", auth()) }
        },
        onOffline = {
            PendingQueue.add(PendingAction("POST", "/subjects/$subjectId/tasks/$taskId/toggle"))
            updateCachedDetail(subjectId) { d ->
                d.copy(
                    tasks = d.tasks.map { t ->
                        if (t.id == taskId) t.copy(status = if (t.status == "COMPLETED") "PENDING" else "COMPLETED") else t
                    },
                )
            }
        },
    )

    // ─── Gamificación ────────────────────────────────────────────────────
    suspend fun gamification(): GamificationProgress = request("gamification") {
        client.get("/gamification/progress") { header("Authorization", auth()) }
    }

    // ─── Notificaciones ──────────────────────────────────────────────────
    // El backend responde paginado: { notifications: [...], total, unreadCount, page, limit }
    suspend fun notifications(): List<NotificationItem> {
        val response: NotificationsResponse = request("notifications") {
            client.get("/notifications") { header("Authorization", auth()) }
        }
        return response.notifications
    }

    suspend fun unreadCount(): Int = request("notifications/unread") {
        client.get("/notifications/unread-count") { header("Authorization", auth()) }
    }

    suspend fun markRead(id: Int) {
        client.patch("/notifications/$id/read") { header("Authorization", auth()) }
    }

    suspend fun markAllRead() {
        client.patch("/notifications/read-all") { header("Authorization", auth()) }
    }

    // ─── Riesgo académico ────────────────────────────────────────────────
    /** Último análisis de riesgo; null si el backend aún no generó uno (200 con cuerpo vacío). */
    suspend fun risk(): RiskAnalysis? = request("risk", nullOnEmpty = true) {
        client.get("/risk") { header("Authorization", auth()) }
    }

    suspend fun riskHistory(): List<RiskAnalysis> = request("risk/history") {
        client.get("/risk/history") { header("Authorization", auth()) }
    }

    // ─── Rutas de aprendizaje ────────────────────────────────────────────
    suspend fun roadmaps(): List<Roadmap> = request("roadmaps") {
        client.get("/roadmaps") { header("Authorization", auth()) }
    }

    suspend fun roadmapDetail(id: Int): Roadmap = request("roadmaps/$id") {
        client.get("/roadmaps/$id") { header("Authorization", auth()) }
    }

    suspend fun generateRoadmap(topic: String, goal: String): Roadmap = mutate(block = {
        client.post("/roadmaps/generate") {
            header("Authorization", auth())
            contentType(ContentType.Application.Json)
            setBody(GenerateRoadmapRequest(topic.trim(), goal.trim(), regenerate = false))
        }
    })

    suspend fun completeStep(stepId: Int) {
        client.patch("/roadmaps/steps/$stepId/complete") { header("Authorization", auth()) }
    }

    // ─── Profesor IA ─────────────────────────────────────────────────────
    /** Perfiles de profesor IA disponibles (GET /ai/teacher-profiles). */
    suspend fun teacherProfiles(): List<TeacherProfile> {
        val resp: TeacherProfilesResponse = request("ai/teacher-profiles") {
            client.get("/ai/teacher-profiles") { header("Authorization", auth()) }
        }
        return resp.profiles
    }

    /** Mensajes guardados de una conversación (para restaurar el chat entre sesiones). */
    suspend fun conversationMessages(conversationId: String): List<ConversationMessage> {
        val resp: ConversationMessagesResponse = request("ai/conversations/$conversationId/messages") {
            client.get("/ai/conversations/$conversationId/messages") { header("Authorization", auth()) }
        }
        return resp.messages
    }

    /** Historial de conversaciones del tutor IA, más recientes primero (GET /ai/conversations). */
    suspend fun conversations(): List<Conversation> {
        val resp: ConversationsResponse = request("ai/conversations") {
            client.get("/ai/conversations?limit=50&sortBy=lastMessageAt&order=desc") { header("Authorization", auth()) }
        }
        return resp.conversations
    }

    /** Elimina una conversación y sus mensajes (DELETE /ai/conversations/:id). */
    suspend fun deleteConversation(conversationId: String) {
        client.delete("/ai/conversations/$conversationId") { header("Authorization", auth()) }
    }

    /** Envía un mensaje al tutor IA con un reintento ante fallos de red transitorios. */
    suspend fun chat(message: String, conversationId: String? = null, teacherId: String? = null): ChatResponse {
        var lastError: Exception? = null
        repeat(2) {
            try {
                return chatOnce(message, conversationId, teacherId)
            } catch (e: ApiException) {
                // Errores del servidor (4xx/5xx): no reintentar
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: ApiException("No se pudo conectar con el servidor")
    }

    private suspend fun chatOnce(message: String, conversationId: String?, teacherId: String?): ChatResponse = mutate(block = {
        client.post("/ai/chat") {
            header("Authorization", auth())
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(conversationId, teacherId, message))
        }
    })

    // ─── Tutor IA: imágenes ──────────────────────────────────────────────
    /**
     * Fase 1 del pipeline de visión: sube la imagen y obtiene el contenido
     * estructurado que luego se usa para conversar con el tutor.
     * El backend solo acepta JPEG/PNG/WebP de hasta 6 MB.
     */
    suspend fun analyzeImage(
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        idioma: String = "es",
        enfoque: String? = null,
    ): ImageAnalysisResponse = mutate(block = {
        withNetworkRetry {
            client.post("/ai/images/content") {
                header("Authorization", auth())
                parameter("idioma", idioma)
                if (!enfoque.isNullOrBlank()) parameter("enfoque", enfoque)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "image",
                                bytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, mimeType)
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "filename=\"${filename.replace("\"", "")}\"",
                                    )
                                },
                            )
                        },
                    ),
                )
            }
        }
    })

    /** Conversa con el tutor a partir del contenido ya extraído de una imagen. */
    suspend fun chatWithImage(
        message: String,
        filename: String,
        contenido: ImageContentPhase,
        conversationId: String? = null,
        teacherId: String? = null,
        nivel: String = "default",
    ): ChatResponse = mutate(block = {
        withNetworkRetry {
            client.post("/ai/chat/image") {
                header("Authorization", auth())
                contentType(ContentType.Application.Json)
                setBody(
                    ChatWithImageRequest(
                        conversationId = conversationId,
                        teacherId = teacherId,
                        message = message,
                        filename = filename,
                        contenido = contenido,
                        nivel = nivel,
                    ),
                )
            }
        }
    })

    // ─── Metas de aprendizaje ────────────────────────────────────────────
    // El backend responde { goals: [...] } (no un array plano)
    suspend fun goals(): List<AiGoal> {
        val response: GoalsResponse = request("ai/goals") {
            client.get("/ai/goals") { header("Authorization", auth()) }
        }
        return response.goals
    }

    suspend fun createGoal(title: String, description: String, targetDate: String): AiGoal = mutate(block = {
        client.post("/ai/goals") {
            header("Authorization", auth())
            contentType(ContentType.Application.Json)
            setBody(CreateGoalRequest(title, description, targetDate))
        }
    })

    // ─── Brechas de conocimiento ─────────────────────────────────────────
    suspend fun knowledgeGaps(): List<KnowledgeGap> = request("ai/knowledge-gaps") {
        client.get("/ai/knowledge-gaps") { header("Authorization", auth()) }
    }

    // ─── Recursos generados por IA (quizzes) ─────────────────────────────
    // El backend responde { resources: [...] } (no un array plano)
    suspend fun aiResources(): List<AiResource> {
        val response: ResourceListResponse = request("ai/resources") {
            client.get("/ai/resources") { header("Authorization", auth()) }
        }
        return response.resources
    }

    suspend fun generateQuiz(topic: String, difficulty: String, count: Int = 5): AiResource {
        val response: ResourceResponse = mutate(block = {
            client.post("/ai/resources/quiz") {
                header("Authorization", auth())
                contentType(ContentType.Application.Json)
                setBody(GenerateQuizRequest(topic, difficulty, count))
            }
        })
        return response.resource ?: AiResource()
    }

    suspend fun aiResourceDetail(id: String): AiResource {
        val response: ResourceResponse = request("ai/resources/$id") {
            client.get("/ai/resources/$id") { header("Authorization", auth()) }
        }
        return response.resource ?: AiResource()
    }

    suspend fun completeResource(id: String, correct: Int, total: Int) {
        val score = if (total > 0) correct.toDouble() / total else 0.0
        client.patch("/ai/resources/$id/complete") {
            header("Authorization", auth())
            contentType(ContentType.Application.Json)
            setBody(CompleteResourceRequest(resultScore = score, resultCorrect = correct, resultTotal = total))
        }
    }

    suspend fun explainAnswer(question: String, choices: List<String>, correctAnswer: String, topic: String = ""): String {
        val response: ExplainAnswerResponse = mutate(block = {
            client.post("/ai/explain-answer") {
                header("Authorization", auth())
                contentType(ContentType.Application.Json)
                setBody(ExplainAnswerRequest(question, choices, correctAnswer, topic))
            }
        })
        return response.explanation
    }

    // ─── CV / Hoja de vida ───────────────────────────────────────────────
    suspend fun resume(): ResumeMe = request("resume/me") {
        client.get("/resume/me") { header("Authorization", auth()) }
    }

    // ─── Laboratorio / Sandbox ───────────────────────────────────────────
    suspend fun sandboxStats(): SandboxStats = request("sandbox/stats") {
        client.get("/sandbox/stats") { header("Authorization", auth()) }
    }

    suspend fun sandboxExercises(): List<SandboxExercise> = request("sandbox/exercises") {
        client.get("/sandbox/exercises") { header("Authorization", auth()) }
    }

    // ─── Calendario / Eventos ──────────────────────────────────────
    suspend fun calendarEvents(): CalendarData = request("calendar/events") {
        client.get("/calendar/events") { header("Authorization", auth()) }
    }

    suspend fun upcomingExams(): List<CalendarEvent> = request("calendar/exams") {
        client.get("/calendar/exams/upcoming") { header("Authorization", auth()) }
    }

    suspend fun createCalendarEvent(request: CalendarEventRequest): CalendarEvent = mutate(
        offlineMessage = "Sin conexión: el evento se guardará cuando tengas internet",
        block = {
            client.post("/calendar/events") {
                header("Authorization", auth())
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        },
    )

    suspend fun updateCalendarEvent(id: Int, request: CalendarEventRequest): CalendarEvent = mutate(
        offlineMessage = "Sin conexión: los cambios se guardarán cuando tengas internet",
        block = {
            client.patch("/calendar/events/$id") {
                header("Authorization", auth())
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        },
    )

    suspend fun deleteCalendarEvent(id: Int): MessageResponse = mutate(
        offlineMessage = "Sin conexión: el evento se eliminará cuando tengas internet",
        block = {
            client.delete("/calendar/events/$id") { header("Authorization", auth()) }
        },
        onOffline = {
            PendingQueue.add(PendingAction("DELETE", "/calendar/events/$id"))
        },
    )

    // ─── Temporizador de estudio ───────────────────────────────────
    suspend fun studyTimerStats(): StudyTimerStats = request("study-timer/stats") {
        client.get("/study-timer/stats") { header("Authorization", auth()) }
    }

    /**
     * Historial de sesiones filtrado por periodo (day/week/month/all) y paginado.
     * El backend nuevo devuelve { sessions, total, page, limit }; el desplegado en
     * producción aún manda un array plano → se toleran ambos formatos.
     */
    suspend fun studyTimerSessions(period: String = "all", page: Int = 1, limit: Int = 20): StudySessionPage {
        val cacheKey = "study-timer/sessions/$period/$page"
        var gotResponse = false
        try {
            val response = withNetworkRetry {
                client.get("/study-timer/sessions") {
                    header("Authorization", auth())
                    url.parameters.append("period", period)
                    url.parameters.append("page", page.toString())
                    url.parameters.append("limit", limit.toString())
                }
            }
            val text = response.bodyAsText()
            gotResponse = true
            val parsed: StudySessionPage = parseSessionPage(text)
            OfflineCache.save(cacheKey, text)
            OfflineState.setOnline(true)
            flushPending()
            return parsed
        } catch (e: ResponseException) {
            val backendMessage = runCatching { e.response.body<ErrorBody>().message }.getOrNull()
            logError("ApiClient", "HTTP ${e.response.status.value} en study-timer/sessions: ${e.message}")
            throw ApiException(backendMessage?.takeIf { it.isNotBlank() } ?: "Error ${e.response.status.value}")
        } catch (e: ApiException) {
            throw e
        } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
            logError("ApiClient", "Timeout en study-timer/sessions: ${e.message}")
            throw ApiException("El servidor tardó demasiado en responder")
        } catch (e: io.ktor.utils.io.errors.IOException) {
            logError("ApiClient", "Error de red en study-timer/sessions: ${e::class.simpleName}: ${e.message}")
            if (!gotResponse) {
                val cached = OfflineCache.load(cacheKey)
                if (cached != null) {
                    OfflineState.setOnline(false)
                    return parseSessionPage(cached)
                }
            }
            throw ApiException("No se pudo conectar con el servidor")
        }
    }

    /** Acepta { sessions, total, page, limit } (nuevo) o un array plano (backend desplegado). */
    private fun parseSessionPage(text: String): StudySessionPage {
        val trimmed = text.trim()
        if (trimmed.startsWith("[")) {
            val list = json.decodeFromString<List<StudySession>>(trimmed)
            return StudySessionPage(sessions = list, total = list.size, page = 1, limit = list.size)
        }
        return json.decodeFromString<StudySessionPage>(trimmed)
    }

    suspend fun saveStudySession(durationMinutes: Int, technique: String, subjectId: Int? = null): StudySession =
        mutate(
            offlineMessage = "Sin conexión: la sesión se guardará cuando tengas internet",
            block = {
                client.post("/study-timer/session") {
                    header("Authorization", auth())
                    contentType(ContentType.Application.Json)
                    setBody(
                        buildString {
                            append("{\"durationMinutes\":$durationMinutes,\"technique\":\"$technique\"")
                            if (subjectId != null) append(",\"subjectId\":$subjectId")
                            append("}")
                        },
                    )
                }
            },
            onOffline = {
                PendingQueue.add(
                    PendingAction(
                        method = "POST",
                        url = "/study-timer/session",
                        body = buildString {
                            append("{\"durationMinutes\":$durationMinutes,\"technique\":\"$technique\"")
                            if (subjectId != null) append(",\"subjectId\":$subjectId")
                            append("}")
                        },
                    ),
                )
            },
        )

    // ─── Perfil ──────────────────────────────────────────────────────────
    suspend fun profileAcademic(): ProfileAcademic = request("profile/academic") {
        client.get("/profile/academic") { header("Authorization", auth()) }
    }

    suspend fun createAcademicProfile(
        universidad: String,
        carrera: String,
        facultad: String,
        semestreActual: Int,
        promedio: Double,
        modalidad: String,
    ): ProfileAcademic = mutate(block = {
        client.post("/profile/academic") {
            header("Authorization", auth())
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "universidad" to universidad,
                    "carrera" to carrera,
                    "facultad" to facultad,
                    "semestreActual" to semestreActual,
                    "promedio" to promedio,
                    "modalidad" to modalidad,
                    "fechaInicio" to "2024-01-15",
                    "fechaGraduacion" to "2029-12-15",
                ),
            )
        }
    })

    suspend fun updateAcademicProfile(
        universidad: String,
        carrera: String,
        facultad: String,
        semestreActual: Int,
        promedio: Double,
        modalidad: String,
    ): ProfileAcademic = mutate(block = {
        client.put("/profile/academic") {
            header("Authorization", auth())
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "universidad" to universidad,
                    "carrera" to carrera,
                    "facultad" to facultad,
                    "semestreActual" to semestreActual,
                    "promedio" to promedio,
                    "modalidad" to modalidad,
                ),
            )
        }
    })
}

@kotlinx.serialization.Serializable
private data class ErrorBody(
    val message: String = "",
    val error: String = "",
    val statusCode: Int = 0,
)

/** Cliente compartido por toda la app (un solo HttpClient por proceso). */
object Api {
    val client by lazy { ApiClient() }
}
