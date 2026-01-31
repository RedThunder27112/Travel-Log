package com.example.mylogbook.util

import com.example.mylogbook.data.LogEntry
import java.io.InputStream
import java.io.OutputStream

object ExcelUtils {
    fun writeWorkbook(entries: List<LogEntry>, outputStream: OutputStream) {
        val header = listOf(
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
        val html = buildString {
            appendLine("<html>")
            appendLine("<head>")
            appendLine("<meta charset=\"UTF-8\"/>")
            appendLine("<style>")
            appendLine("table, th, td { border: 1px solid #000; border-collapse: collapse; }")
            appendLine("th, td { padding: 4px; }")
            appendLine("th { font-weight: bold; }")
            appendLine("</style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("<table>")

            fun emptyRow() = List(header.size) { "" }
            fun rowHtml(
                cells: List<String>,
                headerRow: Boolean = false,
                boldColumns: Set<Int> = emptySet()
            ) {
                append("<tr>")
                cells.forEachIndexed { index, value ->
                    val tag = if (headerRow) "th" else "td"
                    val baseValue = if (value.isBlank()) "&nbsp;" else escapeHtml(value)
                    val cellValue = if (boldColumns.contains(index)) "<b>$baseValue</b>" else baseValue
                    append("<$tag>$cellValue</$tag>")
                }
                appendLine("</tr>")
            }

            rowHtml(listOf("Name:", "Teresa Sischy") + List(header.size - 2) { "" })
            rowHtml(listOf("License Number:", "KV 75 BFGP") + List(header.size - 2) { "" })
            rowHtml(listOf("Tax Year:", "2025 + 2026") + List(header.size - 2) { "" })
            rowHtml(emptyRow())
            rowHtml(header, headerRow = true)
            val baselineRow = emptyRow().toMutableList().apply {
                this[8] = "0"
                this[9] = "<- Prev Month Odomoter"
            }
            rowHtml(baselineRow, boldColumns = setOf(9))

            entries.forEachIndexed { index, entry ->
                val rowNumber = 7 + index
                val distanceFormula = "=I$rowNumber-I${rowNumber - 1}"
                val isPersonal = entry.reason.equals("Personal", ignoreCase = true)
                val pvtValue = if (isPersonal) distanceFormula else "0"
                val bussValue = if (isPersonal) "0" else "$distanceFormula-G$rowNumber"
                rowHtml(
                    listOf(
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
                )
            }

            if (entries.isNotEmpty()) {
                val lastRow = 6 + entries.size
                rowHtml(
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

            appendLine("</table>")
            appendLine("</body>")
            appendLine("</html>")
        }

        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(html)
        }
    }

    fun readWorkbook(inputStream: InputStream): ImportParseResult {
        return ImportParseResult(emptyList(), listOf("Excel import is disabled"))
    }

    private fun escapeHtml(value: String): String {
        return buildString {
            value.forEach { ch ->
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(ch)
                }
            }
        }
    }
}
