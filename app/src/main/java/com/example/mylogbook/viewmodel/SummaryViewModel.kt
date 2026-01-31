package com.example.mylogbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylogbook.data.LogEntry
import com.example.mylogbook.data.LogbookRepository
import com.example.mylogbook.util.LogbookConstants
import com.example.mylogbook.util.SummaryData
import com.example.mylogbook.util.SummaryUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class SummaryUiState(
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val reasonFilter: String = LogbookConstants.reasonAll,
    val entries: List<LogEntry> = emptyList(),
    val filteredEntries: List<LogEntry> = emptyList(),
    val summaryData: SummaryData = SummaryUtils.aggregate(emptyList())
)

private data class SummaryFilters(
    val from: LocalDate?,
    val to: LocalDate?,
    val reason: String
)

class SummaryViewModel(private val repository: LogbookRepository) : ViewModel() {
    private val dateFrom = MutableStateFlow<LocalDate?>(null)
    private val dateTo = MutableStateFlow<LocalDate?>(null)
    private val reasonFilter = MutableStateFlow(LogbookConstants.reasonAll)

    private val filters = combine(
        dateFrom,
        dateTo,
        reasonFilter
    ) { from, to, reason ->
        SummaryFilters(from, to, reason)
    }

    val uiState: StateFlow<SummaryUiState> = combine(
        repository.observeEntries(),
        filters
    ) { entries, filterState ->
        val filtered = entries.filter { entry ->
            val matchesReason = filterState.reason == LogbookConstants.reasonAll || entry.reason == filterState.reason
            val matchesFrom = filterState.from == null || !entry.date.isBefore(filterState.from)
            val matchesTo = filterState.to == null || !entry.date.isAfter(filterState.to)
            matchesReason && matchesFrom && matchesTo
        }
        SummaryUiState(
            dateFrom = filterState.from,
            dateTo = filterState.to,
            reasonFilter = filterState.reason,
            entries = entries,
            filteredEntries = filtered,
            summaryData = SummaryUtils.aggregate(filtered)
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), SummaryUiState())

    fun updateDateFrom(value: LocalDate?) {
        dateFrom.value = value
    }

    fun updateDateTo(value: LocalDate?) {
        dateTo.value = value
    }

    fun updateReasonFilter(value: String) {
        reasonFilter.value = value
    }
}
