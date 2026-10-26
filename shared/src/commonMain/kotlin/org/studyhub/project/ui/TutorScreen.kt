package org.studyhub.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import org.studyhub.project.StudyHubColors
import org.studyhub.project.net.Api
import org.studyhub.project.net.Conversation
import org.studyhub.project.net.TeacherProfile
import org.studyhub.project.net.TokenStore
import org.studyhub.project.ui.icons.AppIcon

private const val KEY_TEACHER = "studyhub_teacher_id"
private const val KEY_CONVERSATION = "studyhub_conversation_id"

private data class ChatUiMessage(
    val role: String,
    val content: String,
    val imageBytes: ByteArray? = null,
    val imageName: String? = null,
)

private val SUGGESTIONS = listOf(
    "Explícame un tema que no entiendo",
    "Prepara un plan de estudio para hoy",
    "Ayúdame con una tarea",
    "¿Qué brechas de conocimiento tengo?",
)

private enum class TutorTab { CHAT, QUIZZES, HISTORY }

@Composable
fun TutorScreen(
    unread: Int,
    onBell: () -> Unit,
    userName: String,
    userPhoto: String?,
    onOpenSpace: (SpaceTarget) -> Unit,
    onOpenSubjects: () -> Unit,
    onChatVisibilityChanged: (Boolean) -> Unit = {},
) {
    val demo = remember { TokenStore.load() == null }
    var messages by remember { mutableStateOf(listOf<ChatUiMessage>()) }
    var input by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var conversationId by remember { mutableStateOf<String?>(null) }
    var teachers by remember { mutableStateOf<List<TeacherProfile>>(emptyList()) }
    var teacherId by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(TutorTab.CHAT) }
    var history by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var historyLoading by remember { mutableStateOf(false) }
    var historyError by remember { mutableStateOf<String?>(null) }
    var showChat by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("Todo") }
    var search by remember { mutableStateOf("") }
    var pendingImage by remember { mutableStateOf<PickedImage?>(null) }
    var analyzingImage by remember { mutableStateOf(false) }
    val canPickImages = remember { imagePickerSupported() }
    val pickImage = rememberImagePicker { picked -> pendingImage = picked }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // El chat ocupa toda la pantalla: MainApp esconde la barra inferior mientras está abierto.
    LaunchedEffect(showChat) {
        onChatVisibilityChanged(showChat)
    }
    PlatformBackHandler(enabled = showChat) {
        showChat = false
        pendingImage = null
    }

    LaunchedEffect(Unit) {
        if (demo) return@LaunchedEffect
        // Restaurar profesor elegido
        TokenStore.loadString(KEY_TEACHER)?.takeIf { it.isNotBlank() }?.let { teacherId = it }
        // Restaurar conversación guardada
        val savedConv = TokenStore.loadString(KEY_CONVERSATION)?.takeIf { it.isNotBlank() }
        if (savedConv != null) {
            conversationId = savedConv
            val loaded = runCatching { Api.client.conversationMessages(savedConv) }.getOrNull()
            if (loaded != null && loaded.isNotEmpty()) {
                messages = loaded.map { ChatUiMessage(it.role, it.content) }
            } else {
                conversationId = null
                TokenStore.saveString(KEY_CONVERSATION, "")
            }
        }
        teachers = runCatching { Api.client.teacherProfiles() }
            .getOrDefault(emptyList())
            .filter { it.active }
    }

    LaunchedEffect(messages.size, typing, showChat, tab) {
        if (!showChat || tab != TutorTab.CHAT || messages.isEmpty()) return@LaunchedEffect
        // Al llegar una respuesta larga, mostrarla desde el inicio; al enviar,
        // bajar hasta el indicador de escritura para dejar ver el mensaje propio.
        val target = if (messages.last().role == "assistant") {
            messages.lastIndex
        } else {
            messages.lastIndex + if (typing) 1 else 0
        }
        listState.animateScrollToItem(target)
    }

    // Cargar historial al abrir la pestaña
    LaunchedEffect(tab, showChat) {
        if (tab == TutorTab.HISTORY && showChat) {
            if (demo) {
                history = emptyList()
                historyError = null
                historyLoading = false
                return@LaunchedEffect
            }
            historyLoading = true
            historyError = null
            try {
                history = Api.client.conversations()
            } catch (e: Exception) {
                historyError = e.message ?: "No se pudo cargar el historial"
            } finally {
                historyLoading = false
            }
        }
    }

    fun openConversation(conv: Conversation) {
        scope.launch {
            val loaded = runCatching { Api.client.conversationMessages(conv.id) }.getOrNull()
            if (loaded != null && loaded.isNotEmpty()) {
                messages = loaded.map { ChatUiMessage(it.role, it.content) }
                conversationId = conv.id
                TokenStore.saveString(KEY_CONVERSATION, conv.id)
                tab = TutorTab.CHAT
            } else {
                historyError = "No se pudo abrir esa conversación"
            }
        }
    }

    fun removeConversation(conv: Conversation) {
        scope.launch {
            runCatching { Api.client.deleteConversation(conv.id) }
            if (conversationId == conv.id) {
                conversationId = null
                TokenStore.saveString(KEY_CONVERSATION, "")
                messages = emptyList()
            }
            history = runCatching { Api.client.conversations() }.getOrDefault(history - conv)
        }
    }

    fun send(text: String, image: PickedImage? = null) {
        val trimmed = text.trim()
        if ((trimmed.isEmpty() && image == null) || typing) return
        val question = trimmed.ifBlank { "Analiza esta imagen y explícame lo que ves." }
        messages = messages + ChatUiMessage(
            role = "user",
            content = question,
            imageBytes = image?.bytes,
            imageName = image?.filename,
        )
        input = ""
        pendingImage = null
        typing = true
        analyzingImage = image != null
        if (demo) {
            messages = messages + ChatUiMessage(
                "assistant",
                if (image != null) {
                    "En el modo demo no puedo analizar imágenes, pero con tu cuenta real sí puedo. " +
                        "Mientras tanto, cuéntame qué materia o ejercicio aparece y te ayudo."
                } else {
                    DemoData.replyFor(question)
                },
            )
            typing = false
            analyzingImage = false
            return
        }
        scope.launch {
            try {
                val response = if (image != null) {
                    // Fase 1: visión extrae el contenido de la imagen; luego el tutor
                    // conversa sobre ese contenido (el backend ya no reenvía la imagen).
                    val analysis = Api.client.analyzeImage(
                        bytes = image.bytes,
                        filename = image.filename,
                        mimeType = image.mimeType,
                    )
                    Api.client.chatWithImage(
                        message = question,
                        filename = image.filename,
                        contenido = analysis.contenido,
                        conversationId = conversationId,
                        teacherId = teacherId,
                    )
                } else {
                    Api.client.chat(question, conversationId, teacherId)
                }
                conversationId = response.conversationId.ifBlank { conversationId }
                conversationId?.let { TokenStore.saveString(KEY_CONVERSATION, it) }
                val reply = response.reply.ifBlank { "No pude generar una respuesta." }
                messages = messages + ChatUiMessage("assistant", reply)
            } catch (e: Exception) {
                messages = messages + ChatUiMessage(
                    "assistant",
                    "No pude conectar con el tutor IA: ${e.message ?: "error de red"}",
                )
            } finally {
                typing = false
                analyzingImage = false
            }
        }
    }

    fun openChat(prompt: String? = null) {
        showChat = true
        tab = TutorTab.CHAT
        if (!prompt.isNullOrBlank()) send(prompt)
    }

    if (!showChat) {
        TutorHub(
            unread = unread,
            onBell = onBell,
            userName = userName,
            userPhoto = userPhoto,
            search = search,
            onSearchChange = { search = it },
            onSearch = { if (search.isNotBlank()) openChat(search) },
            filter = filter,
            onFilter = { filter = it },
            onOpenChat = ::openChat,
            onOpenHistory = {
                tab = TutorTab.HISTORY
                showChat = true
            },
            onOpenSpace = onOpenSpace,
            onOpenSubjects = onOpenSubjects,
        )
        return
    }

    val teacherName = teacherId?.let { id -> teachers.firstOrNull { it.id == id }?.name }

    Column(Modifier.fillMaxSize()) {
        // Encabezado del tutor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudyHubColors.Surface)
                    .border(1.dp, StudyHubColors.GlassBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        showChat = false
                        pendingImage = null
                    },
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(Icons.AutoMirrored.Outlined.ArrowBack, tint = StudyHubColors.TextPrimary, size = 18.dp)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(StudyHubColors.HeroLight, StudyHubColors.Hero),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(Icons.Outlined.SmartToy, tint = Color.White, size = 21.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Tutor IA", color = StudyHubColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(StudyHubColors.Secondary))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        teacherName ?: "En línea",
                        color = StudyHubColors.TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
            // Nueva conversación
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(StudyHubColors.Surface)
                    .border(1.dp, StudyHubColors.GlassBorder, RoundedCornerShape(11.dp))
                    .clickable {
                        messages = emptyList()
                        conversationId = null
                        TokenStore.saveString(KEY_CONVERSATION, "")
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(Icons.Outlined.AddComment, tint = StudyHubColors.Primary, size = 15.dp)
                Spacer(Modifier.width(5.dp))
                Text("Nueva", color = StudyHubColors.Primary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Selector de profesor IA (solo antes de empezar a conversar)
        if (tab == TutorTab.CHAT && teachers.isNotEmpty() && messages.isEmpty()) {
            ProfessorSelector(
                teachers = teachers,
                selectedId = teacherId,
                onSelect = {
                    teacherId = it
                    TokenStore.saveString(KEY_TEACHER, it ?: "")
                },
            )
        }

        // Pestañas Chat / Quizzes (se ocultan al conversar para dar espacio a los mensajes)
        if (tab != TutorTab.CHAT || messages.isEmpty()) {
            TutorTabs(current = tab, onSelect = { tab = it })
        }

        Box(Modifier.weight(1f)) {
            if (tab == TutorTab.QUIZZES) {
                QuizzesScreen(onBack = null)
            } else if (tab == TutorTab.HISTORY) {
                HistoryList(
                    conversations = history,
                    loading = historyLoading,
                    error = historyError,
                    onOpen = ::openConversation,
                    onDelete = ::removeConversation,
                    onRetry = {
                        historyLoading = true
                        historyError = null
                        scope.launch {
                            history = runCatching { Api.client.conversations() }.getOrDefault(history)
                            historyLoading = false
                        }
                    },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 10.dp),
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Column {
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "Hola, soy tu tutor de estudio con IA. Pregúntame dudas, pídeme un plan, un simulacro o una explicación paso a paso. También puedes adjuntar una foto de tus apuntes o de un ejercicio y la analizo contigo.",
                                    color = StudyHubColors.TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(StudyHubColors.Surface)
                                        .border(1.dp, StudyHubColors.GlassBorder, RoundedCornerShape(18.dp))
                                        .padding(14.dp),
                                )
                                Spacer(Modifier.height(18.dp))
                                Text("Sugerencias", color = StudyHubColors.TextTertiary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(10.dp))
                                SUGGESTIONS.forEach { s ->
                                    GlassChip(
                                        text = s,
                                        selected = false,
                                        onClick = { send(s) },
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(messages) { _, msg ->
                            MessageBubble(msg)
                        }
                        if (typing) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(StudyHubColors.Surface)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(
                                        if (analyzingImage) "Analizando imagen…" else "Escribiendo…",
                                        color = StudyHubColors.TextTertiary,
                                        fontSize = 12.5.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Entrada (solo en el chat)
        if (tab == TutorTab.CHAT) {
            pendingImage?.let { image ->
                PendingImageCard(
                    image = image,
                    onRemove = { pendingImage = null },
                )
            }
            val canSend = input.isNotBlank() || pendingImage != null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(StudyHubColors.Surface)
                    .border(1.dp, StudyHubColors.GlassBorder, RoundedCornerShape(22.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canPickImages) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudyHubColors.SageSoft)
                            .clickable(enabled = !typing) { pickImage() },
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(Icons.Outlined.Image, tint = StudyHubColors.Primary, size = 18.dp)
                    }
                    Spacer(Modifier.width(2.dp))
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = StudyHubColors.TextPrimary,
                        fontSize = 14.sp,
                    ),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (input.isEmpty()) {
                            Text(
                                if (pendingImage != null) "Pregunta sobre la imagen…" else "Escribe tu pregunta…",
                                color = StudyHubColors.TextTertiary,
                                fontSize = 14.sp,
                            )
                        }
                        inner()
                    },
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (canSend) StudyHubColors.Primary
                            else StudyHubColors.Primary.copy(alpha = 0.35f),
                        )
                        .clickable(enabled = canSend && !typing) { send(input, pendingImage) },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(Icons.AutoMirrored.Outlined.Send, tint = Color.White, size = 19.dp)
                }
            }
        }
    }
}

// ─── Hub de IA Study ────────────────────────────────────────────────────

private data class StudyTool(
    val title: String,
    val eyebrow: String,
    val subtitle: String,
    val action: String,
    val tag: String,
    val background: Color,
    val tint: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun TutorHub(
    unread: Int,
    onBell: () -> Unit,
    userName: String,
    userPhoto: String?,
    search: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    filter: String,
    onFilter: (String) -> Unit,
    onOpenChat: (String?) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSpace: (SpaceTarget) -> Unit,
    onOpenSubjects: () -> Unit,
) {
    val tools = listOf(
        StudyTool(
            title = "Flashcards",
            eyebrow = "IA STUDY",
            subtitle = "Convierte tus apuntes en tarjetas.",
            action = "Crear +",
            tag = "Flashcards",
            background = StudyHubColors.SageSoft,
            tint = StudyHubColors.Primary,
            icon = Icons.Outlined.Style,
            onClick = { onOpenChat("Crea flashcards de mis apuntes de Cálculo II") },
        ),
        StudyTool(
            title = "Simulacro",
            eyebrow = "PRÁCTICA",
            subtitle = "Prepárate para tus exámenes.",
            action = "Comenzar",
            tag = "Simulacros",
            background = StudyHubColors.Lavender,
            tint = StudyHubColors.Info,
            icon = Icons.Outlined.Quiz,
            onClick = { onOpenSpace(SpaceTarget.QUIZZES) },
        ),
        StudyTool(
            title = "Apuntes",
            eyebrow = "ORGANIZA",
            subtitle = "Repasa y conecta ideas.",
            action = "Ver",
            tag = "Apuntes",
            background = StudyHubColors.Cream,
            tint = StudyHubColors.Accent,
            icon = Icons.Outlined.NoteAlt,
            onClick = onOpenSubjects,
        ),
        StudyTool(
            title = "Tutor IA",
            eyebrow = "CHAT",
            subtitle = "Pregunta lo que necesites.",
            action = "Abrir",
            tag = "Todo",
            background = StudyHubColors.Sky,
            tint = StudyHubColors.Info,
            icon = Icons.Outlined.ChatBubbleOutline,
            onClick = { onOpenChat(null) },
        ),
        StudyTool(
            title = "Enfoque",
            eyebrow = "CONCENTRACIÓN",
            subtitle = "Estudia sin distracciones.",
            action = "Empezar",
            tag = "Todo",
            background = StudyHubColors.Peach,
            tint = StudyHubColors.Accent,
            icon = Icons.Outlined.Timer,
            onClick = { onOpenSpace(SpaceTarget.FOCUS) },
        ),
        StudyTool(
            title = "Historial",
            eyebrow = "CONVERSACIONES",
            subtitle = "Retoma donde lo dejaste.",
            action = "Ver",
            tag = "Todo",
            background = StudyHubColors.SageSoft,
            tint = StudyHubColors.Primary,
            icon = Icons.Outlined.History,
            onClick = onOpenHistory,
        ),
    )
    val visible = if (filter == "Todo") tools else tools.filter { it.tag == filter }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        StudyHubTopBar(
            eyebrow = "Aprende a tu ritmo",
            title = "IA Study",
            unread = unread,
            onBell = onBell,
            userName = userName,
            userPhoto = userPhoto,
        )
        Spacer(Modifier.height(18.dp))

        // Hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(StudyHubColors.Hero, StudyHubColors.HeroDark),
                    ),
                )
                .clickable { onOpenChat(null) }
                .padding(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(120.dp)
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
                            text = "TU ASISTENTE PERSONAL",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Haz que estudiar\nse sienta fácil.",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 26.sp,
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "Herramientas inteligentes para entender más y memorizar mejor.",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(Icons.Outlined.AutoAwesome, tint = Color.White.copy(alpha = 0.92f), size = 27.dp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Buscador
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(StudyHubColors.Surface)
                    .border(1.dp, StudyHubColors.GlassBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(Icons.Outlined.Search, tint = StudyHubColors.TextTertiary, size = 18.dp)
                Spacer(Modifier.width(9.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = search,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = StudyHubColors.TextPrimary,
                        fontSize = 13.5.sp,
                    ),
                    decorationBox = { inner ->
                        if (search.isEmpty()) {
                            Text("¿Qué quieres aprender?", color = StudyHubColors.TextTertiary, fontSize = 13.5.sp)
                        }
                        inner()
                    },
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(StudyHubColors.SageSoft)
                        .clickable(onClick = onSearch),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(Icons.Outlined.ArrowForward, tint = StudyHubColors.Primary, size = 16.dp)
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(StudyHubColors.Surface)
                    .border(1.dp, StudyHubColors.GlassBorder, RoundedCornerShape(18.dp))
                    .clickable {
                        val filters = listOf("Todo", "Flashcards", "Simulacros", "Apuntes")
                        onFilter(filters[(filters.indexOf(filter).coerceAtLeast(0) + 1) % filters.size])
                    },
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(Icons.Outlined.Tune, tint = StudyHubColors.TextSecondary, size = 19.dp)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Todo", "Flashcards", "Simulacros", "Apuntes").forEach { item ->
                GlassChip(
                    text = item,
                    selected = filter == item,
                    onClick = { onFilter(item) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        SectionTitle(
            text = "Para estudiar hoy",
            action = "Ver todo",
            onAction = { onFilter("Todo") },
        )
        Spacer(Modifier.height(10.dp))

        visible.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { tool ->
                    StudyToolCard(tool = tool, modifier = Modifier.weight(1f))
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StudyToolCard(tool: StudyTool, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(172.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(tool.background)
            .clickable(onClick = tool.onClick)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.62f)),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(tool.icon, tint = tool.tint, size = 21.dp)
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = tool.eyebrow,
            color = tool.tint.copy(alpha = 0.75f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = tool.title,
            color = StudyHubColors.TextPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tool.subtitle,
            color = StudyHubColors.TextSecondary,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            maxLines = 2,
        )
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tool.action,
                color = tool.tint,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(3.dp))
            AppIcon(Icons.Outlined.ArrowForward, tint = tool.tint, size = 13.dp)
        }
    }
}

@Composable
private fun ProfessorSelector(
    teachers: List<TeacherProfile>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    val selected = teachers.firstOrNull { it.id == selectedId }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Text("Elige tu profesor IA", color = StudyHubColors.TextTertiary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfessorChip(text = "Auto", selected = selectedId == null) { onSelect(null) }
            teachers.forEach { t ->
                ProfessorChip(text = t.name, selected = selectedId == t.id) { onSelect(t.id) }
            }
        }
        // Detalle del profesor seleccionado
        Spacer(Modifier.height(8.dp))
        if (selected != null) {
            GlassCard(radius = 14.dp, contentPadding = 12.dp, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = selected.description.ifBlank { selected.name },
                    color = StudyHubColors.TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                )
                if (selected.subjects.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Materias: ${selected.subjects.joinToString(" · ") { it.replaceFirstChar { c -> c.uppercase() } }}",
                        color = StudyHubColors.TextTertiary,
                        fontSize = 11.5.sp,
                    )
                }
                if (selected.teachingStyle.isNotBlank() || selected.difficultyLevel.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (selected.teachingStyle.isNotBlank()) {
                            Badge(styleName(selected.teachingStyle), StudyHubColors.PrimaryLight, small = true)
                        }
                        if (selected.difficultyLevel.isNotBlank()) {
                            Badge("Nivel ${selected.difficultyLevel.lowercase()}", StudyHubColors.AccentLight, small = true)
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Auto elige el profesor según el tema de tu pregunta.",
                color = StudyHubColors.TextTertiary,
                fontSize = 11.5.sp,
            )
        }
    }
}

/** Traduce el código de estilo de enseñanza a una etiqueta legible. */
private fun styleName(style: String): String = when (style.lowercase()) {
    "step_by_step" -> "Paso a paso"
    "socratic" -> "Socrático"
    "project_based" -> "Por proyectos"
    "analogies" -> "Con analogías"
    "concise" -> "Directo"
    "contextual" -> "Contextual"
    "explain_then_code" -> "Explica y codifica"
    "correction" -> "Corrección"
    "corrective" -> "Correctivo"
    "narrative" -> "Narrativo"
    "example_based" -> "Con ejemplos"
    "exam_focused" -> "Enfocado a exámenes"
    else -> style.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
private fun ProfessorChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) StudyHubColors.Primary else StudyHubColors.Surface)
            .border(1.dp, if (selected) StudyHubColors.Primary else StudyHubColors.Border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else StudyHubColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun TutorTabs(current: TutorTab, onSelect: (TutorTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StudyHubColors.SurfaceLight)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TutorTab.entries.forEach { t ->
            val selected = t == current
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) StudyHubColors.Surface else Color.Transparent)
                    .clickable { onSelect(t) }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(
                    icon = when (t) {
                        TutorTab.CHAT -> Icons.Outlined.ChatBubbleOutline
                        TutorTab.QUIZZES -> Icons.Outlined.Quiz
                        TutorTab.HISTORY -> Icons.Outlined.History
                    },
                    tint = if (selected) StudyHubColors.Primary else StudyHubColors.TextTertiary,
                    size = 16.dp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (t) {
                        TutorTab.CHAT -> "Chat"
                        TutorTab.QUIZZES -> "Quizzes"
                        TutorTab.HISTORY -> "Historial"
                    },
                    color = if (selected) StudyHubColors.Primary else StudyHubColors.TextTertiary,
                    fontSize = 12.5.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun HistoryList(
    conversations: List<Conversation>,
    loading: Boolean,
    error: String?,
    onOpen: (Conversation) -> Unit,
    onDelete: (Conversation) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        loading -> LoadingState()
        error != null -> ErrorState(error) { onRetry() }
        conversations.isEmpty() -> EmptyState("Aún no tienes historial. Chatea con el tutor y aquí podrás retomar tus conversaciones.")
        else -> Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            conversations.forEach { conv ->
                val date = formatDate(conv.lastMessageAt)
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    radius = 16.dp,
                    contentPadding = 14.dp,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(StudyHubColors.Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppIcon(Icons.Outlined.History, tint = StudyHubColors.PrimaryLight, size = 18.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable { onOpen(conv) },
                        ) {
                            Text(
                                text = conv.title.ifBlank { "Conversación sin título" },
                                color = StudyHubColors.TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = buildString {
                                    if (date.isNotBlank()) {
                                        append(date)
                                        append(" · ")
                                    }
                                    append("${conv.messageCount} mensajes")
                                },
                                color = StudyHubColors.TextTertiary,
                                fontSize = 11.5.sp,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(StudyHubColors.Surface)
                                .clickable { onOpen(conv) }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        ) {
                            Text("Abrir", color = StudyHubColors.PrimaryLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(StudyHubColors.Surface)
                                .clickable { onDelete(conv) },
                            contentAlignment = Alignment.Center,
                        ) {
                            AppIcon(Icons.Outlined.DeleteOutline, tint = StudyHubColors.DangerLight, size = 17.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingImageCard(image: PickedImage, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StudyHubColors.SageSoft)
            .border(1.dp, StudyHubColors.GlassBorder, RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PickedImageThumb(
            bytes = image.bytes,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(11.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                image.filename,
                color = StudyHubColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Listo para analizar", color = StudyHubColors.TextSecondary, fontSize = 10.5.sp)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(StudyHubColors.Surface)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(Icons.Outlined.Close, tint = StudyHubColors.TextSecondary, size = 14.dp)
        }
    }
}

@Composable
private fun PickedImageThumb(bytes: ByteArray, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = bytes,
        contentDescription = "Imagen adjunta",
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { ImageThumbFallback() },
        error = { ImageThumbFallback() },
    )
}

@Composable
private fun ImageThumbFallback() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudyHubColors.SageSoft),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(Icons.Outlined.Image, tint = StudyHubColors.Primary, size = 18.dp)
    }
}

@Composable
private fun MessageBubble(msg: ChatUiMessage) {
    val isUser = msg.role == "user"
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else 5.dp,
        bottomEnd = if (isUser) 5.dp else 18.dp,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.78f else 0.92f)
                .clip(shape)
                .background(if (isUser) StudyHubColors.Primary else StudyHubColors.Surface)
                .border(1.dp, if (isUser) Color.Transparent else StudyHubColors.GlassBorder, shape)
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Column {
                msg.imageBytes?.let { bytes ->
                    PickedImageThumb(
                        bytes = bytes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                RichText(
                    text = msg.content,
                    color = if (isUser) Color.White else StudyHubColors.TextPrimary,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                )
            }
        }
    }
}
