package com.example.mylogbook.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: LocalDate,
    val day: String,
    val fromLocation: String,
    val addressFrom: String,
    val toLocation: String,
    val addressTo: String,
    val odometer: Int?,
    val reason: String,
    val createdAt: Long,
    val updatedAt: Long
)
