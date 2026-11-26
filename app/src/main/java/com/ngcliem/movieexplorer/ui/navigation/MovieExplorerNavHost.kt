package com.ngcliem.movieexplorer.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ngcliem.movieexplorer.domain.model.MovieCategory
import com.ngcliem.movieexplorer.ui.auth.LoginScreen
import com.ngcliem.movieexplorer.ui.auth.RegisterScreen
import com.ngcliem.movieexplorer.ui.booking.BookingScreen
import com.ngcliem.movieexplorer.ui.detail.DetailScreen
import com.ngcliem.movieexplorer.ui.favorites.FavoritesScreen
import com.ngcliem.movieexplorer.ui.home.HomeScreen
import com.ngcliem.movieexplorer.ui.membership.MembershipScreen
import com.ngcliem.movieexplorer.ui.movielist.MovieListScreen
import com.ngcliem.movieexplorer.ui.profile.ChangePasswordScreen
import com.ngcliem.movieexplorer.ui.profile.ProfileScreen
import com.ngcliem.movieexplorer.ui.search.SearchScreen
import com.ngcliem.movieexplorer.ui.tickets.TicketsScreen

@Composable
fun MovieExplorerNavHost(
    isLoggedIn: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selectedTab = TopLevelDestination.entries.firstOrNull { currentDestination.isOnTab(it) }
    val showBottomBar = selectedTab != null

    Scaffold(
        // Each screen handles the status bar itself with its own TopAppBar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                ColorfulBottomBar(
                    selected = selectedTab,
                    onTabSelected = { navController.navigateToTab(it) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) HomeRoute else LoginRoute,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            enterTransition = { slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(300)) },
        ) {
            val openDetail: (Int) -> Unit = { id -> navController.navigate(DetailRoute(id)) }
            val openBooking: (Int) -> Unit = { id -> navController.navigate(BookingRoute(id)) }
            val goHome: () -> Unit = {
                navController.navigate(HomeRoute) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
            val goLogin: () -> Unit = {
                navController.navigate(LoginRoute) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }

            composable<LoginRoute> {
                LoginScreen(
                    onLoggedIn = goHome,
                    onRegisterClick = { navController.navigate(RegisterRoute) },
                )
            }
            composable<RegisterRoute> {
                RegisterScreen(
                    onRegistered = goHome,
                    onLoginClick = { navController.navigateUp() },
                )
            }

            composable<HomeRoute> {
                HomeScreen(
                    onMovieClick = openDetail,
                    onBookClick = openBooking,
                    onSeeAll = { category: MovieCategory -> navController.navigate(MovieListRoute(category.name)) },
                )
            }
            composable<SearchRoute> { SearchScreen(onMovieClick = openDetail) }
            composable<FavoritesRoute> { FavoritesScreen(onMovieClick = openDetail) }
            composable<ProfileRoute> {
                ProfileScreen(
                    onMyTickets = { navController.navigate(TicketsRoute(specialOnly = false)) },
                    onSpecialTickets = { navController.navigate(TicketsRoute(specialOnly = true)) },
                    onMembership = { navController.navigate(MembershipRoute) },
                    onChangePassword = { navController.navigate(ChangePasswordRoute) },
                    onLoggedOut = goLogin,
                )
            }

            composable<DetailRoute> {
                DetailScreen(
                    onBack = { navController.navigateUp() },
                    onBookClick = openBooking,
                    onLoginRequired = goLogin,
                )
            }
            composable<MovieListRoute> {
                MovieListScreen(onMovieClick = openDetail, onBack = { navController.navigateUp() })
            }
            composable<BookingRoute> {
                BookingScreen(
                    onBack = { navController.navigateUp() },
                    onViewTickets = {
                        navController.navigate(TicketsRoute()) {
                            popUpTo<BookingRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<TicketsRoute> { entry ->
                TicketsScreen(
                    specialOnly = entry.toRoute<TicketsRoute>().specialOnly,
                    onBack = { navController.navigateUp() },
                    onMovieClick = openDetail,
                )
            }
            composable<MembershipRoute> { MembershipScreen(onBack = { navController.navigateUp() }) }
            composable<ChangePasswordRoute> { ChangePasswordScreen(onBack = { navController.navigateUp() }) }
        }
    }
}

private fun NavDestination?.isOnTab(tab: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.route::class) } == true

/**
 * Standard bottom-navigation behaviour: one copy of each tab on the back stack,
 * and each tab keeps its scroll position when you come back to it.
 */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        // Home is the root of the signed-in part of the app (the graph's start
        // destination may be the login screen, which is no longer on the stack).
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
