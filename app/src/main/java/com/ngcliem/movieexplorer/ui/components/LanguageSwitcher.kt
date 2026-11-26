package com.ngcliem.movieexplorer.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ngcliem.movieexplorer.data.settings.AppLanguage

/**
 * Saves the new language, then recreates the Activity so every screen reloads
 * its texts from the matching strings.xml.
 */
@Composable
fun rememberLanguageChanger(onSave: (AppLanguage) -> Unit): (AppLanguage) -> Unit {
    val context = LocalContext.current
    return { language ->
        onSave(language)
        context.findActivity()?.recreate()
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Small "VI | EN" toggle, used on the login screen. */
@Composable
fun LanguageSwitcher(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val change = rememberLanguageChanger(onSelect)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .padding(3.dp),
    ) {
        AppLanguage.entries.forEach { language ->
            val selected = language == current
            val bg by animateColorAsState(
                if (selected) Color(0xFFE50914) else Color.Transparent,
                label = "langBg",
            )
            Text(
                text = language.code.uppercase(),
                color = Color.White,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bg)
                    .clickable(enabled = !selected) { change(language) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
