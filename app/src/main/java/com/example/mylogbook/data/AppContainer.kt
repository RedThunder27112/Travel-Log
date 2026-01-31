package com.example.mylogbook.data

import android.content.Context

class AppContainer(context: Context) {
    private val database = LogbookDatabase.getDatabase(context)
    val repository = LogbookRepository(database.logbookDao())
    val settingsRepository = SettingsRepository(context)
}
