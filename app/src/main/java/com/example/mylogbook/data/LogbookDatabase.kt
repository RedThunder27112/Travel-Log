package com.example.mylogbook.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [LogEntry::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LogbookDatabase : RoomDatabase() {
    abstract fun logbookDao(): LogbookDao

    companion object {
        @Volatile
        private var Instance: LogbookDatabase? = null

        fun getDatabase(context: Context): LogbookDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, LogbookDatabase::class.java, "logbook.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
