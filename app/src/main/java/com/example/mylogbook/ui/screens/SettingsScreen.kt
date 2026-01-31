package com.example.mylogbook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mylogbook.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var newPin by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Use system theme")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.useSystemTheme,
                        onCheckedChange = settingsViewModel::setUseSystemTheme
                    )
                }
                if (!settings.useSystemTheme) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark mode")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.darkTheme,
                            onCheckedChange = settingsViewModel::setDarkTheme
                        )
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sharing", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = settings.defaultEmail,
                    onValueChange = settingsViewModel::setDefaultEmail,
                    label = { Text("Default export email address") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = settings.emailPassword,
                    onValueChange = settingsViewModel::setEmailPassword,
                    label = { Text("Email password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { value ->
                        if (value.length <= 8 && value.all { it.isDigit() }) {
                            newPin = value
                        }
                    },
                    label = { Text("New PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
                Button(onClick = {
                    val trimmed = newPin.trim()
                    if (trimmed.length < 4) {
                        scope.launch { snackbarHostState.showSnackbar("PIN must be at least 4 digits") }
                    } else {
                        settingsViewModel.setPin(trimmed)
                        newPin = ""
                        scope.launch { snackbarHostState.showSnackbar("PIN updated") }
                    }
                }) {
                    Text("Update PIN")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Data", style = MaterialTheme.typography.titleMedium)
                Button(onClick = {
                    settingsViewModel.addSampleEntries()
                    scope.launch { snackbarHostState.showSnackbar("Sample entries added") }
                }) {
                    Text("Add sample entries")
                }
            }
        }
    }
}
