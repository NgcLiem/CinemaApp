package com.ngcliem.movieexplorer.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ngcliem.movieexplorer.R
import kotlinx.serialization.Serializable

// Type-safe navigation routes (Navigation Compose 2.8+).
// Arguments are normal properties, so there are no route strings to mistype.

@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

@Serializable
data object HomeRoute

@Serializable
data object SearchRoute

@Serializable
data object FavoritesRoute

@Serializable
data object ProfileRoute

@Serializable
data class DetailRoute(val movieId: Int)

/** "See all" list of a home section; [category] is a MovieCategory name. */
@Serializable
data class MovieListRoute(val category: String)

@Serializable
data class BookingRoute(val movieId: Int)

@Serializable
data class TicketsRoute(val specialOnly: Boolean = false)

@Serializable
data object MembershipRoute

@Serializable
data object ChangePasswordRoute

/** The tabs in the bottom navigation bar, each with its own accent gradient. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val colors: List<Color>,
) {
    HOME(
        HomeRoute, R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home,
        listOf(Color(0xFFFF512F), Color(0xFFE50914)),
    ),
    SEARCH(
        SearchRoute, R.string.tab_search, Icons.Filled.Search, Icons.Outlined.Search,
        listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
    ),
    FAVORITES(
        FavoritesRoute, R.string.tab_favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder,
        listOf(Color(0xFFFF6FD8), Color(0xFFE91E63)),
    ),
    PROFILE(
        ProfileRoute, R.string.tab_account, Icons.Filled.Person, Icons.Outlined.Person,
        listOf(Color(0xFFB06AB3), Color(0xFF6A3DE8)),
    ),
}
