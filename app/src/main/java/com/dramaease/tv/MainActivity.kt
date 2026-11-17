package com.dramaease.tv

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dramaease.tv.data.AppSettings
import com.dramaease.tv.player.PlayerScreen
import com.dramaease.tv.ui.DramaViewModel
import com.dramaease.tv.ui.HomeViewModel
import com.dramaease.tv.ui.PlayerViewModel
import com.dramaease.tv.ui.SearchViewModel
import com.dramaease.tv.ui.VmFactory
import com.dramaease.tv.ui.screens.DramaDetailsScreen
import com.dramaease.tv.ui.screens.EpisodesScreen
import com.dramaease.tv.ui.screens.HomeScreen
import com.dramaease.tv.ui.screens.SearchScreen
import com.dramaease.tv.ui.screens.SettingsScreen
import com.dramaease.tv.ui.theme.DramaEaseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as DramaEaseApp).container
        setContent {
            val settings by container.settings.settings.collectAsState(initial = null)
            val current = settings
            if (current == null) {
                // Wait for saved settings (milliseconds) so the first frame never flashes the wrong text size.
                Box(Modifier.fillMaxSize().background(Color.Black))
            } else {
                DramaEaseTheme(current) { AppNav(container, current) }
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val DETAILS = "details/{dramaId}"
    const val EPISODES = "episodes/{dramaId}"
    const val PLAYER = "player/{dramaId}/{episodeId}"
    fun details(id: String) = "details/${Uri.encode(id)}"
    fun episodes(id: String) = "episodes/${Uri.encode(id)}"
    fun player(d: String, e: String) = "player/${Uri.encode(d)}/${Uri.encode(e)}"
}

@Composable
private fun AppNav(container: AppContainer, settings: AppSettings) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val dramaArg = listOf(navArgument("dramaId") { type = NavType.StringType })

    // No transition animations: calmer on TV hardware and for the viewer.
    NavHost(
        navController = nav, startDestination = Routes.HOME,
        enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None },
    ) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = VmFactory(container))
            HomeScreen(
                vm, settings,
                onOpenDrama = { nav.navigate(Routes.details(it)) },
                onPlay = { d, e -> nav.navigate(Routes.player(d, e)) },
                onSearch = { nav.navigate(Routes.SEARCH) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SEARCH) {
            val vm: SearchViewModel = viewModel(factory = VmFactory(container))
            SearchScreen(vm, onOpenDrama = { nav.navigate(Routes.details(it)) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings,
                onChange = { t -> scope.launch { container.settings.update(t) } },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.DETAILS, dramaArg) { entry ->
            val id = entry.arguments?.getString("dramaId").orEmpty()
            val vm: DramaViewModel = viewModel(factory = VmFactory(container, listOf(id)))
            DramaDetailsScreen(
                vm,
                onPlay = { nav.navigate(Routes.player(id, it)) },
                onChooseEpisode = { nav.navigate(Routes.episodes(id)) },
            )
        }
        composable(Routes.EPISODES, dramaArg) { entry ->
            val id = entry.arguments?.getString("dramaId").orEmpty()
            val vm: DramaViewModel = viewModel(factory = VmFactory(container, listOf(id)))
            EpisodesScreen(vm, onPlay = { nav.navigate(Routes.player(id, it)) })
        }
        composable(
            Routes.PLAYER,
            listOf(navArgument("dramaId") { type = NavType.StringType }, navArgument("episodeId") { type = NavType.StringType }),
        ) { entry ->
            val d = entry.arguments?.getString("dramaId").orEmpty()
            val e = entry.arguments?.getString("episodeId").orEmpty()
            val vm: PlayerViewModel = viewModel(factory = VmFactory(container, listOf(d, e)))
            PlayerScreen(
                vm, settings,
                onBack = { nav.popBackStack() },
                // Replace the current player entry so Back goes to the episode list, not through every episode watched.
                onPlayEpisode = { next ->
                    nav.navigate(Routes.player(d, next)) { popUpTo(Routes.PLAYER) { inclusive = true } }
                },
            )
        }
    }
}
