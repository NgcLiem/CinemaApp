package com.ngcliem.movieexplorer.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.domain.validation.AuthError
import com.ngcliem.movieexplorer.domain.validation.AuthException
import com.ngcliem.movieexplorer.domain.validation.AuthValidator
import retrofit2.HttpException
import java.io.IOException

/**
 * A message produced outside the UI (e.g. in a ViewModel). It only stores the
 * string resource, so it is shown in whatever language is active on screen.
 */
data class UiText(@StringRes val resId: Int, val args: List<Any> = emptyList())

@Composable
fun UiText.asString(): String = stringResource(resId, *args.toTypedArray())

/** Turns a technical exception into a message a user can understand. */
fun Throwable.toUiText(): UiText = when (this) {
    is AuthException -> reason.toUiText()
    is IOException -> UiText(R.string.error_no_internet)
    is HttpException -> when (code()) {
        401 -> UiText(R.string.error_api_key)
        404 -> UiText(R.string.error_not_found)
        429 -> UiText(R.string.error_too_many)
        else -> UiText(R.string.error_server, listOf(code()))
    }
    else -> UiText(R.string.error_generic)
}

fun AuthError.toUiText(): UiText = when (this) {
    AuthError.NAME_EMPTY -> UiText(R.string.err_name_empty)
    AuthError.NAME_TOO_SHORT -> UiText(R.string.err_name_short)
    AuthError.EMAIL_EMPTY -> UiText(R.string.err_email_empty)
    AuthError.EMAIL_INVALID -> UiText(R.string.err_email_invalid)
    AuthError.PHONE_EMPTY -> UiText(R.string.err_phone_empty)
    AuthError.PHONE_INVALID -> UiText(R.string.err_phone_invalid)
    AuthError.PASSWORD_EMPTY -> UiText(R.string.err_password_empty)
    AuthError.PASSWORD_TOO_SHORT -> UiText(R.string.err_password_short, listOf(AuthValidator.MIN_PASSWORD_LENGTH))
    AuthError.PASSWORD_WEAK -> UiText(R.string.err_password_weak)
    AuthError.CONFIRM_EMPTY -> UiText(R.string.err_confirm_empty)
    AuthError.CONFIRM_MISMATCH -> UiText(R.string.err_confirm_mismatch)
    AuthError.TERMS_NOT_ACCEPTED -> UiText(R.string.err_terms)
    AuthError.EMAIL_TAKEN -> UiText(R.string.err_email_taken)
    AuthError.WRONG_CREDENTIALS -> UiText(R.string.err_wrong_credentials)
    AuthError.WRONG_CURRENT_PASSWORD -> UiText(R.string.err_wrong_current_password)
    AuthError.SAME_PASSWORD -> UiText(R.string.err_same_password)
    AuthError.NOT_LOGGED_IN -> UiText(R.string.err_not_logged_in)
    AuthError.NO_SEATS -> UiText(R.string.err_no_seats)
    AuthError.SEATS_TAKEN -> UiText(R.string.err_seats_taken)
}
