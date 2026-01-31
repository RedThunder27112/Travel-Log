package com.example.mylogbook.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mylogbook.viewmodel.ImportExportViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

@Composable
fun ImportExportScreen(
    importExportViewModel: ImportExportViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val status by importExportViewModel.status.collectAsStateWithLifecycle()
    var useDateRange by remember { mutableStateOf(false) }
    var dateFrom by remember { mutableStateOf<LocalDate?>(null) }
    var dateTo by remember { mutableStateOf<LocalDate?>(null) }

    val fromPicker = remember(dateFrom) {
        DatePickerDialog(
            context,
            { _, y, m, d -> dateFrom = LocalDate.of(y, m + 1, d) },
            dateFrom?.year ?: LocalDate.now().year,
            (dateFrom?.monthValue ?: LocalDate.now().monthValue) - 1,
            dateFrom?.dayOfMonth ?: LocalDate.now().dayOfMonth
        )
    }
    val toPicker = remember(dateTo) {
        DatePickerDialog(
            context,
            { _, y, m, d -> dateTo = LocalDate.of(y, m + 1, d) },
            dateTo?.year ?: LocalDate.now().year,
            (dateTo?.monthValue ?: LocalDate.now().monthValue) - 1,
            dateTo?.dayOfMonth ?: LocalDate.now().dayOfMonth
        )
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            val from = if (useDateRange) dateFrom else null
            val to = if (useDateRange) dateTo else null
            importExportViewModel.exportCsv(context.contentResolver, uri, from, to)
        }
    }

    val exportExcelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri: Uri? ->
        if (uri != null) {
            val from = if (useDateRange) dateFrom else null
            val to = if (useDateRange) dateTo else null
            importExportViewModel.exportExcel(context.contentResolver, uri, from, to)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Export Range", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (useDateRange) {
                        OutlinedButton(onClick = { useDateRange = false }) { Text("All") }
                        Button(onClick = { useDateRange = true }) { Text("Date range") }
                    } else {
                        Button(onClick = { useDateRange = false }) { Text("All") }
                        OutlinedButton(onClick = { useDateRange = true }) { Text("Date range") }
                    }
                }
                if (useDateRange) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dateFrom?.toString() ?: "",
                            onValueChange = {},
                            label = { Text("From") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                OutlinedButton(onClick = { fromPicker.show() }) { Text("Pick") }
                            }
                        )
                        OutlinedTextField(
                            value = dateTo?.toString() ?: "",
                            onValueChange = {},
                            label = { Text("To") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                OutlinedButton(onClick = { toPicker.show() }) { Text("Pick") }
                            }
                        )
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Export", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { exportCsvLauncher.launch("my_logbook_mom.csv") }) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Export CSV")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export to CSV")
                }
                Button(onClick = {
                    val from = if (useDateRange) dateFrom else null
                    val to = if (useDateRange) dateTo else null
                    importExportViewModel.shareCsv(from, to)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share CSV")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share CSV export")
                }
                Button(onClick = {
                    val from = if (useDateRange) dateFrom else null
                    val to = if (useDateRange) dateTo else null
                    importExportViewModel.emailCsv(from, to)
                }) {
                    Icon(Icons.Default.Email, contentDescription = "Email CSV")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Email CSV export")
                }
                Button(onClick = {
                    val from = if (useDateRange) dateFrom else null
                    val to = if (useDateRange) dateTo else null
                    importExportViewModel.shareExcel(from, to)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share Excel")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Excel export")
                }
                Button(onClick = {
                    val from = if (useDateRange) dateFrom else null
                    val to = if (useDateRange) dateTo else null
                    importExportViewModel.emailExcel(from, to)
                }) {
                    Icon(Icons.Default.Email, contentDescription = "Email Excel")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Email Excel export")
                }
                Button(onClick = { exportExcelLauncher.launch("my_logbook_mom.xls") }) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Export Excel")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export to Excel (.xls)")
                }
                if (!status.isNullOrBlank()) {
                    Text(
                        text = status.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

    }
}
