package com.example.mylogbook.util

import com.example.mylogbook.data.LogEntry

data class CsvTable(
    val headers: List<String>,
    val rows: List<List<String>>
)

object CsvUtils {
    private const val delimiter = ';'
    private val headerOrder = listOf(
        "Date",
        "Day",
        "From",
        "Address",
        "To:",
        "Address",
        "Pvt",
        "Buss",
        "Odometer",
        "Reason for Travel"
    )

    fun buildCsv(entries: List<LogEntry>): String {
        val builder = StringBuilder()
        val emptyRow = List(headerOrder.size) { "" }
        fun appendRow(values: List<String>) {
            builder.append(values.joinToString(delimiter.toString()) { escape(it, delimiter) }).append("\n")
        }

        appendRow(listOf("Name:", "Teresa Sischy") + List(headerOrder.size - 2) { "" })
        appendRow(listOf("License Number:", "KV 75 BFGP") + List(headerOrder.size - 2) { "" })
        appendRow(listOf("Tax Year:", "2025 + 2026") + List(headerOrder.size - 2) { "" })
        appendRow(emptyRow)
        appendRow(headerOrder)
        val baselineRow = emptyRow.toMutableList().apply {
            this[8] = "0"
            this[9] = "<- Prev Month Odomoter"
        }
        appendRow(baselineRow)

        entries.forEachIndexed { index, entry ->
            val rowNumber = 7 + index
            val distanceFormula = "=I$rowNumber-I${rowNumber - 1}"
            val isPersonal = entry.reason.equals("Personal", ignoreCase = true)
            val pvtValue = if (isPersonal) distanceFormula else "0"
            val bussValue = if (isPersonal) "0" else "$distanceFormula-G$rowNumber"
            val row = listOf(
                entry.date.toString(),
                entry.day,
                entry.fromLocation,
                entry.addressFrom,
                entry.toLocation,
                entry.addressTo,
                pvtValue,
                bussValue,
                entry.odometer?.toString().orEmpty(),
                entry.reason
            )
            appendRow(row)
        }
        if (entries.isNotEmpty()) {
            val lastRow = 6 + entries.size
            appendRow(
                listOf(
                    "",
                    "",
                    "",
                    "",
                    "",
                    "TOTAL",
                    "=SUM(G7:G$lastRow)",
                    "=SUM(H7:H$lastRow)",
                    "=(I$lastRow-I6)",
                    ""
                )
            )
        }
        return builder.toString()
    }

    fun parseCsvTable(content: String): CsvTable {
        val delimiter = detectDelimiter(content)
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (inQuotes) {
                if (c == '"') {
                    val next = content.getOrNull(i + 1)
                    if (next == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                    }
                    '\r' -> {
                        // ignore
                    }
                    else -> currentField.append(c)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow)
        }
        if (rows.isEmpty()) return CsvTable(emptyList(), emptyList())
        val headerIndex = findHeaderRowIndex(rows)
        val headers = if (headerIndex != null) rows[headerIndex] else rows.first()
        val dataRows = if (headerIndex != null) rows.drop(headerIndex + 1) else rows.drop(1)
        val filteredRows = dataRows.filter { row ->
            row.any { value -> value.isNotBlank() } &&
                row.none { value -> value.trim().equals("total", ignoreCase = true) }
        }
        return CsvTable(headers, filteredRows)
    }

    private fun detectDelimiter(content: String): Char {
        val firstLine = content.lineSequence().firstOrNull { it.isNotBlank() } ?: return ','
        var inQuotes = false
        var commaCount = 0
        var semiCount = 0
        for (c in firstLine) {
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (!inQuotes) {
                if (c == ',') commaCount++
                if (c == ';') semiCount++
            }
        }
        return if (semiCount > commaCount) ';' else ','
    }

    private fun escape(value: String, delimiter: Char): String {
        val needsQuotes = value.contains(delimiter) || value.contains('"') || value.contains('\n')
        if (!needsQuotes) return value
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun findHeaderRowIndex(rows: List<List<String>>): Int? {
        val required = setOf("date", "from", "to")
        rows.forEachIndexed { index, row ->
            val normalized = row.map { normalizeHeader(it) }.toSet()
            if (required.all { normalized.contains(it) }) {
                return index
            }
        }
        return null
    }

    private fun normalizeHeader(value: String): String {
        val normalized = value.lowercase().replace(Regex("[^a-z0-9]"), "")
        return when (normalized) {
            "reasonfortravel" -> "reason"
            else -> normalized
        }
    }
}
