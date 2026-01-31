@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.mylogbook.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mylogbook.ui.screens.EditEntryScreen
import com.example.mylogbook.ui.screens.EntriesScreen
import com.example.mylogbook.ui.screens.ImportExportScreen
import com.example.mylogbook.ui.screens.SettingsScreen
import com.example.mylogbook.ui.screens.SummaryScreen
import com.example.mylogbook.viewmodel.AppViewModelProvider

const val EntryIdArg = "entryId"

sealed class NavItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Entries : NavItem("entries", "Entries", Icons.Default.Home)
    object Summary : NavItem("summary", "Summary", Icons.Default.Assessment)
    object ImportExport : NavItem("import_export", "Import/Export", Icons.Default.SwapHoriz)
    object Settings : NavItem("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    NavItem.Entries,
    NavItem.Summary,
    NavItem.ImportExport,
    NavItem.Settings
)

@Composable
fun LogbookNavGraph(
    navController: NavHostController,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    importExportViewModel: com.example.mylogbook.viewmodel.ImportExportViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavItem.Entries.route,
        modifier = modifier
    ) {
        composable(NavItem.Entries.route) {
            val viewModel = viewModel<com.example.mylogbook.viewmodel.EntriesViewModel>(factory = AppViewModelProvider.Factory)
            EntriesScreen(
                uiState = viewModel.uiState,
                onSearchChange = viewModel::updateSearch,
                onReasonFilterChange = viewModel::updateReasonFilter,
                onSortChange = viewModel::updateSortOption,
                onEntryClick = { id -> navController.navigate("edit?entryId=$id") },
                onAddEntry = { navController.navigate("edit") }
            )
        }
        composable(NavItem.Summary.route) {
            val summaryViewModel = viewModel<com.example.mylogbook.viewmodel.SummaryViewModel>(factory = AppViewModelProvider.Factory)
            val context = LocalContext.current
            SummaryScreen(
                uiState = summaryViewModel.uiState,
                onDateFromChange = summaryViewModel::updateDateFrom,
                onDateToChange = summaryViewModel::updateDateTo,
                onReasonFilterChange = summaryViewModel::updateReasonFilter,
                onExportSummary = { uri, entries ->
                    importExportViewModel.exportSummaryCsv(context.contentResolver, uri, entries)
                }
            )
        }
        composable(NavItem.ImportExport.route) {
            ImportExportScreen(importExportViewModel = importExportViewModel, snackbarHostState = snackbarHostState)
        }
        composable(NavItem.Settings.route) {
            val settingsViewModel = viewModel<com.example.mylogbook.viewmodel.SettingsViewModel>(factory = AppViewModelProvider.Factory)
            SettingsScreen(settingsViewModel = settingsViewModel, snackbarHostState = snackbarHostState)
        }
        composable(
            route = "edit?entryId={$EntryIdArg}",
            arguments = listOf(navArgument(EntryIdArg) {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            val editViewModel = viewModel<com.example.mylogbook.viewmodel.EditEntryViewModel>(factory = AppViewModelProvider.Factory)
            EditEntryScreen(
                viewModel = editViewModel,
                onDone = { navController.popBackStack() },
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
fun LogbookBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route?.substringBefore("?")
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    if (!showBottomBar) return

    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) }
            )
        }
    }
}

@Composable
fun LogbookTopBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("?")
    val entryId = navBackStackEntry?.arguments?.getLong(EntryIdArg) ?: -1L
    val title = when (currentRoute) {
        NavItem.Entries.route -> "My LogBook Mom"
        NavItem.Summary.route -> "Summary"
        NavItem.ImportExport.route -> "Import/Export"
        NavItem.Settings.route -> "Settings"
        "edit" -> if (entryId > 0L) "Edit Entry" else "Add Entry"
        else -> "My LogBook Mom"
    }

    val showBack = currentRoute == "edit"

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        }
    )
}
