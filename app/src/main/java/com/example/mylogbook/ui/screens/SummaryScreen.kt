package com.example.mylogbook.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mylogbook.data.LogEntry
import com.example.mylogbook.util.LogbookConstants
import com.example.mylogbook.viewmodel.SummaryUiState
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

@Composable
fun SummaryScreen(
    uiState: StateFlow<SummaryUiState>,
    onDateFromChange: (LocalDate?) -> Unit,
    onDateToChange: (LocalDate?) -> Unit,
    onReasonFilterChange: (String) -> Unit,
    onExportSummary: (Uri, List<LogEntry>) -> Unit
) {
    val state by uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var expandedProcedures by remember { mutableStateOf(setOf<String>()) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            onExportSummary(uri, state.filteredEntries)
        }
    }

    val fromPicker = remember(state.dateFrom) {
        DatePickerDialog(
            context,
            { _, y, m, d -> onDateFromChange(LocalDate.of(y, m + 1, d)) },
            state.dateFrom?.year ?: LocalDate.now().year,
            (state.dateFrom?.monthValue ?: LocalDate.now().monthValue) - 1,
            state.dateFrom?.dayOfMonth ?: LocalDate.now().dayOfMonth
        )
    }
    val toPicker = remember(state.dateTo) {
        DatePickerDialog(
            context,
            { _, y, m, d -> onDateToChange(LocalDate.of(y, m + 1, d)) },
            state.dateTo?.year ?: LocalDate.now().year,
            (state.dateTo?.monthValue ?: LocalDate.now().monthValue) - 1,
            state.dateTo?.dayOfMonth ?: LocalDate.now().dayOfMonth
        )
    }

    LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Filters", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.dateFrom?.toString() ?: "",
                            onValueChange = {},
                            label = { Text("From") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                OutlinedButton(onClick = { fromPicker.show() }) { Text("Pick") }
                            }
                        )
                        OutlinedTextField(
                            value = state.dateTo?.toString() ?: "",
                            onValueChange = {},
                            label = { Text("To") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                OutlinedButton(onClick = { toPicker.show() }) { Text("Pick") }
                            }
                        )
                    }

                    FilterDropdown(
                        label = "Reason",
                        selected = state.reasonFilter,
                        options = listOf(LogbookConstants.reasonAll) + LogbookConstants.reasons,
                        onSelected = onReasonFilterChange
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Totals", style = MaterialTheme.typography.titleMedium)
                    Text("Total entries: ${state.summaryData.totals.total}")
                    Text("Nursing: ${state.summaryData.totals.byReason[LogbookConstants.reasons[0]] ?: 0}")
                    Text("Personal: ${state.summaryData.totals.byReason[LogbookConstants.reasons[1]] ?: 0}")
                }
            }
        }

        item {
            Button(onClick = { exportLauncher.launch("my_logbook_mom_summary.csv") }) {
                androidx.compose.material3.Icon(Icons.Default.ArrowDownward, contentDescription = "Export")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Summary")
            }
        }

        item {
            Text("Destination Breakdown", style = MaterialTheme.typography.titleMedium)
        }

        items(state.summaryData.destinations) { item ->
            val expanded = expandedProcedures.contains(item.destination)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expandedProcedures = if (expanded) {
                            expandedProcedures - item.destination
                        } else {
                            expandedProcedures + item.destination
                        }
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.destination, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(item.count.toString())
                    }
                    if (expanded) {
                        Text("Nursing: ${item.byReason[LogbookConstants.reasons[0]] ?: 0}")
                        Text("Personal: ${item.byReason[LogbookConstants.reasons[1]] ?: 0}")
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                OutlinedButton(onClick = { expanded = true }) { Text("Select") }
            }
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
