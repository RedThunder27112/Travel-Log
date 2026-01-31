package com.example.mylogbook.util

import com.example.mylogbook.data.LogEntry

data class SummaryTotals(
    val total: Int,
    val byReason: Map<String, Int>
)

data class DestinationSummary(
    val destination: String,
    val count: Int,
    val byReason: Map<String, Int>
)

data class SummaryData(
    val totals: SummaryTotals,
    val destinations: List<DestinationSummary>
)

object SummaryUtils {
    fun aggregate(entries: List<LogEntry>): SummaryData {
        val total = entries.size
        val byReason = entries.groupingBy { it.reason }.eachCount()

        val destinations = entries
            .groupBy { it.toLocation }
            .map { (destination, items) ->
                val reasonCounts = items.groupingBy { it.reason }.eachCount()
                DestinationSummary(
                    destination = destination,
                    count = items.size,
                    byReason = reasonCounts
                )
            }
            .sortedBy { it.destination.lowercase() }

        return SummaryData(
            totals = SummaryTotals(
                total = total,
                byReason = byReason
            ),
            destinations = destinations
        )
    }
}
