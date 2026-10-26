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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.studyhub.project.StudyHubColors
import org.studyhub.project.net.Api
import org.studyhub.project.net.Roadmap
import org.studyhub.project.net.RoadmapStep
import org.studyhub.project.net.computedProgress
import org.studyhub.project.ui.icons.AppIcon

@Composable
fun RoadmapsScreen() {
    var roadmaps by remember { mutableStateOf<List<Roadmap>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Roadmap?>(null) }

    LaunchedEffect(reload) {
        loading = true
        try {
            roadmaps = Api.client.roadmaps()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar las rutas"
        } finally {
            loading = false
        }
    }

    // Back del sistema cierra el detalle de la ruta en vez de la app
    PlatformBackHandler(enabled = selected != null) { selected = null }

    if (selected != null) {
        RoadmapDetailScreen(
            roadmap = selected!!,
            onBack = { selected = null; reload++ },
            onCompleted = { selected = selected?.copy(progress = 100) },
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
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                // Header hero con gradiente
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(StudyHubColors.Hero, StudyHubColors.HeroDark)))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(
                            icon = Icons.Outlined.Route,
                            gradient = listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.10f)),
                            size = 48.dp,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Rutas de Aprendizaje",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Planes de estudio paso a paso generados con IA",
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                // Resumen
                val totalSteps = roadmaps.sumOf { it.steps.size }
                val doneSteps = roadmaps.sumOf { it.steps.count { s -> s.completed } }
                val totalHours = roadmaps.sumOf { it.estimatedHours }.roundToInt()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(Icons.Outlined.Map, StudyHubColors.PrimaryLight, "${roadmaps.size}", "Rutas", Modifier.weight(1f))
                    StatCard(Icons.Outlined.CheckCircle, StudyHubColors.SecondaryLight, "$doneSteps/$totalSteps", "Pasos", Modifier.weight(1f))
                    StatCard(Icons.Outlined.Schedule, StudyHubColors.AccentLight, "$totalHours h", "Estimadas", Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp))

                // Generador
                RoadmapGenerator(
                    onGenerated = {
                        selected = it
                        reload++
                    },
                    onError = { error = it },
                )
                Spacer(Modifier.height(20.dp))

                SectionTitle("Mis rutas", icon = Icons.Outlined.Map, iconTint = StudyHubColors.InfoLight)
                Spacer(Modifier.height(10.dp))
                if (roadmaps.isEmpty()) {
                    EmptyState("Aún no tienes rutas de aprendizaje. Genera una con IA para empezar.")
                }
                roadmaps.forEachIndexed { index, rm ->
                    RoadmapCard(
                        roadmap = rm,
                        gradient = ROADMAP_GRADIENTS[index % ROADMAP_GRADIENTS.size],
                        onClick = { selected = rm },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Gradientes para distinguir cada ruta en la lista. */
private val ROADMAP_GRADIENTS = listOf(
    listOf(StudyHubColors.Primary, StudyHubColors.Secondary),
    listOf(StudyHubColors.Info, StudyHubColors.Primary),
    listOf(StudyHubColors.Accent, StudyHubColors.AccentDark),
    listOf(StudyHubColors.Info, StudyHubColors.InfoLight),
    listOf(StudyHubColors.Primary, StudyHubColors.Info),
)

@Composable
private fun RoadmapCard(roadmap: Roadmap, gradient: List<Color>, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable(onClick = onClick),
        radius = 18.dp,
        contentPadding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = Icons.Outlined.Route, gradient = gradient, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = roadmap.title.ifBlank { roadmap.topic },
                    color = StudyHubColors.TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (roadmap.category.isNotBlank()) Badge(roadmap.category, StudyHubColors.InfoLight, small = true)
                    if (roadmap.difficulty.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Badge(roadmap.difficulty, difficultyColor(roadmap.difficulty), small = true)
                    }
                    if (roadmap.estimatedHours > 0) {
                        Spacer(Modifier.width(6.dp))
                        Badge("≈ ${roadmap.estimatedHours.toInt()} h", StudyHubColors.AccentLight, small = true)
                    }
                }
                Spacer(Modifier.height(8.dp))
                GlassProgress(
                    progress = roadmap.computedProgress() / 100f,
                    color = StudyHubColors.SecondaryLight,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${roadmap.computedProgress()}% completado${if (roadmap.steps.isNotEmpty()) " · ${roadmap.steps.count { it.completed }}/${roadmap.steps.size} pasos" else ""}",
                    color = StudyHubColors.TextTertiary,
                    fontSize = 11.5.sp,
                )
            }
            Spacer(Modifier.width(6.dp))
            AppIcon(Icons.Outlined.ChevronRight, tint = StudyHubColors.TextTertiary, size = 20.dp)
        }
    }
}

@Composable
private fun RoadmapGenerator(onGenerated: (Roadmap) -> Unit, onError: (String) -> Unit) {
    var topic by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    GlassCard(radius = 20.dp, contentPadding = 0.dp) {
        // Banner con gradiente: le da presencia al generador
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Brush.linearGradient(listOf(StudyHubColors.HeroLight, StudyHubColors.Hero)))
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    icon = Icons.Outlined.AutoAwesome,
                    gradient = listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.12f)),
                    size = 44.dp,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Generar ruta con IA", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(3.dp))
                    Text("Cuéntanos qué quieres aprender", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }
            }
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            GlassTextField(
                value = topic,
                onValueChange = { topic = it },
                label = "Tema o habilidad",
                placeholder = "Ej: Inglés, React, Cálculo…",
            )
            Spacer(Modifier.height(14.dp))
            GlassTextField(
                value = goal,
                onValueChange = { goal = it },
                label = "Objetivo (opcional)",
                placeholder = "Ej: para conversar en mis viajes…",
            )
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                text = "Generar ruta",
                icon = Icons.Outlined.AutoAwesome,
                modifier = Modifier.fillMaxWidth(),
                enabled = topic.isNotBlank() && !generating,
                loading = generating,
            ) {
                generating = true
                scope.launch {
                    try {
                        val roadmap = Api.client.generateRoadmap(topic, goal)
                        onGenerated(roadmap)
                    } catch (e: Exception) {
                        onError(e.message ?: "No se pudo generar la ruta")
                    } finally {
                        generating = false
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadmapDetailScreen(roadmap: Roadmap, onBack: () -> Unit, onCompleted: () -> Unit) {
    var detail by remember { mutableStateOf<Roadmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var practicing by remember { mutableStateOf<RoadmapStep?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        loading = true
        try {
            detail = Api.client.roadmapDetail(roadmap.id)
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar la ruta"
        } finally {
            loading = false
        }
    }

    fun completeStep(stepId: Int) {
        scope.launch {
            runCatching { Api.client.completeStep(stepId) }
            reload++
        }
    }

    val practiceStep = practicing
    if (practiceStep != null) {
        RoadmapPracticeScreen(
            step = practiceStep,
            onBack = { practicing = null },
            onStepCompleted = {
                practicing = null
                reload++
            },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        FullScreenBackBar(roadmap.title.ifBlank { roadmap.topic }, "Ruta de aprendizaje", onBack)
        Box(Modifier.weight(1f)) {
            when {
                loading -> LoadingState()
                error != null -> ErrorState(error.orEmpty()) {
                    loading = true
                    error = null
                    reload++
                }
                else -> {
                    val data = detail ?: roadmap
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp),
                    ) {
                        if (data.description.isNotBlank()) {
                            Text(data.description, color = StudyHubColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                            Spacer(Modifier.height(12.dp))
                        }
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Progreso", color = StudyHubColors.TextSecondary, fontSize = 12.sp)
                                    Text("${data.computedProgress()}%", color = StudyHubColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                if (data.totalLevels > 0) {
                                    Badge("Nivel ${data.currentLevel} de ${data.totalLevels}", StudyHubColors.InfoLight)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            GlassProgress(progress = data.computedProgress() / 100f, color = StudyHubColors.SecondaryLight)
                        }
                        Spacer(Modifier.height(16.dp))

                        SectionTitle("Pasos", icon = Icons.Outlined.Psychology, iconTint = StudyHubColors.PrimaryLight)
                        Spacer(Modifier.height(10.dp))
                        if (data.steps.isEmpty()) {
                            EmptyState("Esta ruta aún no tiene pasos.")
                        }
                        data.steps.sortedBy { it.order }.forEach { step ->
                            GlassCard(modifier = Modifier.padding(bottom = 8.dp), radius = 16.dp, contentPadding = 14.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(
                                        icon = if (step.completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        tint = if (step.completed) StudyHubColors.SecondaryLight else StudyHubColors.TextTertiary,
                                        size = 22.dp,
                                        modifier = Modifier
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .clickable { completeStep(step.id) }
                                            .padding(2.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            step.title,
                                            color = if (step.completed) StudyHubColors.TextTertiary else StudyHubColors.TextPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textDecoration = if (step.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (step.skill.isNotBlank()) Badge(step.skill, StudyHubColors.InfoLight, small = true)
                                            if (step.practice.isNotEmpty()) {
                                                Spacer(Modifier.width(6.dp))
                                                Badge("Práctica (quiz)", StudyHubColors.AccentLight, small = true)
                                            }
                                        }
                                        if (step.practice.isNotEmpty()) {
                                            Spacer(Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(StudyHubColors.Accent.copy(alpha = 0.10f))
                                                    .clickable { practicing = step }
                                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                AppIcon(Icons.Outlined.PlayArrow, tint = StudyHubColors.AccentLight, size = 18.dp)
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "Practicar quiz (${step.practice.size} preguntas)",
                                                    color = StudyHubColors.AccentLight,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                AppIcon(Icons.Outlined.ChevronRight, tint = StudyHubColors.TextTertiary, size = 18.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Completa los pasos tocando el círculo o superando su quiz de práctica.",
                            color = StudyHubColors.TextTertiary,
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadmapPracticeScreen(
    step: RoadmapStep,
    onBack: () -> Unit,
    onStepCompleted: () -> Unit,
) {
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    QuizPlayer(
        title = "Práctica: ${step.title}",
        items = step.practice.map { p ->
            QuizItem(
                question = p.question,
                options = p.options,
                correctIndex = p.correctIndex,
                explanation = p.explanation,
            )
        },
        onBack = onBack,
        resultMessage = { passed ->
            if (passed) "¡Aprobaste la lección! Dominas este paso de tu ruta."
            else "Repasa la lección y vuelve a intentarlo para completar el paso."
        },
        resultAction = { _, _ ->
            if (!saved) {
                PrimaryButton(
                    text = "Marcar paso como completado",
                    icon = Icons.Outlined.Save,
                    loading = saving,
                    enabled = !saving,
                ) {
                    saving = true
                    scope.launch {
                        // El backend alterna el estado; solo marcamos si aún no está completado
                        if (!step.completed) runCatching { Api.client.completeStep(step.id) }
                        saving = false
                        saved = true
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Completar el paso también avanza el progreso de tu ruta",
                    color = StudyHubColors.TextTertiary,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            } else {
                GlassCard(radius = 16.dp, contentPadding = 14.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(Icons.Outlined.CheckCircle, tint = StudyHubColors.SecondaryLight, size = 20.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Paso completado correctamente",
                            color = StudyHubColors.SecondaryLight,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton(text = "Volver a la ruta", icon = Icons.Outlined.Route) { onStepCompleted() }
            }
        },
        onDone = onStepCompleted,
    )
}
