package com.example.mylogbook.data

import kotlinx.coroutines.flow.Flow

class LogbookRepository(private val dao: LogbookDao) {
    fun observeEntries(): Flow<List<LogEntry>> = dao.observeAll()

    suspend fun getAllEntries(): List<LogEntry> = dao.getAll()

    suspend fun getEntry(id: Long): LogEntry? = dao.getById(id)

    suspend fun getLatestEntry(): LogEntry? = dao.getLatest()

    suspend fun insert(entry: LogEntry): Long = dao.insert(entry)

    suspend fun update(entry: LogEntry) = dao.update(entry)

    suspend fun delete(entry: LogEntry) = dao.delete(entry)

    fun observeDays(): Flow<List<String>> = dao.observeDays()

    fun observeFromLocations(): Flow<List<String>> = dao.observeFromLocations()

    fun observeAddressFroms(): Flow<List<String>> = dao.observeAddressFroms()

    fun observeToLocations(): Flow<List<String>> = dao.observeToLocations()

    fun observeAddressTos(): Flow<List<String>> = dao.observeAddressTos()
}
