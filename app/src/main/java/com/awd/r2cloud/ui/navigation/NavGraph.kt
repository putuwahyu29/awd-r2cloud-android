package com.awd.r2cloud.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.awd.r2cloud.R
import com.awd.r2cloud.data.worker.BackupManager
import com.awd.r2cloud.domain.repository.R2Repository
import com.awd.r2cloud.ui.screen.dashboard.DashboardScreen
import com.awd.r2cloud.ui.screen.filebrowser.FileBrowserScreen
import com.awd.r2cloud.ui.screen.login.LoginScreen
import com.awd.r2cloud.ui.screen.settings.BackupSettingsScreen
import com.awd.r2cloud.ui.screen.settings.SettingsScreen
import com.awd.r2cloud.ui.screen.starred.StarredScreen
import com.awd.r2cloud.ui.screen.transfer.TransferScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Buckets : Screen("buckets", R.string.buckets, Icons.Default.Storage)
    object Starred : Screen("starred", R.string.starred, Icons.Default.Star)
    object Transfers : Screen("transfers", R.string.transfers, Icons.Default.History)
    object Settings : Screen("settings", R.string.settings, Icons.Default.Settings)
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    repository: R2Repository,
    backupManager: BackupManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val showBottomBar = currentDestination?.route in listOf(
        Screen.Buckets.route, Screen.Starred.route, Screen.Transfers.route, Screen.Settings.route
    )

    // Double Tap to Exit logic
    var backPressedOnce by remember { mutableStateOf(false) }
    
    if (showBottomBar) {
        BackHandler {
            if (backPressedOnce) {
                (context as? Activity)?.finish()
            } else {
                backPressedOnce = true
                Toast.makeText(context, context.getString(R.string.press_back_again), Toast.LENGTH_SHORT).show()
                scope.launch {
                    delay(2000)
                    backPressedOnce = false
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                // NavigationBar with ZERO window insets to force background to bottom
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    windowInsets = WindowInsets(0, 0, 0, 0) 
                ) {
                    val items = listOf(Screen.Buckets, Screen.Starred, Screen.Transfers, Screen.Settings)
                    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(stringResource(screen.labelRes)) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            // Add manual padding to icons/labels to respect system bars while keeping background full
                            modifier = Modifier.padding(bottom = (navBarPadding / 2).coerceAtLeast(0.dp))
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Buckets.route) {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = if (navController.previousBackStackEntry != null) {
                        { navController.popBackStack() }
                    } else null
                )
            }
            composable(Screen.Buckets.route) {
                DashboardScreen(
                    onBucketClick = { bucketName ->
                        navController.navigate("file_browser/$bucketName")
                    }
                )
            }
            composable(Screen.Starred.route) {
                StarredScreen()
            }
            composable(Screen.Transfers.route) {
                TransferScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onAddAccount = {
                        navController.navigate("login")
                    },
                    onLogoutAll = {
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    },
                    onViewBackup = {
                        navController.navigate("backup_settings")
                    },
                    repository = repository
                )
            }
            composable("backup_settings") {
                BackupSettingsScreen(
                    onBack = { navController.popBackStack() },
                    backupManager = backupManager
                )
            }
            composable(
                route = "file_browser/{bucketName}",
                arguments = listOf(navArgument("bucketName") { type = NavType.StringType })
            ) { backStackEntry ->
                val bucketName = backStackEntry.arguments?.getString("bucketName") ?: ""
                FileBrowserScreen(
                    bucketName = bucketName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
