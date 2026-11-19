package com.dhikri.islamic.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dhikri.islamic.ui.components.WhatsNewDialog
import com.dhikri.islamic.ui.components.WhatsNewTracker
import com.dhikri.islamic.ui.screens.*
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    data object Home : BottomTab("home", "الرئيسية", Icons.Default.Home)
    data object Quran : BottomTab("quran", "القرآن", Icons.Default.MenuBook)
    data object Adhkar : BottomTab("adhkar", "الأذكار", Icons.Default.VolunteerActivism)
    data object Qibla : BottomTab("qibla_tab", "القبلة", Icons.Default.Explore)
}

object Routes {
    const val MUSHAF_PAGE = "mushaf/{page}"
    const val QURAN_SEARCH = "quran_search"
    const val ADHKAR_DETAIL = "adhkar_detail/{categoryId}/{mode}"
    const val MORE = "more"
    const val TASBIH = "tasbih"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
    const val SOURCES = "sources"
    const val FAVORITES = "favorites"
    const val KHATMAH = "khatmah"
    const val RECITATIONS = "recitations"
    const val RUQYAH = "ruqyah"
    const val ASMA_UL_HUSNA = "asma_ul_husna"
    const val QIBLA = "qibla"
    const val HIJRI = "hijri"

    fun mushafPage(page: Int) = "mushaf/${page.coerceIn(1, 604)}"
    fun adhkarDetail(categoryId: Int, mode: String = "all") = "adhkar_detail/$categoryId/$mode"
}

@Composable
fun DhikriAppShell(viewModel: DhikriViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showWhatsNew by remember { mutableStateOf(WhatsNewTracker.shouldShowForCurrentVersion(context)) }
    val tabs = listOf(BottomTab.Home, BottomTab.Quran, BottomTab.Adhkar, BottomTab.Qibla)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val updateProgress by viewModel.updateProgress.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()

    val showBottomBar = tabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    val isOnSecondaryTab = currentDestination?.route in listOf(
        BottomTab.Quran.route,
        BottomTab.Adhkar.route,
        BottomTab.Qibla.route
    )

    fun navigateToTab(tab: BottomTab) {
        if (tab == BottomTab.Home) {
            val popped = navController.popBackStack(BottomTab.Home.route, inclusive = false)
            if (!popped) {
                navController.navigate(BottomTab.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else {
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    BackHandler(enabled = isOnSecondaryTab) {
        navigateToTab(BottomTab.Home)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                DhikriElegantBottomBar(
                    tabs = tabs,
                    currentRoute = currentDestination?.route,
                    onSelect = { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        if (tab == BottomTab.Home || !selected) {
                            navigateToTab(tab)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = BottomTab.Home.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(animationSpec = tween(160)) },
                exitTransition = { fadeOut(animationSpec = tween(140)) },
                popEnterTransition = { fadeIn(animationSpec = tween(160)) },
                popExitTransition = { fadeOut(animationSpec = tween(140)) }
            ) {
                composable(BottomTab.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onContinueReading = { page -> navController.navigate(Routes.mushafPage(page)) },
                        onOpenQuran = { navigateToTab(BottomTab.Quran) },
                        onOpenAdhkarTab = { navigateToTab(BottomTab.Adhkar) },
                        onOpenAdhkarCategory = { id -> navController.navigate(Routes.adhkarDetail(id, "morning")) },
                        onOpenTasbih = { navController.navigate(Routes.TASBIH) },
                        onOpenQibla = { navigateToTab(BottomTab.Qibla) },
                        onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                        onOpenMore = { navController.navigate(Routes.MORE) },
                        onOpenHijri = { navController.navigate(Routes.HIJRI) }
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
                        onOpenCategory = { id, mode -> navController.navigate(Routes.adhkarDetail(id, mode)) }
                    )
                }
                composable(
                    route = Routes.ADHKAR_DETAIL,
                    arguments = listOf(
                        navArgument("categoryId") { type = NavType.IntType },
                        navArgument("mode") { type = NavType.StringType }
                    )
                ) { entry ->
                    val catId = entry.arguments?.getInt("categoryId") ?: 27
                    val mode = entry.arguments?.getString("mode") ?: "all"
                    AdhkarDetailScreen(
                        categoryId = catId,
                        mode = mode,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(BottomTab.Qibla.route) {
                    QiblaScreen(onBack = null)
                }
                composable(Routes.MORE) {
                    MoreScreen(
                        onOpenKhatmah = { navController.navigate(Routes.KHATMAH) },
                        onOpenRecitations = { navController.navigate(Routes.RECITATIONS) },
                        onOpenRuqyah = { navController.navigate(Routes.RUQYAH) },
                        onOpenAsmaUlHusna = { navController.navigate(Routes.ASMA_UL_HUSNA) },
                        onOpenTasbih = { navController.navigate(Routes.TASBIH) },
                        onOpenHisnAlMuslim = { navigateToTab(BottomTab.Adhkar) },
                        onOpenQibla = { navController.navigate(Routes.QIBLA) },
                        onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                        onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenSources = { navController.navigate(Routes.SOURCES) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.KHATMAH) {
                    KhatmahScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenPage = { page -> navController.navigate(Routes.mushafPage(page)) }
                    )
                }
                composable(Routes.RECITATIONS) {
                    AudioRecitationsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable(Routes.RUQYAH) {
                    RuqyahScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.ASMA_UL_HUSNA) {
                    AsmaUlHusnaScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.QIBLA) {
                    QiblaScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.HIJRI) {
                    HijriCalendarScreen(onBack = { navController.popBackStack() })
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
                        onOpenPage = { page -> navController.navigate(Routes.mushafPage(page)) }
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

            if (showWhatsNew) {
                WhatsNewDialog(
                    onDismiss = {
                        WhatsNewTracker.markCurrentVersionSeen(context)
                        showWhatsNew = false
                    }
                )
            }

            // Floating Auto-Update Download Overlay (visible on any screen when updating)
            updateProgress?.let { pct ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = EmeraldDark.copy(alpha = 0.94f),
                    border = BorderStroke(1.2.dp, SoftGold),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = SoftGold)
                            Text(
                                text = "جاري تنزيل تحديث ذِكري (${availableUpdate?.latestVersion ?: ""})... $pct%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = SoftGold,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DhikriElegantBottomBar(
    tabs: List<BottomTab>,
    currentRoute: String?,
    onSelect: (BottomTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF163A69).copy(alpha = 0.92f), EmeraldDark.copy(alpha = 0.95f))
                    )
                )
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                SoftGold.copy(alpha = 0.25f),
                                SoftGold,
                                SoftGold.copy(alpha = 0.25f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = currentRoute == tab.route
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(tab) }
                            .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) SoftGold else Color.White.copy(alpha = 0.78f),
                            modifier = Modifier.size(if (selected) 25.dp else 22.dp)
                        )
                        Text(
                            text = tab.label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) SoftGold else Color.White.copy(alpha = 0.80f),
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .width(if (selected) 26.dp else 0.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(SoftGold)
                        )
                    }
                    if (index < tabs.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(Color.White.copy(alpha = 0.16f))
                        )
                    }
                }
            }
        }
    }
}
