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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.studyhub.project.StudyHubColors
import org.studyhub.project.net.Api
import org.studyhub.project.net.OfflineState
import org.studyhub.project.net.TokenStore
import org.studyhub.project.ui.icons.AppIcon

enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Outlined.Home),
    AGENDA("Agenda", Icons.Outlined.CalendarMonth),
    TUTOR("IA Study", Icons.Outlined.AutoAwesome),
    SUBJECTS("Materias", Icons.Outlined.MenuBook),
    PROFILE("Perfil", Icons.Outlined.Person),
}

/** Vistas completas que se abren encima de las pestañas (con botón ‹ para volver). */
sealed interface FullScreen {
    data class Space(val target: SpaceTarget) : FullScreen
    data class SubjectDetail(
        val subjectId: Int,
        val subjectName: String,
        val colorHex: String,
        /** Pestaña desde la que se abrió: el back vuelve a ella. */
        val origin: AppTab,
    ) : FullScreen
}

@Composable
fun MainApp(onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var fullScreen by remember { mutableStateOf<FullScreen?>(null) }
    var notifOpen by remember { mutableStateOf(false) }
    var tutorChatOpen by remember { mutableStateOf(false) }
    var unread by remember { mutableStateOf(0) }
    var refreshTick by remember { mutableStateOf(0) }

    val demo = remember { TokenStore.load() == null }
    val scope = rememberCoroutineScope()

    var userName by remember { mutableStateOf(if (demo) DemoData.userName else "Estudiante") }
    var userPhoto by remember { mutableStateOf<String?>(null) }
    var offline by remember { mutableStateOf(!OfflineState.isOnline) }
    DisposableEffect(Unit) {
        OfflineState.onChanged = { offline = !OfflineState.isOnline }
        onDispose { OfflineState.onChanged = null }
    }

    LaunchedEffect(refreshTick) {
        if (demo) {
            unread = DemoData.unread
            userName = DemoData.userName
        } else {
            unread = runCatching { Api.client.unreadCount() }.getOrDefault(unread)
            runCatching { Api.client.dashboard() }
                .getOrNull()
                ?.user
                ?.nombre
                ?.takeIf { it.isNotBlank() }
                ?.let { userName = it }
            runCatching { Api.client.personalInfo() }
                .getOrNull()
                ?.let { info ->
                    info.nombre.takeIf { it.isNotBlank() }?.let { userName = it }
                    userPhoto = info.foto
                }
        }
    }

    Box(Modifier.fillMaxSize().background(StudyHubColors.Bg)) {
        Column(Modifier.fillMaxSize()) {
            if (offline) {
                OfflineBanner(
                    pending = OfflineState.pendingCount,
                    onRetry = {
                        scope.launch {
                            Api.client.flushPending()
                            refreshTick++
                        }
                    },
                )
            }
            if (demo) {
                DemoBanner()
            }
            Box(Modifier.weight(1f)) {
                when (tab) {
                    AppTab.HOME -> HomeScreen(
                        unread = unread,
                        onBell = { notifOpen = true },
                        onOpenSpace = { fullScreen = FullScreen.Space(it) },
                        onOpenSubject = { id, name, color -> fullScreen = FullScreen.SubjectDetail(id, name, color, AppTab.HOME) },
                        onOpenSubjects = { tab = AppTab.SUBJECTS },
                        photoUrl = userPhoto,
                    )
                    AppTab.AGENDA -> AgendaScreen(
                        unread = unread,
                        onBell = { notifOpen = true },
                        userName = userName,
                        userPhoto = userPhoto,
                    )
                    AppTab.TUTOR -> TutorScreen(
                        unread = unread,
                        onBell = { notifOpen = true },
                        userName = userName,
                        userPhoto = userPhoto,
                        onOpenSpace = { fullScreen = FullScreen.Space(it) },
                        onOpenSubjects = { tab = AppTab.SUBJECTS },
                        onChatVisibilityChanged = { tutorChatOpen = it },
                    )
                    AppTab.SUBJECTS -> SubjectsScreen(
                        onOpenDetail = { id, name, color -> fullScreen = FullScreen.SubjectDetail(id, name, color, AppTab.SUBJECTS) },
                        unread = unread,
                        onBell = { notifOpen = true },
                        userName = userName,
                        onOpenSpace = { fullScreen = FullScreen.Space(it) },
                        userPhoto = userPhoto,
                    )
                    AppTab.PROFILE -> ProfileScreen(
                        onLogout = onLogout,
                        onOpenSpace = { fullScreen = FullScreen.Space(it) },
                        photoUrl = userPhoto,
                        unread = unread,
                        onBell = { notifOpen = true },
                    )
                }
            }
            if (!(tab == AppTab.TUTOR && tutorChatOpen)) {
                BottomNav(tab = tab, onSelect = { tab = it })
            }
        }

        val current = fullScreen
        fun closeCurrent() {
            // El detalle de materia vuelve a la pestaña desde la que se abrió
            if (current is FullScreen.SubjectDetail) tab = current.origin
            fullScreen = null
            refreshTick++
        }
        // El botón back del sistema cierra la vista abierta en vez de la app
        PlatformBackHandler(enabled = current != null, onBack = ::closeCurrent)
        PlatformBackHandler(enabled = notifOpen) {
            notifOpen = false
            refreshTick++
        }
        if (current != null) {
            FullScreenView(screen = current, onBack = ::closeCurrent)
        }
        if (notifOpen) {
            NotificationsOverlay(
                onClose = {
                    notifOpen = false
                    refreshTick++
                },
            )
        }
    }
}

@Composable
private fun DemoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StudyHubColors.Peach)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(Icons.Outlined.AutoAwesome, tint = StudyHubColors.Accent, size = 15.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Modo demo: inicia sesión para ver tus datos reales",
            color = StudyHubColors.AccentDark,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─── Banner sin conexión ────────────────────────────────────────────────

@Composable
private fun OfflineBanner(pending: Int, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StudyHubColors.Rose)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(Icons.Outlined.Notifications, tint = StudyHubColors.Danger, size = 15.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (pending > 0) {
                "Sin conexión · $pending cambio${if (pending == 1) "" else "s"} pendiente${if (pending == 1) "" else "s"} de sincronizar"
            } else {
                "Sin conexión · mostrando datos guardados"
            },
            color = StudyHubColors.DangerLight,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Reintentar",
            color = StudyHubColors.DangerLight,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRetry)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// ─── Barra de navegación inferior ──────────────────────────────────────

@Composable
private fun BottomNav(tab: AppTab, onSelect: (AppTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(StudyHubColors.Surface)
            .border(1.dp, StudyHubColors.GlassBorder, RoundedCornerShape(26.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        AppTab.entries.forEach { item ->
            val active = item == tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelect(item) }
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (active) StudyHubColors.SageSoft else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        icon = item.icon,
                        tint = if (active) StudyHubColors.Primary else StudyHubColors.TextTertiary,
                        size = 19.dp,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.label,
                    color = if (active) StudyHubColors.Primary else StudyHubColors.TextTertiary,
                    fontSize = 9.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

// ─── Overlay de vista completa ──────────────────────────────────────────

@Composable
private fun FullScreenView(screen: FullScreen, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(StudyHubColors.Bg)) {
        when (screen) {
            is FullScreen.Space -> SecondaryScreen(target = screen.target, onBack = onBack)
            is FullScreen.SubjectDetail -> SubjectDetailScreen(
                subjectId = screen.subjectId,
                subjectName = screen.subjectName,
                colorHex = screen.colorHex,
                onBack = onBack,
            )
        }
    }
}

@Composable
fun FullScreenBackBar(title: String, subtitle: String? = null, onBack: () -> Unit) {
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
                .border(1.dp, StudyHubColors.GlassBorder, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(Icons.AutoMirrored.Outlined.ArrowBack, tint = StudyHubColors.TextPrimary, size = 19.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = StudyHubColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = StudyHubColors.TextTertiary,
                    fontSize = 11.5.sp,
                )
            }
        }
    }
}
