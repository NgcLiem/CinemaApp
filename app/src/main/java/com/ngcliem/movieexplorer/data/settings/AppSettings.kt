package com.ngcliem.movieexplorer.data.settings

import android.content.Context
import android.content.res.Configuration
import com.ngcliem.movieexplorer.domain.repository.SearchHistoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Languages the app is translated into. */
enum class AppLanguage(val code: String, val tmdbCode: String, val nativeName: String) {
    VIETNAMESE("vi", "vi-VN", "Tiếng Việt"),
    ENGLISH("en", "en-US", "English");

    val locale: Locale get() = Locale(code)

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: VIETNAMESE
    }
}

/**
 * Remembers the chosen app language. The static helpers are needed because the
 * Activity reads the language in attachBaseContext, before Hilt injects anything.
 */
@Singleton
class LanguageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _language = MutableStateFlow(read(context))
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        prefs(context).edit().putString(KEY_LANGUAGE, language.code).apply()
        _language.value = language
    }

    companion object {
        private const val PREFS = "settings"
        private const val KEY_LANGUAGE = "language"

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun read(context: Context): AppLanguage = AppLanguage.fromCode(prefs(context).getString(KEY_LANGUAGE, null))

        /** Returns a context whose resources use the saved language. */
        fun wrap(base: Context): Context {
            val locale = read(base).locale
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
            return base.createConfigurationContext(config)
        }
    }
}

/** The last searches typed by the user, newest first. */
@Singleton
class SearchHistoryStore @Inject constructor(
    @ApplicationContext context: Context,
) : SearchHistoryRepository {
    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow(load())
    override val history: StateFlow<List<String>> = _history.asStateFlow()

    override fun add(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        save(listOf(clean) + _history.value.filterNot { it.equals(clean, ignoreCase = true) })
    }

    override fun remove(query: String) = save(_history.value - query)

    override fun clear() = save(emptyList())

    private fun save(list: List<String>) {
        val trimmed = list.take(MAX_ITEMS)
        prefs.edit().putString(KEY, trimmed.joinToString(SEPARATOR)).apply()
        _history.value = trimmed
    }

    private fun load(): List<String> =
        prefs.getString(KEY, null)?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    private companion object {
        const val KEY = "items"
        const val SEPARATOR = "\u001F"
        const val MAX_ITEMS = 10
    }
}
