package com.ngcliem.movieexplorer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ngcliem.movieexplorer.data.settings.LanguageStore
import com.ngcliem.movieexplorer.domain.repository.AuthRepository
import com.ngcliem.movieexplorer.ui.navigation.MovieExplorerNavHost
import com.ngcliem.movieexplorer.ui.theme.MovieExplorerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    /** Applies the language chosen in the app (Vietnamese by default) to all string resources. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Read once: the session is stored synchronously in SharedPreferences.
        val isLoggedIn = authRepository.isLoggedIn()
        setContent {
            MovieExplorerTheme {
                MovieExplorerNavHost(isLoggedIn = isLoggedIn)
            }
        }
    }
}
