package com.example.mylogbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylogbook.data.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed class LoginEvent {
    data class Error(val message: String) : LoginEvent()
    data class ResetSuccess(val email: String, val newPin: String) : LoginEvent()
    object LoggedIn : LoginEvent()
}

data class LoginUiState(
    val pinInput: String = "",
    val emailInput: String = "",
    val isLoggedIn: Boolean = false
)

class LoginViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    private val _events = MutableSharedFlow<LoginEvent>()
    val events = _events.asSharedFlow()

    fun updatePinInput(value: String) {
        _state.update { it.copy(pinInput = value) }
    }

    fun updateEmailInput(value: String) {
        _state.update { it.copy(emailInput = value) }
    }

    fun attemptLogin() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val input = state.value.pinInput.trim()
            if (input == settings.pin) {
                _state.update { it.copy(isLoggedIn = true, pinInput = "") }
                _events.emit(LoginEvent.LoggedIn)
            } else {
                _events.emit(LoginEvent.Error("Incorrect PIN"))
            }
        }
    }

    fun resetPin() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val email = state.value.emailInput.trim()
            if (email.isBlank() || settings.defaultEmail.isBlank()) {
                _events.emit(LoginEvent.Error("Please set your default email in Settings first."))
                return@launch
            }
            if (!email.equals(settings.defaultEmail, ignoreCase = true)) {
                _events.emit(LoginEvent.Error("Email does not match the saved address."))
                return@launch
            }
            val newPin = Random.nextInt(0, 100_000_000).toString().padStart(8, '0')
            settingsRepository.setPin(newPin)
            _events.emit(LoginEvent.ResetSuccess(email = settings.defaultEmail, newPin = newPin))
        }
    }
}
