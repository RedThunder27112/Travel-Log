@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.mylogbook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mylogbook.data.LogEntry
import com.example.mylogbook.util.LogbookConstants
import com.example.mylogbook.viewmodel.EntriesUiState
import com.example.mylogbook.viewmodel.SortOption
import kotlinx.coroutines.flow.StateFlow

@Composable
fun EntriesScreen(
    uiState: StateFlow<EntriesUiState>,
    onSearchChange: (String) -> Unit,
    onReasonFilterChange: (String) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onEntryClick: (Long) -> Unit,
    onAddEntry: () -> Unit
) {
    val state by uiState.collectAsStateWithLifecycle()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    placeholder = { Text("Search day, from, to, addresses") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                FilterRow(
                    label = "Reason",
                    options = listOf(LogbookConstants.reasonAll) + LogbookConstants.reasons,
                    selected = state.reasonFilter,
                    onSelected = onReasonFilterChange
                )
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sort", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { sortMenuExpanded = true }) {
                        Text(
                            when (state.sortOption) {
                                SortOption.DATE_DESC -> "Date (newest)"
                                SortOption.FROM_ASC -> "From A-Z"
                                SortOption.TO_ASC -> "To A-Z"
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Date (newest)") },
                            onClick = {
                                onSortChange(SortOption.DATE_DESC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("From A-Z") },
                            onClick = {
                                onSortChange(SortOption.FROM_ASC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("To A-Z") },
                            onClick = {
                                onSortChange(SortOption.TO_ASC)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            if (state.isEmpty) {
                item {
                    EmptyState("No entries yet - tap + to add one")
                }
            } else if (state.isFilteredEmpty) {
                item {
                    EmptyState("No results match your filters")
                }
            } else {
                items(state.filteredEntries, key = { it.id }) { entry ->
                    EntryCard(entry = entry, onClick = { onEntryClick(entry.id) })
                }
            }
        }

        FloatingActionButton(
            onClick = onAddEntry,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                AssistChip(
                    onClick = { onSelected(option) },
                    label = { Text(option) },
                    colors = if (option == selected) {
                        androidx.compose.material3.AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        androidx.compose.material3.AssistChipDefaults.assistChipColors()
                    }
                )
            }
        }
    }
}

@Composable
private fun EntryCard(entry: LogEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${entry.fromLocation} → ${entry.toLocation}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            Text("${entry.date} • ${entry.day}", style = MaterialTheme.typography.bodyMedium)
            AssistChip(
                onClick = {},
                label = { Text(entry.reason) }
            )
            if (entry.odometer != null) {
                Text("Odometer: ${entry.odometer}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
