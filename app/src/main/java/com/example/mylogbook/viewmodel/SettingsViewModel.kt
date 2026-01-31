package com.example.mylogbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylogbook.data.LogEntry
import com.example.mylogbook.data.LogbookRepository
import com.example.mylogbook.data.Settings
import com.example.mylogbook.data.SettingsRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val repository: LogbookRepository
) : ViewModel() {
    val settings: StateFlow<Settings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun setUseSystemTheme(value: Boolean) {
        viewModelScope.launch { settingsRepository.setUseSystemTheme(value) }
    }

    fun setDarkTheme(value: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkTheme(value) }
    }

    fun setDefaultEmail(value: String) {
        viewModelScope.launch { settingsRepository.setDefaultEmail(value) }
    }

    fun setEmailPassword(value: String) {
        viewModelScope.launch { settingsRepository.setEmailPassword(value) }
    }

    fun setPin(value: String) {
        viewModelScope.launch { settingsRepository.setPin(value) }
    }

    fun setLastExportUri(value: String) {
        viewModelScope.launch { settingsRepository.setLastExportUri(value) }
    }

    fun addSampleEntries() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val samples = listOf(
                sampleEntry(today.minusDays(6), "Monday", "Alice", "12 Main St", "Clinic", "45 Health Rd", 120, "Nursing"),
                sampleEntry(today.minusDays(5), "Tuesday", "Bob", "78 Pine Ave", "Hospital", "10 Care Blvd", 132, "Nursing"),
                sampleEntry(today.minusDays(4), "Wednesday", "Charlie", "55 Lake Dr", "Home", "55 Lake Dr", 140, "Personal"),
                sampleEntry(today.minusDays(3), "Thursday", "Daisy", "9 Hill St", "Pharmacy", "22 Pill Rd", 150, "Personal"),
                sampleEntry(today.minusDays(2), "Friday", "Ethan", "33 Oak Ln", "Clinic", "45 Health Rd", 162, "Nursing"),
                sampleEntry(today.minusDays(1), "Saturday", "Fiona", "90 River Rd", "Hospital", "10 Care Blvd", 175, "Nursing"),
                sampleEntry(today, "Sunday", "Grace", "1 Sunset Way", "Home", "1 Sunset Way", 180, "Personal")
            )
            samples.forEach { repository.insert(it) }
        }
    }

    private fun sampleEntry(
        date: LocalDate,
        day: String,
        fromLocation: String,
        addressFrom: String,
        toLocation: String,
        addressTo: String,
        odometer: Int,
        reason: String
    ): LogEntry {
        val now = System.currentTimeMillis()
        return LogEntry(
            date = date,
            day = day,
            fromLocation = fromLocation,
            addressFrom = addressFrom,
            toLocation = toLocation,
            addressTo = addressTo,
            odometer = odometer,
            reason = reason,
            createdAt = now,
            updatedAt = now
        )
    }
}
