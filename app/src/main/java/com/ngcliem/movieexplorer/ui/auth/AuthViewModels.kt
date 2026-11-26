package com.ngcliem.movieexplorer.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngcliem.movieexplorer.data.settings.AppLanguage
import com.ngcliem.movieexplorer.data.settings.LanguageStore
import com.ngcliem.movieexplorer.domain.repository.AuthRepository
import com.ngcliem.movieexplorer.domain.validation.AuthError
import com.ngcliem.movieexplorer.domain.validation.AuthValidator
import com.ngcliem.movieexplorer.ui.components.UiText
import com.ngcliem.movieexplorer.ui.components.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: AuthError? = null,
    val passwordError: AuthError? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val languageStore: LanguageStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val language: StateFlow<AppLanguage> = languageStore.language

    fun setLanguage(language: AppLanguage) = languageStore.setLanguage(language)

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return
        val emailError = AuthValidator.validateEmail(state.email)
        val passwordError = if (state.password.isEmpty()) AuthError.PASSWORD_EMPTY else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.login(state.email, state.password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.toUiText()) } }
        }
    }
}

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirm: String = "",
    val acceptedTerms: Boolean = false,
    val errors: Map<RegisterField, AuthError> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isSuccess: Boolean = false,
) {
    val passwordStrength: Int get() = AuthValidator.passwordStrength(password)
}

enum class RegisterField { NAME, EMAIL, PHONE, PASSWORD, CONFIRM, TERMS }

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onChange(field: RegisterField, value: String) = _uiState.update {
        val updated = when (field) {
            RegisterField.NAME -> it.copy(fullName = value)
            RegisterField.EMAIL -> it.copy(email = value)
            RegisterField.PHONE -> it.copy(phone = value)
            RegisterField.PASSWORD -> it.copy(password = value)
            RegisterField.CONFIRM -> it.copy(confirm = value)
            RegisterField.TERMS -> it
        }
        updated.copy(errors = it.errors - field, errorMessage = null)
    }

    fun onTermsChange(accepted: Boolean) = _uiState.update {
        it.copy(acceptedTerms = accepted, errors = it.errors - RegisterField.TERMS)
    }

    fun register() {
        val s = _uiState.value
        if (s.isLoading) return
        val errors = buildMap {
            AuthValidator.validateName(s.fullName)?.let { put(RegisterField.NAME, it) }
            AuthValidator.validateEmail(s.email)?.let { put(RegisterField.EMAIL, it) }
            AuthValidator.validatePhone(s.phone)?.let { put(RegisterField.PHONE, it) }
            AuthValidator.validatePassword(s.password)?.let { put(RegisterField.PASSWORD, it) }
            AuthValidator.validateConfirm(s.password, s.confirm)?.let { put(RegisterField.CONFIRM, it) }
            if (!s.acceptedTerms) put(RegisterField.TERMS, AuthError.TERMS_NOT_ACCEPTED)
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.register(s.fullName, s.email, s.phone, s.password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.toUiText()) } }
        }
    }
}

data class ChangePasswordUiState(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val currentError: AuthError? = null,
    val newError: AuthError? = null,
    val confirmError: AuthError? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isSuccess: Boolean = false,
) {
    val strength: Int get() = AuthValidator.passwordStrength(new)
}

enum class ChangePasswordField { CURRENT, NEW, CONFIRM }

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onChange(field: ChangePasswordField, value: String) = _uiState.update {
        when (field) {
            ChangePasswordField.CURRENT -> it.copy(current = value, currentError = null)
            ChangePasswordField.NEW -> it.copy(new = value, newError = null)
            ChangePasswordField.CONFIRM -> it.copy(confirm = value, confirmError = null)
        }.copy(errorMessage = null)
    }

    fun submit() {
        val s = _uiState.value
        if (s.isLoading) return
        val currentError = if (s.current.isEmpty()) AuthError.PASSWORD_EMPTY else null
        val newError = AuthValidator.validatePassword(s.new)
            ?: if (s.new == s.current) AuthError.SAME_PASSWORD else null
        val confirmError = AuthValidator.validateConfirm(s.new, s.confirm)
        if (currentError != null || newError != null || confirmError != null) {
            _uiState.update { it.copy(currentError = currentError, newError = newError, confirmError = confirmError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.changePassword(s.current, s.new)
                .onSuccess { _uiState.update { ChangePasswordUiState(isSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.toUiText()) } }
        }
    }
}
