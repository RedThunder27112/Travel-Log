package com.example.mylogbook.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mylogbook.LogbookApp

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as LogbookApp
            EntriesViewModel(app.container.repository)
        }
        initializer {
            val app = this[APPLICATION_KEY] as LogbookApp
            SummaryViewModel(app.container.repository)
        }
        initializer {
            val app = this[APPLICATION_KEY] as LogbookApp
            ImportExportViewModel(app.container.repository, app.container.settingsRepository, app.applicationContext)
        }
        initializer {
            val app = this[APPLICATION_KEY] as LogbookApp
            SettingsViewModel(app.container.settingsRepository, app.container.repository)
        }
        initializer {
            val app = this[APPLICATION_KEY] as LogbookApp
            LoginViewModel(app.container.settingsRepository)
        }
        initializer {
            val app = this[APPLICATION_KEY] as LogbookApp
            val savedStateHandle: SavedStateHandle = createSavedStateHandle()
            EditEntryViewModel(
                app.container.repository,
                app.container.settingsRepository,
                savedStateHandle
            )
        }
    }
}
