package com.burton.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.burton.finance.report.ShakeToReport
import com.burton.finance.ui.detail.DetailScreen
import com.burton.finance.ui.feed.FeedScreen
import com.burton.finance.ui.markets.MarketsScreen
import com.burton.finance.ui.navigation.Routes
import com.burton.finance.ui.theme.BurtonBlack
import com.burton.finance.ui.theme.BurtonFinanceTheme
import com.burton.finance.ui.theme.BurtonIvory
import com.burton.finance.ui.theme.BurtonMute
import com.burton.finance.ui.watchlist.WatchlistScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val shakeToReport by lazy { ShakeToReport(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BurtonFinanceTheme {
                BurtonApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shakeToReport.start()
    }

    override fun onPause() {
        shakeToReport.stop()
        super.onPause()
    }
}

@Composable
private fun BurtonApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val onDetail = route?.startsWith("symbol/") == true
    val selectedTab = when (route) {
        Routes.MARKETS -> Routes.MARKETS
        Routes.FEED -> Routes.FEED
        else -> Routes.WATCHLIST
    }
    Scaffold(
        containerColor = BurtonBlack,
        bottomBar = {
            if (!onDetail) {
                NavigationBar(containerColor = BurtonBlack, contentColor = BurtonIvory) {
                    NavigationBarItem(
                        selected = selectedTab == Routes.WATCHLIST,
                        onClick = { navController.goTab(Routes.WATCHLIST) },
                        icon = { Icon(Icons.AutoMirrored.Rounded.ShowChart, contentDescription = "Watchlist") },
                        label = { Text("Watchlist") },
                        colors = navColors(selectedTab == Routes.WATCHLIST),
                    )
                    NavigationBarItem(
                        selected = selectedTab == Routes.MARKETS,
                        onClick = { navController.goTab(Routes.MARKETS) },
                        icon = { Icon(Icons.Rounded.Public, contentDescription = "Markets") },
                        label = { Text("Markets") },
                        colors = navColors(selectedTab == Routes.MARKETS),
                    )
                    NavigationBarItem(
                        selected = selectedTab == Routes.FEED,
                        onClick = { navController.goTab(Routes.FEED) },
                        icon = { Icon(Icons.Rounded.Newspaper, contentDescription = "Feed") },
                        label = { Text("Feed") },
                        colors = navColors(selectedTab == Routes.FEED),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.WATCHLIST,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.WATCHLIST) {
                WatchlistScreen(
                    onOpenSymbol = { navController.navigate(Routes.symbol(it)) },
                    onOpenMarkets = { navController.goTab(Routes.MARKETS) },
                )
            }
            composable(Routes.MARKETS) {
                MarketsScreen(
                    onOpenSymbol = { navController.navigate(Routes.symbol(it)) },
                )
            }
            composable(Routes.FEED) {
                FeedScreen()
            }
            composable(
                Routes.SYMBOL,
                arguments = listOf(navArgument("symbolId") { type = NavType.StringType }),
            ) {
                DetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun navColors(selected: Boolean) = NavigationBarItemDefaults.colors(
    selectedIconColor = BurtonIvory,
    selectedTextColor = BurtonIvory,
    unselectedIconColor = BurtonMute,
    unselectedTextColor = BurtonMute,
    indicatorColor = Color(0xFF222222),
)
