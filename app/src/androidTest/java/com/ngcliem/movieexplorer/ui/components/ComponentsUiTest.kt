package com.ngcliem.movieexplorer.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.ui.theme.MovieExplorerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI tests for shared components. Run on a device or emulator. */
@RunWith(AndroidJUnit4::class)
class ComponentsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun errorState_showsMessage_andRetryCallsCallback() {
        var retried = false
        composeRule.setContent {
            MovieExplorerTheme(dynamicColor = false) {
                ErrorState(message = "No internet connection.", onRetry = { retried = true })
            }
        }

        composeRule.onNodeWithText("No internet connection.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertTrue(retried)
    }

    @Test
    fun movieCard_showsTitleRatingAndYear_andHandlesClick() {
        var clickedId = -1
        val movie = Movie(
            id = 42,
            title = "Interstellar",
            overview = "",
            posterUrl = null,
            backdropUrl = null,
            releaseDate = "2014-11-07",
            voteAverage = 8.4,
        )
        composeRule.setContent {
            MovieExplorerTheme(dynamicColor = false) {
                MovieCard(movie = movie, onClick = { clickedId = movie.id })
            }
        }

        composeRule.onNodeWithText("Interstellar").assertIsDisplayed()
        composeRule.onNodeWithText("8.4").assertIsDisplayed()
        composeRule.onNodeWithText("• 2014").assertIsDisplayed()
        composeRule.onNodeWithText("Interstellar").performClick()

        assertEquals(42, clickedId)
    }
}
