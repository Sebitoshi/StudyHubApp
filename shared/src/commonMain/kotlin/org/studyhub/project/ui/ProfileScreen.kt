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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.studyhub.project.StudyHubColors
import org.studyhub.project.net.Api
import org.studyhub.project.net.DashboardSummary
import org.studyhub.project.net.ProfileAcademic
import org.studyhub.project.net.TokenStore
import org.studyhub.project.ui.icons.AppIcon

@Composable
fun ProfileScreen(onLogout: () -> Unit, onOpenSpace: (SpaceTarget) -> Unit) {
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var academic by remember { mutableStateOf<ProfileAcademic?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    val demo = remember { TokenStore.load() == null }

    LaunchedEffect(reload) {
        loading = true
        if (demo) {
            summary = DemoData.dashboard
            academic = DemoData.academic
            error = null
            loading = false
            return@LaunchedEffect
        }
        try {
            summary = Api.client.dashboard()
            academic = runCatching { Api.client.profileAcademic() }.getOrNull()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el perfil"
        } finally {
            loading = false
        }
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
                val name = summary?.user?.nombre?.let { "$it" } ?: "Estudiante"
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                ) {
                    Spacer(Modifier.height(4.dp))
                    SectionTitle("Perfil", icon = Icons.Outlined.Person, iconTint = StudyHubColors.PrimaryLight)
                    Spacer(Modifier.height(14.dp))

                    // Tarjeta de identidad
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialsAvatar(name = name, size = 54.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name, color = StudyHubColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    // Gamificación
                    summary?.gamification?.let { g ->
                        GamificationCard(g)
                        Spacer(Modifier.height(14.dp))
                    }

                    // Perfil académico
                    academic?.let { a ->
                        GlassCard {
                            Text("Perfil académico", color = StudyHubColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            if (a.carrera.isNotBlank()) {
                                InfoRow(Icons.Outlined.MenuBook, "Carrera", a.carrera)
                                Spacer(Modifier.height(8.dp))
                            }
                            if (a.universidad.isNotBlank()) {
                                InfoRow(Icons.Outlined.AccountBalance, "Universidad", a.universidad)
                                Spacer(Modifier.height(8.dp))
                            }
                            if (a.semestreActual > 0) {
                                InfoRow(Icons.Outlined.WorkspacePremium, "Semestre", "${a.semestreActual}")
                                Spacer(Modifier.height(8.dp))
                            }
                            if (a.promedio > 0) {
                                val avg5 = if (a.promedio > 5) a.promedio / 20 else a.promedio
                                val label = ((avg5 * 10).toInt() / 10.0).toString()
                                InfoRow(Icons.Outlined.WorkspacePremium, "Promedio", "$label / 5.0")
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    // Tu espacio: grilla de vistas (Riesgo, Logros, Quizzes, Calendario, Enfoque…)
                    SectionTitle("Tu espacio", icon = Icons.Outlined.GridView, iconTint = StudyHubColors.PrimaryLight)
                    Spacer(Modifier.height(12.dp))
                    SpaceGrid(onOpenSpace = onOpenSpace)
                    Spacer(Modifier.height(14.dp))

                    // Cerrar sesión
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .background(StudyHubColors.Danger.copy(alpha = 0.1f))
                            .clickable(onClick = onLogout)
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(Icons.AutoMirrored.Outlined.Logout, tint = StudyHubColors.DangerLight, size = 19.dp)
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = "Cerrar sesión",
                            color = StudyHubColors.DangerLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
internal fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(icon, tint = StudyHubColors.InfoLight, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(label, color = StudyHubColors.TextTertiary, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
        Text(value, color = StudyHubColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
