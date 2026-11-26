package com.ngcliem.movieexplorer.domain.validation

/** Everything that can go wrong in the account forms; the UI turns each into a message. */
enum class AuthError {
    NAME_EMPTY,
    NAME_TOO_SHORT,
    EMAIL_EMPTY,
    EMAIL_INVALID,
    PHONE_EMPTY,
    PHONE_INVALID,
    PASSWORD_EMPTY,
    PASSWORD_TOO_SHORT,
    PASSWORD_WEAK,
    CONFIRM_EMPTY,
    CONFIRM_MISMATCH,
    TERMS_NOT_ACCEPTED,
    EMAIL_TAKEN,
    WRONG_CREDENTIALS,
    WRONG_CURRENT_PASSWORD,
    SAME_PASSWORD,
    NOT_LOGGED_IN,
    NO_SEATS,
    SEATS_TAKEN,
}

/** Form checks shared by the account screens. Each returns an error or null. */
object AuthValidator {

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val phoneRegex = Regex("^(0|\\+84)[0-9]{9}$")

    const val MIN_PASSWORD_LENGTH = 6

    fun validateName(name: String): AuthError? = when {
        name.isBlank() -> AuthError.NAME_EMPTY
        name.trim().length < 2 -> AuthError.NAME_TOO_SHORT
        else -> null
    }

    fun validateEmail(email: String): AuthError? = when {
        email.isBlank() -> AuthError.EMAIL_EMPTY
        !emailRegex.matches(email.trim()) -> AuthError.EMAIL_INVALID
        else -> null
    }

    fun validatePhone(phone: String): AuthError? = when {
        phone.isBlank() -> AuthError.PHONE_EMPTY
        !phoneRegex.matches(phone.trim().replace(" ", "")) -> AuthError.PHONE_INVALID
        else -> null
    }

    fun validatePassword(password: String): AuthError? = when {
        password.isEmpty() -> AuthError.PASSWORD_EMPTY
        password.length < MIN_PASSWORD_LENGTH -> AuthError.PASSWORD_TOO_SHORT
        password.none { it.isDigit() } || password.none { it.isLetter() } -> AuthError.PASSWORD_WEAK
        else -> null
    }

    fun validateConfirm(password: String, confirm: String): AuthError? = when {
        confirm.isEmpty() -> AuthError.CONFIRM_EMPTY
        confirm != password -> AuthError.CONFIRM_MISMATCH
        else -> null
    }

    /** 0..4, used by the password strength bar. */
    fun passwordStrength(password: String): Int {
        if (password.isEmpty()) return 0
        var score = 0
        if (password.length >= MIN_PASSWORD_LENGTH) score++
        if (password.length >= 10) score++
        if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++
        if (password.any { !it.isLetterOrDigit() } ||
            (password.any { it.isUpperCase() } && password.any { it.isLowerCase() })
        ) {
            score++
        }
        return score
    }
}

/** An expected failure whose [reason] the UI can explain to the user. */
class AuthException(val reason: AuthError) : Exception(reason.name)
