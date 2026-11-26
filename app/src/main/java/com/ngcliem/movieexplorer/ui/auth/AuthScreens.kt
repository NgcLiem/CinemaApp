package com.ngcliem.movieexplorer.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.ui.components.LanguageSwitcher
import com.ngcliem.movieexplorer.ui.components.asString
import com.ngcliem.movieexplorer.ui.components.toUiText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngcliem.movieexplorer.ui.components.LightStatusBarIcons
import kotlin.math.PI
import kotlin.math.sin

private val CinemaRed = Color(0xFFE50914)

// ---------------------------------------------------------------------------
// Login
// ---------------------------------------------------------------------------

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    LaunchedEffect(state.isSuccess) { if (state.isSuccess) onLoggedIn() }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(24.dp))
            AnimatedLogo()
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.login_title), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.login_subtitle), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))

            SlideInCard {
                AuthTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = stringResource(R.string.field_email),
                    icon = Icons.Outlined.Email,
                    error = state.emailError?.toUiText()?.asString(),
                    keyboardType = KeyboardType.Email,
                    onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                )
                PasswordField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = stringResource(R.string.field_password),
                    error = state.passwordError?.toUiText()?.asString(),
                    imeAction = ImeAction.Done,
                    onDone = { focus.clearFocus(); viewModel.login() },
                )
                ErrorBanner(state.errorMessage?.asString())
                PrimaryButton(text = stringResource(R.string.login_button), isLoading = state.isLoading, onClick = viewModel::login)
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.no_account), color = Color.White.copy(alpha = 0.8f))
                TextButton(onClick = onRegisterClick) {
                    Text(stringResource(R.string.register_now), color = Color(0xFFFFC94D), fontWeight = FontWeight.Bold)
                }
            }
        }
        // Drawn last so it stays on top of the scrolling form.
        LanguageSwitcher(
            current = language,
            onSelect = viewModel::setLanguage,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(12.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Register
// ---------------------------------------------------------------------------

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val next = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down); Unit }
    LaunchedEffect(state.isSuccess) { if (state.isSuccess) onRegistered() }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedLogo(size = 64)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.register_title), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.register_subtitle), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))

            SlideInCard {
                AuthTextField(
                    value = state.fullName,
                    onValueChange = { viewModel.onChange(RegisterField.NAME, it) },
                    label = stringResource(R.string.field_full_name),
                    icon = Icons.Outlined.Person,
                    error = state.errors[RegisterField.NAME]?.toUiText()?.asString(),
                    onNext = next,
                )
                AuthTextField(
                    value = state.email,
                    onValueChange = { viewModel.onChange(RegisterField.EMAIL, it) },
                    label = stringResource(R.string.field_email),
                    icon = Icons.Outlined.Email,
                    error = state.errors[RegisterField.EMAIL]?.toUiText()?.asString(),
                    keyboardType = KeyboardType.Email,
                    onNext = next,
                )
                AuthTextField(
                    value = state.phone,
                    onValueChange = { viewModel.onChange(RegisterField.PHONE, it) },
                    label = stringResource(R.string.field_phone),
                    icon = Icons.Outlined.Phone,
                    error = state.errors[RegisterField.PHONE]?.toUiText()?.asString(),
                    keyboardType = KeyboardType.Phone,
                    onNext = next,
                )
                PasswordField(
                    value = state.password,
                    onValueChange = { viewModel.onChange(RegisterField.PASSWORD, it) },
                    label = stringResource(R.string.field_password),
                    error = state.errors[RegisterField.PASSWORD]?.toUiText()?.asString(),
                    onNext = next,
                )
                PasswordStrengthBar(strength = state.passwordStrength, visible = state.password.isNotEmpty())
                PasswordField(
                    value = state.confirm,
                    onValueChange = { viewModel.onChange(RegisterField.CONFIRM, it) },
                    label = stringResource(R.string.field_confirm_password),
                    error = state.errors[RegisterField.CONFIRM]?.toUiText()?.asString(),
                    imeAction = ImeAction.Done,
                    onDone = { focus.clearFocus() },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.acceptedTerms, onCheckedChange = viewModel::onTermsChange)
                    Text(stringResource(R.string.accept_terms), style = MaterialTheme.typography.bodySmall)
                }
                state.errors[RegisterField.TERMS]?.let {
                    Text(it.toUiText().asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                ErrorBanner(state.errorMessage?.asString())
                PrimaryButton(text = stringResource(R.string.register_button), isLoading = state.isLoading, onClick = { focus.clearFocus(); viewModel.register() })
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.have_account), color = Color.White.copy(alpha = 0.8f))
                TextButton(onClick = onLoginClick) {
                    Text(stringResource(R.string.login_button), color = Color(0xFFFFC94D), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------

/** Dark cinema background with slowly moving colored glows and twinkling "projector" dots. */
@Composable
private fun AuthBackground(content: @Composable BoxScope.() -> Unit) {
    LightStatusBarIcons()
    val transition = rememberInfiniteTransition(label = "authBg")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label = "phase",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0B14), Color(0xFF1A0B1E), Color(0xFF0B0B14)))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val angle = phase * 2 * PI
            val glow1 = Offset(
                x = size.width * (0.2f + 0.15f * sin(angle).toFloat()),
                y = size.height * (0.18f + 0.06f * sin(angle * 2).toFloat()),
            )
            val glow2 = Offset(
                x = size.width * (0.85f - 0.15f * sin(angle + 1).toFloat()),
                y = size.height * (0.8f + 0.05f * sin(angle * 2 + 2).toFloat()),
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(CinemaRed.copy(alpha = 0.45f), Color.Transparent), glow1, size.width * 0.7f),
                radius = size.width * 0.7f,
                center = glow1,
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF6A3DE8).copy(alpha = 0.4f), Color.Transparent), glow2, size.width * 0.75f),
                radius = size.width * 0.75f,
                center = glow2,
            )
            // Twinkling dots.
            repeat(28) { i ->
                val x = ((i * 73) % 100) / 100f * size.width
                val y = ((i * 37 + 11) % 100) / 100f * size.height
                val twinkle = ((sin(angle * 3 + i).toFloat() + 1f) / 2f)
                drawCircle(Color.White.copy(alpha = 0.08f + 0.35f * twinkle), radius = 1.5f + 1.5f * twinkle, center = Offset(x, y))
            }
        }
        content()
    }
}

/** Logo that pops in with a spring, then gently pulses. */
@Composable
private fun AnimatedLogo(size: Int = 88) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val pulse by rememberInfiniteTransition(label = "logoPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        modifier = Modifier
            .size(size.dp)
            .graphicsLayer {
                val s = entrance.value * pulse
                scaleX = s
                scaleY = s
                rotationZ = (1f - entrance.value) * -90f
            }
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(CinemaRed, Color(0xFFFF6A00)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size((size / 2).dp))
    }
}

@Composable
private fun SlideInCard(content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(700, delayMillis = 150, easing = FastOutSlowInEasing)) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * 120f
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    onNext: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    imeAction: ImeAction = ImeAction.Next,
    onNext: () -> Unit = {},
    onDone: () -> Unit = {},
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(if (visible) R.string.hide_password else R.string.show_password),
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun PasswordStrengthBar(strength: Int, visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        val (label, color) = when (strength) {
            0, 1 -> stringResource(R.string.strength_weak) to Color(0xFFE53935)
            2 -> stringResource(R.string.strength_medium) to Color(0xFFFFA000)
            3 -> stringResource(R.string.strength_good) to Color(0xFF7CB342)
            else -> stringResource(R.string.strength_strong) to Color(0xFF2E7D32)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(4) { index ->
                    val segmentColor by animateColorAsState(
                        if (index < strength) color else MaterialTheme.colorScheme.surfaceVariant,
                        label = "strength$index",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(segmentColor),
                    )
                }
            }
            Text(stringResource(R.string.password_strength, label), style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
internal fun ErrorBanner(message: String?) {
    AnimatedVisibility(visible = message != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Text(
            text = message.orEmpty(),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(12.dp),
        )
    }
}

@Composable
internal fun PrimaryButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CinemaRed, contentColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}
