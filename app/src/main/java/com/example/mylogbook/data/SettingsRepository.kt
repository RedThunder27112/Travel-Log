package com.example.mylogbook.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Settings(
    val useSystemTheme: Boolean = true,
    val darkTheme: Boolean = false,
    val defaultEmail: String = "",
    val emailPassword: String = "",
    val pin: String = "1234",
    val lastFromLocation: String = "",
    val lastToLocation: String = "",
    val lastExportUri: String = ""
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val useSystemTheme = booleanPreferencesKey("use_system_theme")
        val darkTheme = booleanPreferencesKey("dark_theme")
        val defaultEmail = stringPreferencesKey("default_email")
        val emailPassword = stringPreferencesKey("email_password")
        val pin = stringPreferencesKey("pin")
        val lastFromLocation = stringPreferencesKey("last_from_location")
        val lastToLocation = stringPreferencesKey("last_to_location")
        val lastExportUri = stringPreferencesKey("last_export_uri")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            useSystemTheme = prefs[Keys.useSystemTheme] ?: true,
            darkTheme = prefs[Keys.darkTheme] ?: false,
            defaultEmail = prefs[Keys.defaultEmail] ?: "",
            emailPassword = prefs[Keys.emailPassword] ?: "",
            pin = prefs[Keys.pin] ?: "1234",
            lastFromLocation = prefs[Keys.lastFromLocation] ?: "",
            lastToLocation = prefs[Keys.lastToLocation] ?: "",
            lastExportUri = prefs[Keys.lastExportUri] ?: ""
        )
    }

    suspend fun setUseSystemTheme(value: Boolean) {
        context.dataStore.edit { it[Keys.useSystemTheme] = value }
    }

    suspend fun setDarkTheme(value: Boolean) {
        context.dataStore.edit { it[Keys.darkTheme] = value }
    }

    suspend fun setDefaultEmail(value: String) {
        context.dataStore.edit { it[Keys.defaultEmail] = value }
    }

    suspend fun setEmailPassword(value: String) {
        context.dataStore.edit { it[Keys.emailPassword] = value }
    }

    suspend fun setPin(value: String) {
        context.dataStore.edit { it[Keys.pin] = value }
    }

    suspend fun setLastFromLocation(value: String) {
        context.dataStore.edit { it[Keys.lastFromLocation] = value }
    }

    suspend fun setLastToLocation(value: String) {
        context.dataStore.edit { it[Keys.lastToLocation] = value }
    }

    suspend fun setLastExportUri(value: String) {
        context.dataStore.edit { it[Keys.lastExportUri] = value }
    }
}
