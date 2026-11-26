package com.ngcliem.movieexplorer.ui.profile

import com.ngcliem.movieexplorer.ui.components.AppDialog
import com.ngcliem.movieexplorer.ui.components.DialogPrimaryButton
import com.ngcliem.movieexplorer.ui.components.DialogSecondaryButton
import com.ngcliem.movieexplorer.ui.components.DialogTone
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.ui.auth.ChangePasswordField
import com.ngcliem.movieexplorer.ui.auth.ChangePasswordViewModel
import com.ngcliem.movieexplorer.ui.auth.ErrorBanner
import com.ngcliem.movieexplorer.ui.auth.PasswordField
import com.ngcliem.movieexplorer.ui.auth.PasswordStrengthBar
import com.ngcliem.movieexplorer.ui.auth.PrimaryButton
import com.ngcliem.movieexplorer.ui.components.asString
import com.ngcliem.movieexplorer.ui.components.toUiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.change_password_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.LockReset, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
            }
            Text(
                text = stringResource(R.string.change_password_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PasswordField(
                value = state.current,
                onValueChange = { viewModel.onChange(ChangePasswordField.CURRENT, it) },
                label = stringResource(R.string.field_current_password),
                error = state.currentError?.toUiText()?.asString(),
            )
            PasswordField(
                value = state.new,
                onValueChange = { viewModel.onChange(ChangePasswordField.NEW, it) },
                label = stringResource(R.string.field_new_password),
                error = state.newError?.toUiText()?.asString(),
            )
            PasswordStrengthBar(strength = state.strength, visible = state.new.isNotEmpty())
            PasswordField(
                value = state.confirm,
                onValueChange = { viewModel.onChange(ChangePasswordField.CONFIRM, it) },
                label = stringResource(R.string.field_confirm_new_password),
                error = state.confirmError?.toUiText()?.asString(),
                imeAction = ImeAction.Done,
                onDone = { focus.clearFocus(); viewModel.submit() },
            )
            ErrorBanner(state.errorMessage?.asString())
            PrimaryButton(
                text = stringResource(R.string.action_save),
                isLoading = state.isLoading,
                onClick = { focus.clearFocus(); viewModel.submit() },
            )
        }
    }

    if (state.isSuccess) {
        val pop = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
        }
        AppDialog(
            onDismissRequest = onBack,
            tone = DialogTone.SUCCESS,
            iconContent = {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = DialogTone.SUCCESS.accent,
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                )
            },
            title = stringResource(R.string.password_changed),
            confirmButton = { DialogPrimaryButton(stringResource(R.string.action_close), onClick = onBack) },
        )
    }
}
