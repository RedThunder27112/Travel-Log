package com.example.mylogbook.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylogbook.data.LogEntry
import com.example.mylogbook.data.LogbookRepository
import com.example.mylogbook.data.SettingsRepository
import com.example.mylogbook.util.CsvUtils
import com.example.mylogbook.util.EmailSender
import com.example.mylogbook.util.ExcelUtils
import com.example.mylogbook.util.ImportParser
import com.example.mylogbook.util.ImportParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ImportReport(
    val imported: Int,
    val skipped: Int,
    val errors: Int
)

sealed class ImportExportEvent {
    data class Message(val text: String) : ImportExportEvent()
    data class ShareCsv(val uri: Uri) : ImportExportEvent()
    data class ShareExcel(val uri: Uri) : ImportExportEvent()
}

class ImportExportViewModel(
    private val repository: LogbookRepository,
    private val settingsRepository: SettingsRepository,
    private val appContext: Context
) : ViewModel() {
    private val _events = MutableSharedFlow<ImportExportEvent>()
    val events = _events.asSharedFlow()
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    fun exportCsv(resolver: ContentResolver, uri: Uri, from: LocalDate? = null, to: LocalDate? = null) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val entries = getEntriesForRange(from, to)
                    val csv = CsvUtils.buildCsv(entries)
                    val outputStream = resolver.openOutputStream(uri)
                        ?: throw IllegalStateException("Unable to open output stream")
                    outputStream.use { output ->
                        output.write(csv.toByteArray(Charsets.UTF_8))
                    }
                }
                persistPermission(resolver, uri)
                settingsRepository.setLastExportUri(uri.toString())
                emitMessage("CSV exported")
            } catch (e: Exception) {
                emitMessage("Export failed: ${e.message}")
            }
        }
    }

    fun exportExcel(resolver: ContentResolver, uri: Uri, from: LocalDate? = null, to: LocalDate? = null) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val entries = getEntriesForRange(from, to)
                    val outputStream = resolver.openOutputStream(uri)
                        ?: throw IllegalStateException("Unable to open output stream")
                    outputStream.use { output ->
                        ExcelUtils.writeWorkbook(entries, output)
                    }
                }
                persistPermission(resolver, uri)
                settingsRepository.setLastExportUri(uri.toString())
                emitMessage("Excel exported")
            } catch (t: Throwable) {
                emitMessage("Export failed: ${t.message ?: t::class.java.simpleName}")
            }
        }
    }

    fun exportSummaryCsv(resolver: ContentResolver, uri: Uri, entries: List<LogEntry>) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val summary = buildSummaryCsv(entries)
                    val outputStream = resolver.openOutputStream(uri)
                        ?: throw IllegalStateException("Unable to open output stream")
                    outputStream.use { output ->
                        output.write(summary.toByteArray(Charsets.UTF_8))
                    }
                }
                persistPermission(resolver, uri)
                settingsRepository.setLastExportUri(uri.toString())
                emitMessage("Summary exported")
            } catch (e: Exception) {
                emitMessage("Export failed: ${e.message}")
            }
        }
    }

    fun importExcel(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val parseResult = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { input ->
                        ExcelUtils.readWorkbook(input)
                    } ?: ImportParseResult(emptyList(), listOf("Empty file"))
                }
                val report = applyImport(parseResult)
                persistPermission(resolver, uri)
                handleImportErrors(parseResult.errors, report)
            } catch (t: Throwable) {
                emitMessage("Import failed: ${t.message ?: t::class.java.simpleName}")
            }
        }
    }

    fun shareCsv(from: LocalDate? = null, to: LocalDate? = null) {
        viewModelScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) { generateCsvFileUri(from, to) }
                _events.emit(ImportExportEvent.ShareCsv(uri))
            } catch (e: Exception) {
                emitMessage("Share failed: ${e.message}")
            }
        }
    }

    fun shareExcel(from: LocalDate? = null, to: LocalDate? = null) {
        viewModelScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) { generateExcelFileUri(from, to) }
                _events.emit(ImportExportEvent.ShareExcel(uri))
            } catch (e: Exception) {
                emitMessage("Share failed: ${e.message}")
            }
        }
    }

    fun emailCsv(from: LocalDate? = null, to: LocalDate? = null) {
        viewModelScope.launch {
            try {
                val currentSettings = settingsRepository.settings.first()
                if (currentSettings.defaultEmail.isBlank() || currentSettings.emailPassword.isBlank()) {
                    emitMessage("Set email and password in Settings first")
                    return@launch
                }

                val file = withContext(Dispatchers.IO) { generateCsvFile(from, to) }
                withContext(Dispatchers.IO) {
                    EmailSender.sendEmailWithAttachment(
                        fromEmail = currentSettings.defaultEmail,
                        password = currentSettings.emailPassword,
                        toEmail = currentSettings.defaultEmail,
                        subject = "My LogBook Mom Export",
                        body = "Attached is your My LogBook Mom CSV export.",
                        attachment = file
                    )
                }
                emitMessage("Email sent")
            } catch (e: Exception) {
                emitMessage("Email failed: ${e.message}")
            }
        }
    }

    fun emailExcel(from: LocalDate? = null, to: LocalDate? = null) {
        viewModelScope.launch {
            try {
                val currentSettings = settingsRepository.settings.first()
                if (currentSettings.defaultEmail.isBlank() || currentSettings.emailPassword.isBlank()) {
                    emitMessage("Set email and password in Settings first")
                    return@launch
                }

                val file = withContext(Dispatchers.IO) { generateExcelFile(from, to) }
                withContext(Dispatchers.IO) {
                    EmailSender.sendEmailWithAttachment(
                        fromEmail = currentSettings.defaultEmail,
                        password = currentSettings.emailPassword,
                        toEmail = currentSettings.defaultEmail,
                        subject = "My LogBook Mom Excel Export",
                        body = "Attached is your My LogBook Mom Excel export.",
                        attachment = file
                    )
                }
                emitMessage("Email sent")
            } catch (e: Exception) {
                emitMessage("Email failed: ${e.message}")
            }
        }
    }

    fun importShared(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val mimeType = resolver.getType(uri).orEmpty().lowercase()
                val name = resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else ""
                }.orEmpty()

                val lowerName = name.lowercase()
                val isCsv = mimeType.contains("csv") || lowerName.endsWith(".csv")
                val isXls = mimeType.contains("application/vnd.ms-excel") || lowerName.endsWith(".xls")
                val isXlsx = mimeType.contains("openxmlformats-officedocument.spreadsheetml.sheet") || lowerName.endsWith(".xlsx")

                when {
                    isXls -> importExcel(resolver, uri)
                    isXlsx -> emitMessage("XLSX import is disabled")
                    isCsv -> emitMessage("CSV import is disabled")
                    else -> emitMessage("Unsupported file type")
                }
            } catch (t: Throwable) {
                emitMessage("Import failed: ${t.message ?: t::class.java.simpleName}")
            }
        }
    }

    private suspend fun emitMessage(text: String) {
        _status.value = text
        _events.emit(ImportExportEvent.Message(text))
    }

    private suspend fun handleImportErrors(errors: List<String>, report: ImportReport) {
        if (errors.isEmpty()) {
            emitMessage("Import completed: ${report.imported} imported, ${report.skipped} skipped, 0 errors")
            return
        }
        val logFile = writeImportErrorLog(errors)
        val settings = settingsRepository.settings.first()
        if (settings.defaultEmail.isNotBlank() && settings.emailPassword.isNotBlank()) {
            try {
                withContext(Dispatchers.IO) {
                    EmailSender.sendEmailWithAttachment(
                        fromEmail = settings.defaultEmail,
                        password = settings.emailPassword,
                        toEmail = settings.defaultEmail,
                        subject = "My LogBook Mom Import Errors",
                        body = "Attached is the import error report.",
                        attachment = logFile
                    )
                }
                emitMessage("Import completed: ${report.imported} imported, ${report.skipped} skipped, ${errors.size} errors (report emailed)")
            } catch (e: Exception) {
                emitMessage("Import completed: ${report.imported} imported, ${report.skipped} skipped, ${errors.size} errors (email failed)")
            }
        } else {
            emitMessage("Import completed: ${report.imported} imported, ${report.skipped} skipped, ${errors.size} errors (set email to get report)")
        }
    }

    private suspend fun applyImport(parseResult: ImportParseResult): ImportReport {
        val existing = repository.getAllEntries()
        val existingKeys = existing.map {
            keyFor(
                it.date.toString(),
                it.fromLocation,
                it.addressFrom,
                it.toLocation,
                it.addressTo,
                it.odometer
            )
        }.toMutableSet()
        var imported = 0
        var skipped = 0
        parseResult.entries.forEach { input ->
            val key = keyFor(
                input.date.toString(),
                input.fromLocation,
                input.addressFrom,
                input.toLocation,
                input.addressTo,
                input.odometer
            )
            if (existingKeys.contains(key)) {
                skipped++
            } else {
                val now = System.currentTimeMillis()
                repository.insert(
                    LogEntry(
                        date = input.date,
                        day = input.day,
                        fromLocation = input.fromLocation,
                        addressFrom = input.addressFrom,
                        toLocation = input.toLocation,
                        addressTo = input.addressTo,
                        odometer = input.odometer,
                        reason = input.reason,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                existingKeys.add(key)
                imported++
            }
        }
        return ImportReport(imported, skipped, parseResult.errors.size)
    }

    private fun keyFor(
        date: String,
        fromLocation: String,
        addressFrom: String,
        toLocation: String,
        addressTo: String,
        odometer: Int?
    ): String {
        return listOf(
            date,
            fromLocation.lowercase(),
            addressFrom.lowercase(),
            toLocation.lowercase(),
            addressTo.lowercase(),
            odometer?.toString().orEmpty()
        ).joinToString("|")
    }

    private fun persistPermission(resolver: ContentResolver, uri: Uri) {
        try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Ignore if not persistable.
        }
    }

    private suspend fun generateCsvFile(from: LocalDate?, to: LocalDate?): File {
        val entries = getEntriesForRange(from, to)
        val csv = CsvUtils.buildCsv(entries)
        val exportsDir = File(appContext.cacheDir, "exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val file = File(exportsDir, "my_logbook_mom_$timestamp.csv")
        file.writeText(csv, Charsets.UTF_8)
        return file
    }

    private suspend fun generateExcelFile(from: LocalDate?, to: LocalDate?): File {
        val entries = getEntriesForRange(from, to)
        val exportsDir = File(appContext.cacheDir, "exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val file = File(exportsDir, "my_logbook_mom_$timestamp.xls")
        file.outputStream().use { output ->
            ExcelUtils.writeWorkbook(entries, output)
        }
        return file
    }

    private fun writeImportErrorLog(errors: List<String>): File {
        val exportsDir = File(appContext.cacheDir, "exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val file = File(exportsDir, "import_errors_$timestamp.txt")
        file.writeText(errors.joinToString("\n"), Charsets.UTF_8)
        return file
    }

    private suspend fun generateCsvFileUri(from: LocalDate?, to: LocalDate?): Uri {
        val file = generateCsvFile(from, to)
        return FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        )
    }

    private suspend fun generateExcelFileUri(from: LocalDate?, to: LocalDate?): Uri {
        val file = generateExcelFile(from, to)
        return FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        )
    }

    private suspend fun getEntriesForRange(from: LocalDate?, to: LocalDate?): List<LogEntry> {
        val entries = repository.getAllEntries()
        val sorted = entries.sortedBy { it.date }
        if (from == null && to == null) return sorted
        val normalized = normalizeRange(from, to)
        return sorted.filter { entry ->
            val date = entry.date
            val afterFrom = normalized.first?.let { !date.isBefore(it) } ?: true
            val beforeTo = normalized.second?.let { !date.isAfter(it) } ?: true
            afterFrom && beforeTo
        }
    }

    private fun normalizeRange(from: LocalDate?, to: LocalDate?): Pair<LocalDate?, LocalDate?> {
        if (from != null && to != null && from.isAfter(to)) {
            return Pair(to, from)
        }
        return Pair(from, to)
    }

    private fun buildSummaryCsv(entries: List<LogEntry>): String {
        val summary = com.example.mylogbook.util.SummaryUtils.aggregate(entries)
        val header = listOf("Destination", "Count") + com.example.mylogbook.util.LogbookConstants.reasons
        val builder = StringBuilder()
        builder.append(header.joinToString(",")).append("\n")
        fun escape(value: String): String {
            val needsQuotes = value.contains(',') || value.contains('\"') || value.contains('\n')
            if (!needsQuotes) return value
            val escaped = value.replace("\"", "\"\"")
            return "\"$escaped\""
        }

        summary.destinations.forEach { item ->
            val row = mutableListOf<String>()
            row.add(item.destination)
            row.add(item.count.toString())
            com.example.mylogbook.util.LogbookConstants.reasons.forEach { reason ->
                row.add((item.byReason[reason] ?: 0).toString())
            }
            builder.append(row.joinToString(",") { escape(it) }).append("\n")
        }
        return builder.toString()
    }
}
