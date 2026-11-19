package com.dhikri.islamic.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dhikri.islamic.ui.screens.*
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    data object Home : BottomTab("home", "الرئيسية", Icons.Default.Home)
    data object Quran : BottomTab("quran", "القرآن الكريم", Icons.Default.MenuBook)
    data object Adhkar : BottomTab("adhkar", "الأذكار", Icons.Default.VolunteerActivism)
    data object Stories : BottomTab("stories", "الحكايات", Icons.Default.AutoStories)
    data object More : BottomTab("more", "المزيد", Icons.Default.MoreHoriz)
}

object Routes {
    const val MUSHAF_PAGE = "mushaf/{page}"
    const val QURAN_SEARCH = "quran_search"
    const val ADHKAR_DETAIL = "adhkar_detail/{categoryId}"
    const val STORY_DETAIL = "story_detail/{storyId}"
    const val TASBIH = "tasbih"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
    const val SOURCES = "sources"
    const val FAVORITES = "favorites"

    fun mushafPage(page: Int) = "mushaf/${page.coerceIn(1, 604)}"
    fun adhkarDetail(categoryId: Int) = "adhkar_detail/$categoryId"
    fun storyDetail(storyId: Int) = "story_detail/$storyId"
}

@Composable
fun DhikriAppShell(viewModel: DhikriViewModel) {
    val navController = rememberNavController()
    val tabs = listOf(BottomTab.Home, BottomTab.Quran, BottomTab.Adhkar, BottomTab.Stories, BottomTab.More)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = tabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomTab.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onContinueReading = { page -> navController.navigate(Routes.mushafPage(page)) },
                    onOpenQuran = { navController.navigate(BottomTab.Quran.route) },
                    onOpenAdhkarCategory = { id -> navController.navigate(Routes.adhkarDetail(id)) },
                    onOpenTasbih = { navController.navigate(Routes.TASBIH) },
                    onOpenStories = { navController.navigate(BottomTab.Stories.route) }
                )
            }
            composable(BottomTab.Quran.route) {
                QuranIndexScreen(
                    viewModel = viewModel,
                    onOpenPage = { page -> navController.navigate(Routes.mushafPage(page)) },
                    onOpenSearch = { navController.navigate(Routes.QURAN_SEARCH) }
                )
            }
            composable(
                route = Routes.MUSHAF_PAGE,
                arguments = listOf(navArgument("page") { type = NavType.IntType })
            ) { entry ->
                val page = entry.arguments?.getInt("page") ?: 1
                MushafReaderScreen(
                    initialPage = page,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) }
                )
            }
            composable(Routes.QURAN_SEARCH) {
                QuranSearchScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenPage = { page -> navController.navigate(Routes.mushafPage(page)) }
                )
            }
            composable(BottomTab.Adhkar.route) {
                AdhkarListScreen(
                    viewModel = viewModel,
                    onOpenCategory = { id -> navController.navigate(Routes.adhkarDetail(id)) }
                )
            }
            composable(
                route = Routes.ADHKAR_DETAIL,
                arguments = listOf(navArgument("categoryId") { type = NavType.IntType })
            ) { entry ->
                val catId = entry.arguments?.getInt("categoryId") ?: 27
                AdhkarDetailScreen(
                    categoryId = catId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(BottomTab.Stories.route) {
                StoriesListScreen(
                    viewModel = viewModel,
                    onOpenStory = { id -> navController.navigate(Routes.storyDetail(id)) }
                )
            }
            composable(
                route = Routes.STORY_DETAIL,
                arguments = listOf(navArgument("storyId") { type = NavType.IntType })
            ) { entry ->
                val storyId = entry.arguments?.getInt("storyId") ?: 1
                StoryDetailScreen(
                    storyId = storyId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(BottomTab.More.route) {
                MoreScreen(
                    onOpenTasbih = { navController.navigate(Routes.TASBIH) },
                    onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenSources = { navController.navigate(Routes.SOURCES) }
                )
            }
            composable(Routes.TASBIH) {
                TasbihScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.DOWNLOADS) {
                DownloadCenterScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenPage = { page -> navController.navigate(Routes.mushafPage(page)) },
                    onOpenStory = { id -> navController.navigate(Routes.storyDetail(id)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onOpenSources = { navController.navigate(Routes.SOURCES) }
                )
            }
            composable(Routes.SOURCES) {
                SourcesAndLicensesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
