package com.example.mylogbook.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LogbookDao {
    @Query("SELECT * FROM LogEntry ORDER BY date DESC")
    fun observeAll(): Flow<List<LogEntry>>

    @Query("SELECT * FROM LogEntry ORDER BY date DESC")
    suspend fun getAll(): List<LogEntry>

    @Query("SELECT * FROM LogEntry WHERE id = :id")
    suspend fun getById(id: Long): LogEntry?

    @Query("SELECT * FROM LogEntry ORDER BY date DESC, updatedAt DESC LIMIT 1")
    suspend fun getLatest(): LogEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntry): Long

    @Update
    suspend fun update(entry: LogEntry)

    @Delete
    suspend fun delete(entry: LogEntry)

    @Query("SELECT DISTINCT day FROM LogEntry WHERE day != '' ORDER BY day")
    fun observeDays(): Flow<List<String>>

    @Query("SELECT DISTINCT fromLocation FROM LogEntry WHERE fromLocation != '' ORDER BY fromLocation")
    fun observeFromLocations(): Flow<List<String>>

    @Query("SELECT DISTINCT addressFrom FROM LogEntry WHERE addressFrom != '' ORDER BY addressFrom")
    fun observeAddressFroms(): Flow<List<String>>

    @Query("SELECT DISTINCT toLocation FROM LogEntry WHERE toLocation != '' ORDER BY toLocation")
    fun observeToLocations(): Flow<List<String>>

    @Query("SELECT DISTINCT addressTo FROM LogEntry WHERE addressTo != '' ORDER BY addressTo")
    fun observeAddressTos(): Flow<List<String>>
}
