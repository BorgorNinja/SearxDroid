package dev.searxdroid.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.searxdroid.app.ui.home.HomeScreen
import dev.searxdroid.app.ui.search.SearchResultsScreen
import dev.searxdroid.app.ui.search.SearchViewModel
import dev.searxdroid.app.ui.settings.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val HOME    = "home"
    const val RESULTS = "results/{query}"
    const val SETTINGS = "settings"

    fun results(query: String): String {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        return "results/$encoded"
    }
}

@Composable
fun SearxNavHost(initialQuery: String = "") {
    val navController = rememberNavController()
    val searchViewModel: SearchViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                initialQuery    = initialQuery,
                onSearch        = { q ->
                    searchViewModel.search(q)
                    navController.navigate(Routes.results(q))
                },
                onOpenSettings  = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.RESULTS,
            arguments = listOf(navArgument("query") { type = NavType.StringType }),
        ) { backStack ->
            val raw = backStack.arguments?.getString("query") ?: ""
            val query = URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
            SearchResultsScreen(
                viewModel  = searchViewModel,
                initialQuery = query,
                onBack     = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
