package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.ActivityRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.TodayRoute
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.DayPulseTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DayPulseTheme {
                DayPulseRoot(viewModel = viewModel)
            }
        }
    }
}

private data class NavItem(
    val label: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

@Composable
private fun DayPulseRoot(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onAppForegrounded()
                Lifecycle.Event.ON_PAUSE -> viewModel.onAppBackgrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navItems = remember {
        listOf(
            NavItem(
                label = "Today",
                route = TodayRoute,
                selectedIcon = Icons.Rounded.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                testTag = "nav_today",
            ),
            NavItem(
                label = "Activity",
                route = ActivityRoute,
                selectedIcon = Icons.Rounded.ListAlt,
                unselectedIcon = Icons.Outlined.ListAlt,
                testTag = "nav_activity",
            ),
            NavItem(
                label = "Settings",
                route = SettingsRoute,
                selectedIcon = Icons.Rounded.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_settings",
            ),
        )
    }

    val navigateTo: (Any) -> Unit = { destination ->
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = TodayRoute,
                    modifier = Modifier.weight(1f),
                ) {
                    composable<TodayRoute> {
                        TodayScreen(
                            uiState = uiState,
                            onRefresh = { viewModel.refreshAll() },
                            onOpenSettings = { navigateTo(SettingsRoute) },
                            onPinWidget = {
                                val requested = viewModel.requestPinWidget()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (requested) {
                                            "Confirm adding DayPulse widget on your launcher."
                                        } else {
                                            "Long-press your home screen and pick DayPulse under Widgets."
                                        },
                                    )
                                }
                            },
                            onRefreshWidget = {
                                viewModel.refreshAll()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Widget synced")
                                }
                            },
                        )
                    }

                    composable<ActivityRoute> {
                        ActivityScreen(
                            uiState = uiState,
                            onSelectTab = viewModel::selectActivityTab,
                            onSearchChanged = viewModel::updateSearchQuery,
                        )
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(
                            uiState = uiState,
                            onSaveConnection = { url, token ->
                                viewModel.saveConnectionSettings(url, token)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Server configuration saved")
                                }
                            },
                            onTestConnection = viewModel::testConnection,
                            onSetRequireVpn = viewModel::setRequireVpn,
                            onSetLiveSyncEnabled = viewModel::setLiveSyncEnabled,
                            onSetSyncIntervalSeconds = viewModel::setSyncIntervalSeconds,
                            onSetWidgetTopCount = viewModel::setWidgetTopCount,
                        )
                    }
                }
            }
        }
    }
}
