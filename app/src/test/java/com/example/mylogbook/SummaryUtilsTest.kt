package com.example.mylogbook

import com.example.mylogbook.data.LogEntry
import com.example.mylogbook.util.SummaryUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SummaryUtilsTest {
    @Test
    fun aggregateSummaries() {
        val entries = listOf(
            LogEntry(
                id = 1L,
                date = LocalDate.of(2024, 1, 1),
                day = "Monday",
                fromLocation = "Home",
                addressFrom = "Street A",
                toLocation = "Clinic",
                addressTo = "Road B",
                odometer = 10,
                reason = "Nursing",
                createdAt = 0L,
                updatedAt = 0L
            ),
            LogEntry(
                id = 2L,
                date = LocalDate.of(2024, 1, 2),
                day = "Tuesday",
                fromLocation = "Home",
                addressFrom = "Street A",
                toLocation = "Clinic",
                addressTo = "Road B",
                odometer = 12,
                reason = "Personal",
                createdAt = 0L,
                updatedAt = 0L
            ),
            LogEntry(
                id = 3L,
                date = LocalDate.of(2024, 1, 3),
                day = "Wednesday",
                fromLocation = "Office",
                addressFrom = "Street C",
                toLocation = "Gym",
                addressTo = "Road D",
                odometer = 5,
                reason = "Nursing",
                createdAt = 0L,
                updatedAt = 0L
            )
        )

        val summary = SummaryUtils.aggregate(entries)
        assertEquals(3, summary.totals.total)
        assertEquals(2, summary.totals.byReason["Nursing"])
        val clinic = summary.destinations.first { it.destination == "Clinic" }
        assertEquals(2, clinic.count)
        assertEquals(1, clinic.byReason["Nursing"])
        assertEquals(1, clinic.byReason["Personal"])
    }
}
