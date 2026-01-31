package com.example.mylogbook.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylogbook.data.LogEntry
import com.example.mylogbook.data.LogbookRepository
import com.example.mylogbook.data.SettingsRepository
import com.example.mylogbook.ui.navigation.EntryIdArg
import com.example.mylogbook.util.LogbookConstants
import com.example.mylogbook.util.TextUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

sealed class EditEntryEvent {
    object Saved : EditEntryEvent()
    object Deleted : EditEntryEvent()
    data class ValidationError(val message: String) : EditEntryEvent()
}

data class EditEntryState(
    val id: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val day: String = "",
    val fromLocation: String = "",
    val addressFrom: String = "",
    val toLocation: String = "",
    val addressTo: String = "",
    val odometer: String = "",
    val reason: String = LogbookConstants.reasons.first(),
    val createdAt: Long = System.currentTimeMillis(),
    val isEditing: Boolean = false
)

class EditEntryViewModel(
    private val repository: LogbookRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val entryId: Long = savedStateHandle[EntryIdArg] ?: -1L

    private val _state = MutableStateFlow(EditEntryState())
    val state: StateFlow<EditEntryState> = _state

    private val _events = MutableSharedFlow<EditEntryEvent>()
    val events = _events.asSharedFlow()

    val days: StateFlow<List<String>> = repository.observeDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val fromLocations: StateFlow<List<String>> = repository.observeFromLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val addressFroms: StateFlow<List<String>> = repository.observeAddressFroms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val toLocations: StateFlow<List<String>> = repository.observeToLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val addressTos: StateFlow<List<String>> = repository.observeAddressTos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var dayIsManual = false

    init {
        viewModelScope.launch {
            if (entryId > 0L) {
                val entry = repository.getEntry(entryId)
                if (entry != null) {
                    _state.update {
                        it.copy(
                            id = entry.id,
                            date = entry.date,
                            day = entry.day,
                            fromLocation = entry.fromLocation,
                            addressFrom = entry.addressFrom,
                            toLocation = entry.toLocation,
                            addressTo = entry.addressTo,
                            odometer = entry.odometer?.toString() ?: "",
                            reason = entry.reason,
                            createdAt = entry.createdAt,
                            isEditing = true
                        )
                    }
                    dayIsManual = true
                }
            } else {
                val latest = repository.getLatestEntry()
                _state.update { current ->
                    val defaultDay = dayForDate(current.date)
                    if (latest == null) {
                        current.copy(day = defaultDay, reason = LogbookConstants.reasons.first())
                    } else {
                        current.copy(
                            day = defaultDay,
                            fromLocation = latest.toLocation,
                            addressFrom = latest.addressTo,
                            toLocation = "",
                            addressTo = "",
                            reason = LogbookConstants.reasons.first()
                        )
                    }
                }
            }
        }
    }

    fun updateDate(value: LocalDate) {
        _state.update { current ->
            val updatedDay = if (dayIsManual) current.day else dayForDate(value)
            current.copy(date = value, day = updatedDay)
        }
    }

    fun updateDay(value: String) {
        dayIsManual = value.isNotBlank()
        _state.update { it.copy(day = value) }
    }

    fun updateFromLocation(value: String) {
        _state.update { it.copy(fromLocation = value) }
    }

    fun updateAddressFrom(value: String) {
        _state.update { it.copy(addressFrom = value) }
    }

    fun updateToLocation(value: String) {
        _state.update { it.copy(toLocation = value) }
    }

    fun updateAddressTo(value: String) {
        _state.update { it.copy(addressTo = value) }
    }

    fun updateOdometer(value: String) {
        _state.update { it.copy(odometer = value) }
    }

    fun updateReason(value: String) {
        _state.update { it.copy(reason = value) }
    }

    fun saveEntry() {
        viewModelScope.launch {
            val current = state.value
            val fromLocation = TextUtils.normalizeSpaces(current.fromLocation)
            val toLocation = TextUtils.normalizeSpaces(current.toLocation)
            val day = TextUtils.normalizeSpaces(current.day)
            val addressFrom = TextUtils.normalizeSpaces(current.addressFrom)
            val addressTo = TextUtils.normalizeSpaces(current.addressTo)
            if (fromLocation.isBlank() || toLocation.isBlank()) {
                _events.emit(EditEntryEvent.ValidationError("Date, From, and To are required."))
                return@launch
            }

            val odometer = if (current.odometer.isBlank()) {
                null
            } else {
                current.odometer.toIntOrNull()?.takeIf { it >= 0 }
            }
            if (current.odometer.isNotBlank() && odometer == null) {
                _events.emit(EditEntryEvent.ValidationError("Odometer must be a valid number."))
                return@launch
            }

            val now = System.currentTimeMillis()
            val entry = LogEntry(
                id = current.id ?: 0L,
                date = current.date,
                day = day,
                fromLocation = fromLocation,
                addressFrom = addressFrom,
                toLocation = toLocation,
                addressTo = addressTo,
                odometer = odometer,
                reason = current.reason,
                createdAt = if (current.isEditing) current.createdAt else now,
                updatedAt = now
            )

            if (current.isEditing) {
                repository.update(entry)
            } else {
                repository.insert(entry)
            }

            settingsRepository.setLastFromLocation(fromLocation)
            settingsRepository.setLastToLocation(toLocation)

            _events.emit(EditEntryEvent.Saved)
        }
    }

    fun deleteEntry() {
        viewModelScope.launch {
            val current = state.value
            if (!current.isEditing || current.id == null) return@launch
            repository.delete(
                LogEntry(
                    id = current.id,
                    date = current.date,
                    day = current.day,
                    fromLocation = current.fromLocation,
                    addressFrom = current.addressFrom,
                    toLocation = current.toLocation,
                    addressTo = current.addressTo,
                    odometer = current.odometer.toIntOrNull(),
                    reason = current.reason,
                    createdAt = current.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _events.emit(EditEntryEvent.Deleted)
        }
    }

    private fun dayForDate(date: LocalDate): String {
        return date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }
}
