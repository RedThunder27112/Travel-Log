package com.example.mylogbook

import com.example.mylogbook.util.ImportParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportParserTest {
    @Test
    fun parseTableReadsEntries() {
        val headers = listOf(
            "Date",
            "Day",
            "From",
            "AddressFrom",
            "To",
            "AddressTo",
            "Odometer",
            "Reason"
        )
        val rows = listOf(
            listOf(
                "2024-03-05",
                "Tuesday",
                "Home",
                "123 Main St",
                "Clinic",
                "456 Health Rd",
                "18",
                "Nursing"
            )
        )

        val result = ImportParser.parseTable(headers, rows)
        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.entries.size)
        val entry = result.entries.first()
        assertEquals("Home", entry.fromLocation)
        assertEquals("Clinic", entry.toLocation)
        assertEquals(18, entry.odometer)
    }
}
