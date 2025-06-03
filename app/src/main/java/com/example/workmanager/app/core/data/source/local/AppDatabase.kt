package com.example.workmanager.app.core.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.workmanager.app.core.domain.model.CalendarEvent
import com.example.workmanager.app.core.domain.model.EventStatus

@Database(entities = [CalendarEvent::class], version = 1)
@TypeConverters(EventStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calendarEventDao(): CalendarEventDao
}


class EventStatusConverter {
    @TypeConverter
    fun fromStatus(value: EventStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): EventStatus = EventStatus.valueOf(value)
}

