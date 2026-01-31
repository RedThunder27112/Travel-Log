package com.example.mylogbook

import com.example.mylogbook.data.LogEntry
import com.example.mylogbook.util.CsvUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvUtilsTest {
    @Test
    fun buildCsvFormatsCorrectly() {
        val entry = LogEntry(
            id = 1L,
            date = LocalDate.of(2024, 1, 2),
            day = "Tuesday",
            fromLocation = "Home",
            addressFrom = "123 Main St",
            toLocation = "Clinic",
            addressTo = "456 Health Rd",
            odometer = 12,
            reason = "Nursing",
            createdAt = 0L,
            updatedAt = 0L
        )

        val csv = CsvUtils.buildCsv(listOf(entry))
        val lines = csv.trim().split("\n")
        assertEquals("Name:;Teresa Sischy;;;;;;;;", lines[0])
        assertEquals("License Number:;KV 75 BFGP;;;;;;;;", lines[1])
        assertEquals("Tax Year:;2025 + 2026;;;;;;;;", lines[2])
        assertEquals(
            "Date;Day;From;Address;To:;Address;Pvt;Buss;Odometer;Reason for Travel",
            lines[4]
        )
        assertTrue(lines[6].contains("2024-01-02"))
        assertTrue(lines[6].contains("Home"))
        assertTrue(lines[6].contains("Clinic"))
        assertTrue(lines[6].contains("Nursing"))
    }
}
