@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.mylogbook.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mylogbook.util.LogbookConstants
import com.example.mylogbook.viewmodel.EditEntryEvent
import com.example.mylogbook.viewmodel.EditEntryViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@Composable
fun EditEntryScreen(
    viewModel: EditEntryViewModel,
    onDone: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var dayMenuExpanded by remember { mutableStateOf(false) }
    var fromMenuExpanded by remember { mutableStateOf(false) }
    var addressFromMenuExpanded by remember { mutableStateOf(false) }
    var toMenuExpanded by remember { mutableStateOf(false) }
    var addressToMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val datePickerDialog = remember(state.date) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                viewModel.updateDate(LocalDate.of(year, month + 1, day))
            },
            state.date.year,
            state.date.monthValue - 1,
            state.date.dayOfMonth
        )
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditEntryEvent.Saved -> {
                    snackbarHostState.showSnackbar("Saved")
                    onDone()
                }
                is EditEntryEvent.Deleted -> {
                    snackbarHostState.showSnackbar("Deleted")
                    onDone()
                }
                is EditEntryEvent.ValidationError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Trip Details", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.date.toString(),
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick date")
                        }
                    }
                )

                val days by viewModel.days.collectAsStateWithLifecycle()
                val filteredDays = days.filter { it.contains(state.day, ignoreCase = true) }.take(8)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.day,
                        onValueChange = {
                            viewModel.updateDay(it)
                            dayMenuExpanded = true
                        },
                        label = { Text("Day") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { dayMenuExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Days")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = dayMenuExpanded && filteredDays.isNotEmpty(),
                        onDismissRequest = { dayMenuExpanded = false }
                    ) {
                        filteredDays.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    viewModel.updateDay(day)
                                    dayMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Reason", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LogbookConstants.reasons.forEach { option ->
                        AssistChip(
                            onClick = { viewModel.updateReason(option) },
                            label = { Text(option) },
                            colors = if (state.reason == option) {
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

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Route", style = MaterialTheme.typography.titleMedium)
                val fromLocations by viewModel.fromLocations.collectAsStateWithLifecycle()
                val toLocationsForFrom by viewModel.toLocations.collectAsStateWithLifecycle()
                val patientSuggestions = remember(fromLocations, toLocationsForFrom) {
                    (fromLocations + toLocationsForFrom).map { it.trim() }.filter { it.isNotBlank() }
                        .distinct()
                        .sortedBy { it.lowercase() }
                }
                var fromShowAll by remember { mutableStateOf(false) }
                val filteredFrom = if (fromShowAll || state.fromLocation.isBlank()) {
                    patientSuggestions
                } else {
                    patientSuggestions.filter { it.contains(state.fromLocation, ignoreCase = true) }
                }.take(12)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.fromLocation,
                        onValueChange = {
                            viewModel.updateFromLocation(it)
                            fromMenuExpanded = true
                            fromShowAll = false
                        },
                        label = { Text("Patient (From)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                fromMenuExpanded = true
                                fromShowAll = true
                            }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Patient from")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = fromMenuExpanded && filteredFrom.isNotEmpty(),
                        onDismissRequest = {
                            fromMenuExpanded = false
                            fromShowAll = false
                        }
                    ) {
                        filteredFrom.forEach { from ->
                            DropdownMenuItem(
                                text = { Text(from) },
                                onClick = {
                                    viewModel.updateFromLocation(from)
                                    fromMenuExpanded = false
                                    fromShowAll = false
                                }
                            )
                        }
                    }
                }

                val addressFroms by viewModel.addressFroms.collectAsStateWithLifecycle()
                val addressTosForFrom by viewModel.addressTos.collectAsStateWithLifecycle()
                val addressSuggestions = remember(addressFroms, addressTosForFrom) {
                    (addressFroms + addressTosForFrom).map { it.trim() }.filter { it.isNotBlank() }
                        .distinct()
                        .sortedBy { it.lowercase() }
                }
                var addressFromShowAll by remember { mutableStateOf(false) }
                val filteredAddressFrom = if (addressFromShowAll || state.addressFrom.isBlank()) {
                    addressSuggestions
                } else {
                    addressSuggestions.filter { it.contains(state.addressFrom, ignoreCase = true) }
                }.take(12)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.addressFrom,
                        onValueChange = {
                            viewModel.updateAddressFrom(it)
                            addressFromMenuExpanded = true
                            addressFromShowAll = false
                        },
                        label = { Text("Address (From)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                addressFromMenuExpanded = true
                                addressFromShowAll = true
                            }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Address from")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = addressFromMenuExpanded && filteredAddressFrom.isNotEmpty(),
                        onDismissRequest = {
                            addressFromMenuExpanded = false
                            addressFromShowAll = false
                        }
                    ) {
                        filteredAddressFrom.forEach { address ->
                            DropdownMenuItem(
                                text = { Text(address) },
                                onClick = {
                                    viewModel.updateAddressFrom(address)
                                    addressFromMenuExpanded = false
                                    addressFromShowAll = false
                                }
                            )
                        }
                    }
                }

                val toLocations by viewModel.toLocations.collectAsStateWithLifecycle()
                val fromLocationsForTo by viewModel.fromLocations.collectAsStateWithLifecycle()
                val patientSuggestionsForTo = remember(toLocations, fromLocationsForTo) {
                    (toLocations + fromLocationsForTo).map { it.trim() }.filter { it.isNotBlank() }
                        .distinct()
                        .sortedBy { it.lowercase() }
                }
                var toShowAll by remember { mutableStateOf(false) }
                val filteredTo = if (toShowAll || state.toLocation.isBlank()) {
                    patientSuggestionsForTo
                } else {
                    patientSuggestionsForTo.filter { it.contains(state.toLocation, ignoreCase = true) }
                }.take(12)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.toLocation,
                        onValueChange = {
                            viewModel.updateToLocation(it)
                            toMenuExpanded = true
                            toShowAll = false
                        },
                        label = { Text("Patient (To)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                toMenuExpanded = true
                                toShowAll = true
                            }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Patient to")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = toMenuExpanded && filteredTo.isNotEmpty(),
                        onDismissRequest = {
                            toMenuExpanded = false
                            toShowAll = false
                        }
                    ) {
                        filteredTo.forEach { to ->
                            DropdownMenuItem(
                                text = { Text(to) },
                                onClick = {
                                    viewModel.updateToLocation(to)
                                    toMenuExpanded = false
                                    toShowAll = false
                                }
                            )
                        }
                    }
                }

                val addressTos by viewModel.addressTos.collectAsStateWithLifecycle()
                val addressFromsForTo by viewModel.addressFroms.collectAsStateWithLifecycle()
                val addressSuggestionsForTo = remember(addressTos, addressFromsForTo) {
                    (addressTos + addressFromsForTo).map { it.trim() }.filter { it.isNotBlank() }
                        .distinct()
                        .sortedBy { it.lowercase() }
                }
                var addressToShowAll by remember { mutableStateOf(false) }
                val filteredAddressTo = if (addressToShowAll || state.addressTo.isBlank()) {
                    addressSuggestionsForTo
                } else {
                    addressSuggestionsForTo.filter { it.contains(state.addressTo, ignoreCase = true) }
                }.take(12)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.addressTo,
                        onValueChange = {
                            viewModel.updateAddressTo(it)
                            addressToMenuExpanded = true
                            addressToShowAll = false
                        },
                        label = { Text("Address (To)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                addressToMenuExpanded = true
                                addressToShowAll = true
                            }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Address to")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = addressToMenuExpanded && filteredAddressTo.isNotEmpty(),
                        onDismissRequest = {
                            addressToMenuExpanded = false
                            addressToShowAll = false
                        }
                    ) {
                        filteredAddressTo.forEach { address ->
                            DropdownMenuItem(
                                text = { Text(address) },
                                onClick = {
                                    viewModel.updateAddressTo(address)
                                    addressToMenuExpanded = false
                                    addressToShowAll = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.odometer,
                    onValueChange = viewModel::updateOdometer,
                    label = { Text("Odometer") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = viewModel::saveEntry) {
                Text("Save")
            }
            OutlinedButton(onClick = onDone) {
                Text("Cancel")
            }
            if (state.isEditing) {
                OutlinedButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteEntry()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            title = { Text("Delete entry?") },
            text = { Text("This cannot be undone.") }
        )
    }
}
