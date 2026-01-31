package com.example.mylogbook.ui

import androidx.compose.foundation.layout.padding
import android.content.Intent
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mylogbook.LogbookApp as LogbookApplication
import androidx.navigation.compose.rememberNavController
import com.example.mylogbook.ui.screens.LoginScreen
import com.example.mylogbook.ui.navigation.LogbookNavGraph
import com.example.mylogbook.ui.navigation.LogbookTopBar
import com.example.mylogbook.ui.theme.LogbookTheme
import com.example.mylogbook.viewmodel.AppViewModelProvider
import com.example.mylogbook.viewmodel.ImportExportEvent
import com.example.mylogbook.viewmodel.ImportExportViewModel
import com.example.mylogbook.viewmodel.LoginViewModel
import com.example.mylogbook.viewmodel.SettingsViewModel

@Composable
fun LogbookApp() {
    val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val loginViewModel: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val loginState by loginViewModel.state.collectAsStateWithLifecycle()
    val importExportViewModel: ImportExportViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val context = LocalContext.current
    val app = context.applicationContext as LogbookApplication
    val sharedImportUri by app.sharedImportUri.collectAsStateWithLifecycle()

    val useDarkTheme = if (settings.useSystemTheme) {
        androidx.compose.foundation.isSystemInDarkTheme()
    } else {
        settings.darkTheme
    }

    LogbookTheme(darkTheme = useDarkTheme) {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            importExportViewModel.events.collect { event ->
                when (event) {
                    is ImportExportEvent.Message -> snackbarHostState.showSnackbar(event.text)
                    is ImportExportEvent.ShareCsv -> {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, event.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(Intent.createChooser(shareIntent, "Share CSV"))
                            } catch (e: android.content.ActivityNotFoundException) {
                                val fallback = Intent(Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_STREAM, event.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(fallback, "Share CSV"))
                            }
                        } catch (t: Throwable) {
                            snackbarHostState.showSnackbar("Share failed: ${t.message ?: t::class.java.simpleName}")
                        }
                    }
                    is ImportExportEvent.ShareExcel -> {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.ms-excel"
                                putExtra(Intent.EXTRA_STREAM, event.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(Intent.createChooser(shareIntent, "Share Excel"))
                            } catch (e: android.content.ActivityNotFoundException) {
                                val fallback = Intent(Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_STREAM, event.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(fallback, "Share Excel"))
                            }
                        } catch (t: Throwable) {
                            snackbarHostState.showSnackbar("Share failed: ${t.message ?: t::class.java.simpleName}")
                        }
                    }
                }
            }
        }
        if (!loginState.isLoggedIn) {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { padding ->
                androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
                    LoginScreen(viewModel = loginViewModel, snackbarHostState = snackbarHostState)
                }
            }
        } else {
            LaunchedEffect(sharedImportUri, loginState.isLoggedIn) {
                val uri = sharedImportUri ?: return@LaunchedEffect
                if (loginState.isLoggedIn) {
                    // Clear first to avoid repeated navigation/import loops.
                    app.clearSharedImport()
                    importExportViewModel.importShared(context.contentResolver, uri)
                    navController.navigate(com.example.mylogbook.ui.navigation.NavItem.ImportExport.route) {
                        launchSingleTop = true
                    }
                }
            }
            Scaffold(
                topBar = { LogbookTopBar(navController) },
                bottomBar = { com.example.mylogbook.ui.navigation.LogbookBottomBar(navController) },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { padding ->
                LogbookNavGraph(
                    navController = navController,
                    snackbarHostState = snackbarHostState,
                    importExportViewModel = importExportViewModel,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}
