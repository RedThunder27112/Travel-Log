package com.example.mylogbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylogbook.data.LogEntry
import com.example.mylogbook.data.LogbookRepository
import com.example.mylogbook.util.LogbookConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortOption {
    DATE_DESC,
    FROM_ASC,
    TO_ASC
}

data class EntriesUiState(
    val searchQuery: String = "",
    val reasonFilter: String = LogbookConstants.reasonAll,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val entries: List<LogEntry> = emptyList(),
    val filteredEntries: List<LogEntry> = emptyList(),
    val isEmpty: Boolean = true,
    val isFilteredEmpty: Boolean = false
)

private data class EntryFilters(
    val search: String,
    val reason: String,
    val sort: SortOption
)

class EntriesViewModel(private val repository: LogbookRepository) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val reasonFilter = MutableStateFlow(LogbookConstants.reasonAll)
    private val sortOption = MutableStateFlow(SortOption.DATE_DESC)

    private val filters = combine(
        searchQuery,
        reasonFilter,
        sortOption
    ) { search, reason, sort ->
        EntryFilters(search, reason, sort)
    }

    val uiState: StateFlow<EntriesUiState> = combine(
        repository.observeEntries(),
        filters
    ) { entries, filterState ->
        val filtered = entries.filter { entry ->
            val matchesSearch = filterState.search.isBlank() ||
                entry.day.contains(filterState.search, ignoreCase = true) ||
                entry.fromLocation.contains(filterState.search, ignoreCase = true) ||
                entry.addressFrom.contains(filterState.search, ignoreCase = true) ||
                entry.toLocation.contains(filterState.search, ignoreCase = true) ||
                entry.addressTo.contains(filterState.search, ignoreCase = true)
            val matchesReason = filterState.reason == LogbookConstants.reasonAll ||
                entry.reason == filterState.reason
            matchesSearch && matchesReason
        }
        val sorted = when (filterState.sort) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.date }
            SortOption.FROM_ASC -> filtered.sortedBy { it.fromLocation.lowercase() }
            SortOption.TO_ASC -> filtered.sortedBy { it.toLocation.lowercase() }
        }
        EntriesUiState(
            searchQuery = filterState.search,
            reasonFilter = filterState.reason,
            sortOption = filterState.sort,
            entries = entries,
            filteredEntries = sorted,
            isEmpty = entries.isEmpty(),
            isFilteredEmpty = entries.isNotEmpty() && sorted.isEmpty()
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), EntriesUiState())

    fun updateSearch(value: String) {
        searchQuery.update { value }
    }

    fun updateReasonFilter(value: String) {
        reasonFilter.update { value }
    }

    fun updateSortOption(value: SortOption) {
        sortOption.update { value }
    }

    fun clearFilters() {
        viewModelScope.launch {
            reasonFilter.emit(LogbookConstants.reasonAll)
        }
    }
}
