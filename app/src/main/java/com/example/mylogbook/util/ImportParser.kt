package com.example.mylogbook.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class EntryInput(
    val date: LocalDate,
    val day: String,
    val fromLocation: String,
    val addressFrom: String,
    val toLocation: String,
    val addressTo: String,
    val odometer: Int?,
    val reason: String
)

data class ImportParseResult(
    val entries: List<EntryInput>,
    val errors: List<String>
)

object ImportParser {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val slashFormatter1 = DateTimeFormatter.ofPattern("d/M/yyyy")
    private val slashFormatter2 = DateTimeFormatter.ofPattern("M/d/yyyy")
    private val slashFormatter3 = DateTimeFormatter.ofPattern("yyyy/M/d")
    private val slashFormatter4 = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    fun parseTable(headers: List<String>, rows: List<List<String>>): ImportParseResult {
        if (headers.isEmpty()) return ImportParseResult(emptyList(), listOf("Missing header row."))

        val indexByHeader = buildHeaderIndex(headers)
        fun idx(key: String): Int? = indexByHeader[key]

        val errors = mutableListOf<String>()
        val entries = mutableListOf<EntryInput>()

        val requiredKeys = listOf(
            "date",
            "from",
            "to"
        )

        for (key in requiredKeys) {
            if (idx(key) == null) {
                errors.add("Missing required column: $key")
            }
        }

        if (errors.isNotEmpty()) {
            return ImportParseResult(emptyList(), errors)
        }

        rows.forEachIndexed { rowIndex, row ->
            val rowNumber = rowIndex + 2
            fun valueFor(key: String): String {
                val index = idx(key) ?: return ""
                val raw = row.getOrNull(index)?.trim().orEmpty()
                return if (raw == "-") "" else raw
            }

            val dateValue = valueFor("date")
            val dayValue = TextUtils.normalizeSpaces(valueFor("day"))
            val fromValue = TextUtils.normalizeSpaces(valueFor("from"))
            val addressFromValue = TextUtils.normalizeSpaces(valueFor("addressfrom"))
            val toValue = TextUtils.normalizeSpaces(valueFor("to"))
            val addressToValue = TextUtils.normalizeSpaces(valueFor("addressto"))
            val odometerRaw = valueFor("odometer")
            val reasonValue = valueFor("reason")

            if (dateValue.isBlank() || fromValue.isBlank() || toValue.isBlank()) {
                errors.add("Row $rowNumber: missing required fields.")
                return@forEachIndexed
            }

            val date = parseDate(dateValue)
            if (date == null) {
                errors.add("Row $rowNumber: invalid date '$dateValue'.")
                return@forEachIndexed
            }

            val reason = if (reasonValue.isBlank()) {
                LogbookConstants.reasons.last()
            } else {
                normalizeChoice(reasonValue, LogbookConstants.reasons)
                    ?: run {
                        errors.add("Row $rowNumber: invalid reason '$reasonValue'.")
                        return@forEachIndexed
                    }
            }

            val odometer = parseOdometer(odometerRaw)

            entries.add(
                EntryInput(
                    date = date,
                    day = dayValue,
                    fromLocation = fromValue,
                    addressFrom = addressFromValue,
                    toLocation = toValue,
                    addressTo = addressToValue,
                    odometer = odometer,
                    reason = reason
                )
            )
        }

        return ImportParseResult(entries, errors)
    }

    private fun buildHeaderIndex(headers: List<String>): Map<String, Int> {
        val indexByHeader = mutableMapOf<String, Int>()
        var seenFrom = false
        var seenTo = false
        headers.forEachIndexed { index, header ->
            val normalized = normalizeHeader(header)
            when (normalized) {
                "date" -> indexByHeader.putIfAbsent("date", index)
                "day" -> indexByHeader.putIfAbsent("day", index)
                "from" -> {
                    indexByHeader.putIfAbsent("from", index)
                    seenFrom = true
                }
                "to" -> {
                    indexByHeader.putIfAbsent("to", index)
                    seenTo = true
                }
                "addressfrom" -> indexByHeader.putIfAbsent("addressfrom", index)
                "addressto" -> indexByHeader.putIfAbsent("addressto", index)
                "address" -> {
                    val target = if (!indexByHeader.containsKey("addressfrom") && (seenFrom && !seenTo)) {
                        "addressfrom"
                    } else if (seenTo) {
                        "addressto"
                    } else if (!indexByHeader.containsKey("addressfrom")) {
                        "addressfrom"
                    } else {
                        "addressto"
                    }
                    indexByHeader.putIfAbsent(target, index)
                }
                "odometer" -> indexByHeader.putIfAbsent("odometer", index)
                "reason", "reasonfortravel" -> indexByHeader.putIfAbsent("reason", index)
            }
        }
        return indexByHeader
    }

    private fun normalizeHeader(value: String): String {
        val normalized = value.lowercase().replace(Regex("[^a-z0-9]"), "")
        return when (normalized) {
            "addressfrom" -> "addressfrom"
            "addressto" -> "addressto"
            else -> normalized
        }
    }

    private fun parseDate(value: String): LocalDate? {
        return try {
            LocalDate.parse(value, isoFormatter)
        } catch (_: DateTimeParseException) {
            try {
                LocalDate.parse(value, slashFormatter1)
            } catch (_: DateTimeParseException) {
                try {
                    LocalDate.parse(value, slashFormatter2)
                } catch (_: DateTimeParseException) {
                    try {
                        LocalDate.parse(value, slashFormatter3)
                    } catch (_: DateTimeParseException) {
                        try {
                            LocalDate.parse(value, slashFormatter4)
                        } catch (_: DateTimeParseException) {
                            null
                        }
                    }
                }
            }
        }
    }

    private fun normalizeChoice(value: String, options: List<String>): String? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        return options.firstOrNull { it.equals(trimmed, ignoreCase = true) }
    }

    private fun parseOdometer(value: String): Int? {
        if (value.isBlank()) return null
        val trimmed = value.trim()
        if (trimmed == "-") return null
        return trimmed.toIntOrNull()
    }
}
