package org.studyhub.project

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.launch
import org.studyhub.project.net.Api
import org.studyhub.project.net.ApiException
import org.studyhub.project.net.GoogleOAuth
import org.studyhub.project.net.TokenStore
import org.studyhub.project.net.logError
import org.studyhub.project.ui.icons.AppIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AuthMode { SIGN_IN, SIGN_UP }

/** Pasos del flujo de autenticación dentro del login. */
private enum class AuthStep { FORM, VERIFY, FORGOT }

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    onEnterDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(AuthStep.FORM) }
    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Verificación de email (después de registrarse)
    var verifyEmail by remember { mutableStateOf("") }
    var verifyCode by remember { mutableStateOf("") }

    // Recuperación de contraseña
    var forgotSent by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var resetToken by remember { mutableStateOf("") }
    var resetPassword by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    fun submit() {
        if (loading) return
        error = null
        successMessage = null
        if (email.isBlank() || password.isBlank()) {
            error = "Completa tu correo y contraseña"
            return
        }
        if (mode == AuthMode.SIGN_UP) {
            if (firstName.isBlank() || lastName.isBlank()) {
                error = "Ingresa tu nombre y apellido"
                return
            }
            if (password.length < 6) {
                error = "La contraseña debe tener al menos 6 caracteres"
                return
            }
            if (confirmPassword != password) {
                error = "Las contraseñas no coinciden"
                return
            }
        }
        loading = true
        scope.launch {
            try {
                if (mode == AuthMode.SIGN_IN) {
                    val response = Api.client.login(email, password)
                    if (response.accessToken.isNotBlank()) {
                        TokenStore.save(response.accessToken)
                        onAuthenticated()
                    } else {
                        error = "Respuesta del servidor sin token"
                    }
                } else {
                    val response = Api.client.register(firstName, lastName, email, password)
                    if (response.accessToken.isNotBlank()) {
                        TokenStore.save(response.accessToken)
                        verifyEmail = email
                        step = AuthStep.VERIFY
                    } else {
                        error = "Respuesta del servidor sin token"
                    }
                }
            } catch (e: ApiException) {
                error = e.message ?: "Error de autenticación"
            } catch (e: Exception) {
                // Log del error REAL (clase + mensaje) para diagnosticar en logcat
                logError("Login", "Error inesperado en login: ${e::class.simpleName}: ${e.message}")
                e.printStackTrace()
                error = "No se pudo conectar con el servidor"
            } finally {
                loading = false
            }
        }
    }

    fun verify() {
        if (loading) return
        error = null
        successMessage = null
        if (verifyCode.length != 6) {
            error = "El código tiene 6 dígitos"
            return
        }
        loading = true
        scope.launch {
            try {
                val message = Api.client.verifyEmail(verifyEmail, verifyCode)
                successMessage = message
                loading = false
                // Entra a la app: el token de registro ya está guardado
                onAuthenticated()
            } catch (e: ApiException) {
                error = e.message ?: "Código inválido"
                loading = false
            } catch (e: Exception) {
                error = "No se pudo conectar con el servidor"
                loading = false
            }
        }
    }

    fun resendCode() {
        if (loading) return
        error = null
        loading = true
        scope.launch {
            try {
                successMessage = Api.client.resendCode(verifyEmail)
            } catch (e: Exception) {
                error = e.message ?: "No se pudo reenviar el código"
            } finally {
                loading = false
            }
        }
    }

    fun sendForgot() {
        if (loading) return
        error = null
        successMessage = null
        if (forgotEmail.isBlank()) {
            error = "Ingresa tu correo"
            return
        }
        loading = true
        scope.launch {
            try {
                successMessage = Api.client.forgotPassword(forgotEmail)
                forgotSent = true
            } catch (e: ApiException) {
                error = e.message ?: "No se pudo enviar el código"
            } catch (e: Exception) {
                error = "No se pudo conectar con el servidor"
            } finally {
                loading = false
            }
        }
    }

    fun doReset() {
        if (loading) return
        error = null
        successMessage = null
        if (resetToken.isBlank() || resetPassword.length < 6) {
            error = "Ingresa el token del correo y una contraseña de al menos 6 caracteres"
            return
        }
        loading = true
        scope.launch {
            try {
                successMessage = Api.client.resetPassword(resetToken, resetPassword)
                loading = false
                // Vuelve al login con la contraseña nueva
                forgotSent = false
                resetToken = ""
                resetPassword = ""
                step = AuthStep.FORM
                mode = AuthMode.SIGN_IN
            } catch (e: ApiException) {
                error = e.message ?: "No se pudo restablecer la contraseña"
                loading = false
            } catch (e: Exception) {
                error = "No se pudo conectar con el servidor"
                loading = false
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(StudyHubColors.Bg)) {
        // Orbes de luz difuminados (como auth.css de la web: blur 140px, opacity .15)
        Orb(
            modifier = Modifier
                .size(420.dp)
                .align(Alignment.TopEnd)
                .offset(x = 110.dp, y = (-130).dp),
            color = StudyHubColors.Primary,
        )
        Orb(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-120).dp, y = 150.dp),
            color = StudyHubColors.Secondary,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GraduationCapLogo()
            Spacer(Modifier.height(16.dp))
            Text(
                text = "StudyHub",
                color = StudyHubColors.TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tu gestión académica con IA: materias, tareas, tutor IA, rutas de aprendizaje y CV en un solo lugar.",
                color = StudyHubColors.TextSecondary,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(26.dp))

            when (step) {
                AuthStep.FORM -> GlassCard {
                    SegmentedAuth(mode = mode, onSelect = { mode = it })
                    Spacer(Modifier.height(16.dp))
                    if (mode == AuthMode.SIGN_UP) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AuthField(
                                label = "Nombre",
                                value = firstName,
                                onValueChange = { firstName = it },
                                modifier = Modifier.weight(1f),
                            )
                            AuthField(
                                label = "Apellido",
                                value = lastName,
                                onValueChange = { lastName = it },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    AuthField(label = "Correo electrónico", value = email, onValueChange = { email = it })
                    Spacer(Modifier.height(10.dp))
                    AuthField(
                        label = "Contraseña",
                        value = password,
                        onValueChange = { password = it },
                        isPassword = true,
                    )
                    if (mode == AuthMode.SIGN_UP) {
                        Spacer(Modifier.height(10.dp))
                        AuthField(
                            label = "Confirmar contraseña",
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            isPassword = true,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (error != null) {
                        AuthMessage(message = error.orEmpty(), color = StudyHubColors.DangerLight)
                    }
                    if (successMessage != null) {
                        AuthMessage(message = successMessage.orEmpty(), color = StudyHubColors.SecondaryLight)
                    }
                    PrimaryButton(
                        label = if (mode == AuthMode.SIGN_IN) "Iniciar sesión" else "Crear cuenta",
                        loading = loading,
                    ) { submit() }
                    Spacer(Modifier.height(11.dp))
                    GoogleButton {
                        // Abre el navegador con GET /auth/google; el token vuelve por deep link studyhub://
                        uriHandler.openUri(GoogleOAuth.authUrl())
                    }
                    if (mode == AuthMode.SIGN_IN) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            color = StudyHubColors.TextTertiary,
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    forgotEmail = email
                                    forgotSent = false
                                    resetToken = ""
                                    resetPassword = ""
                                    error = null
                                    successMessage = null
                                    step = AuthStep.FORGOT
                                }
                                .padding(6.dp),
                        )
                    }
                }

                AuthStep.VERIFY -> GlassCard {
                    Text(
                        text = "Verifica tu correo",
                        color = StudyHubColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Te enviamos un código de 6 dígitos a\n$verifyEmail",
                        color = StudyHubColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    AuthField(
                        label = "Código de verificación",
                        value = verifyCode,
                        onValueChange = { verifyCode = it.filter { c -> c.isDigit() }.take(6) },
                    )
                    Spacer(Modifier.height(14.dp))
                    if (error != null) {
                        AuthMessage(message = error.orEmpty(), color = StudyHubColors.DangerLight)
                    }
                    if (successMessage != null) {
                        AuthMessage(message = successMessage.orEmpty(), color = StudyHubColors.SecondaryLight)
                    }
                    PrimaryButton(label = "Verificar email", loading = loading) { verify() }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = "Reenviar código",
                            color = StudyHubColors.PrimaryLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { resendCode() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Verificar después →",
                        color = StudyHubColors.TextTertiary,
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAuthenticated() }
                            .padding(6.dp),
                    )
                }

                AuthStep.FORGOT -> GlassCard {
                    Text(
                        text = "Recuperar contraseña",
                        color = StudyHubColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    if (!forgotSent) {
                        Text(
                            text = "Te enviaremos un código a tu correo para restablecer tu contraseña.",
                            color = StudyHubColors.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(16.dp))
                        AuthField(
                            label = "Correo electrónico",
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                        )
                        Spacer(Modifier.height(14.dp))
                        if (error != null) {
                            AuthMessage(message = error.orEmpty(), color = StudyHubColors.DangerLight)
                        }
                        if (successMessage != null) {
                            AuthMessage(message = successMessage.orEmpty(), color = StudyHubColors.SecondaryLight)
                        }
                        PrimaryButton(label = "Enviar código", loading = loading) { sendForgot() }
                    } else {
                        Text(
                            text = "Revisa tu correo: recibiste un enlace con un token. Pégalo aquí junto con tu nueva contraseña.",
                            color = StudyHubColors.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(16.dp))
                        AuthField(
                            label = "Token del enlace (de tu correo)",
                            value = resetToken,
                            onValueChange = { resetToken = it },
                        )
                        Spacer(Modifier.height(10.dp))
                        AuthField(
                            label = "Nueva contraseña",
                            value = resetPassword,
                            onValueChange = { resetPassword = it },
                            isPassword = true,
                        )
                        Spacer(Modifier.height(14.dp))
                        if (error != null) {
                            AuthMessage(message = error.orEmpty(), color = StudyHubColors.DangerLight)
                        }
                        if (successMessage != null) {
                            AuthMessage(message = successMessage.orEmpty(), color = StudyHubColors.SecondaryLight)
                        }
                        PrimaryButton(label = "Restablecer contraseña", loading = loading) { doReset() }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                step = AuthStep.FORM
                                error = null
                                successMessage = null
                            }
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, tint = StudyHubColors.TextTertiary, size = 16.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Volver al inicio de sesión",
                            color = StudyHubColors.TextTertiary,
                            fontSize = 12.5.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            if (step == AuthStep.FORM) {
                DemoButton(onClick = onEnterDemo)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Acceso protegido · JWT + Google OAuth\nHecha para estudiantes, por estudiantes",
                color = StudyHubColors.TextTertiary,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AuthMessage(message: String, color: Color) {
    Text(
        text = message,
        color = color,
        fontSize = 12.5.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    )
}

/** Orbe de luz: gradiente radial que se desvanece (simula el blur de la web). */
@Composable
private fun Orb(modifier: Modifier = Modifier, color: Color) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.10f), color.copy(alpha = 0f)),
                ),
            ),
    )
}

/**
 * Logo oficial: badge con gradiente indigo + el birrete de graduación dibujado con
 * Canvas en blanco puro. Se pinta directamente (sin depender del tinte de Icon) para
 * que el birrete siempre se vea, y el gradiente es oscuro para máximo contraste.
 */
@Composable
private fun GraduationCapLogo(modifier: Modifier = Modifier) {
    val capPath = remember {
        Path().apply {
            // Mortaja (diamante superior)
            moveTo(24f, 8f)
            lineTo(42f, 24f)
            lineTo(24f, 36f)
            lineTo(6f, 24f)
            close()

            // Botón superior (diamante pequeño)
            moveTo(24f, 3.1f)
            lineTo(26.4f, 5.5f)
            lineTo(24f, 7.9f)
            lineTo(21.6f, 5.5f)
            close()

            // Banda inferior con esquinas redondeadas (curvas cuadráticas ≈ arcos de radio 2)
            moveTo(12f, 33f)
            relativeLineTo(24f, 0f)
            quadraticBezierTo(38f, 33f, 38f, 35f)
            relativeLineTo(0f, 1f)
            quadraticBezierTo(38f, 38f, 36f, 38f)
            relativeLineTo(-24f, 0f)
            quadraticBezierTo(10f, 38f, 10f, 36f)
            relativeLineTo(0f, -1f)
            quadraticBezierTo(10f, 33f, 12f, 33f)
            close()

            // Borla (cordón)
            moveTo(35.6f, 26.8f)
            lineTo(38.2f, 28.4f)
            lineTo(43.8f, 42.8f)
            lineTo(41.2f, 44f)
            close()

            // Punta de la borla (círculo ≈ 4 curvas cuadráticas, centro (42, 42.4) radio 2.6)
            moveTo(42f, 39.8f)
            quadraticBezierTo(44.6f, 39.8f, 44.6f, 42.4f)
            quadraticBezierTo(44.6f, 45f, 42f, 45f)
            quadraticBezierTo(39.4f, 45f, 39.4f, 42.4f)
            quadraticBezierTo(39.4f, 39.8f, 42f, 39.8f)
            close()
        }
    }
    Box(
        modifier = modifier
            .size(84.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(StudyHubColors.Primary, StudyHubColors.PrimaryDark))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(58.dp)) {
            // Las coordenadas del path están en un viewport de 48 unidades; se escala al tamaño real.
            val s = size.minDimension / 48f
            scale(s, s, pivot = Offset.Zero) {
                drawPath(capPath, Color.White)
            }
        }
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(StudyHubColors.Glass)
            .border(1.dp, StudyHubColors.GlassBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 22.dp, vertical = 22.dp),
        content = content,
    )
}

@Composable
private fun SegmentedAuth(mode: AuthMode, onSelect: (AuthMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(StudyHubColors.SurfaceLight)
            .border(1.dp, StudyHubColors.GlassBorder, RoundedCornerShape(999.dp))
            .padding(4.dp),
    ) {
        SegmentedItem("Iniciar sesión", active = mode == AuthMode.SIGN_IN) { onSelect(AuthMode.SIGN_IN) }
        SegmentedItem("Crear cuenta", active = mode == AuthMode.SIGN_UP) { onSelect(AuthMode.SIGN_UP) }
    }
}

@Composable
private fun RowScope.SegmentedItem(label: String, active: Boolean, onClick: () -> Unit) {
    val background = if (active) {
        Modifier.background(
            Brush.linearGradient(listOf(StudyHubColors.Primary, StudyHubColors.PrimaryDark)),
        )
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(999.dp))
            .then(background)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) Color.White else StudyHubColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        textStyle = TextStyle(color = StudyHubColors.TextPrimary, fontSize = 14.sp),
        placeholder = {
            Text(label, color = StudyHubColors.TextTertiary, fontSize = 14.sp)
        },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = StudyHubColors.Primary,
            unfocusedBorderColor = StudyHubColors.Border,
            focusedContainerColor = StudyHubColors.Surface,
            unfocusedContainerColor = StudyHubColors.Surface,
            focusedTextColor = StudyHubColors.TextPrimary,
            unfocusedTextColor = StudyHubColors.TextPrimary,
            cursorColor = StudyHubColors.Primary,
            focusedPlaceholderColor = StudyHubColors.TextTertiary,
            unfocusedPlaceholderColor = StudyHubColors.TextTertiary,
            focusedLabelColor = StudyHubColors.PrimaryLight,
            unfocusedLabelColor = StudyHubColors.TextSecondary,
        ),
    )
}

@Composable
private fun PrimaryButton(label: String, loading: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, spotColor = StudyHubColors.Primary.copy(alpha = 0.35f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(StudyHubColors.Primary, StudyHubColors.PrimaryDark)))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GoogleButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudyHubColors.SurfaceLight)
            .border(1.dp, StudyHubColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "G",
                color = StudyHubColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Continuar con Google",
            color = StudyHubColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DemoButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, StudyHubColors.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Ver demo sin cuenta",
            color = StudyHubColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
