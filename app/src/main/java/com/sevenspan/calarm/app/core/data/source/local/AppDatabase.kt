package com.sevenspan.calarm.app.core.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.sevenspan.calarm.app.core.model.AppSettings
import com.sevenspan.calarm.app.core.model.AttendeeData
import com.sevenspan.calarm.app.core.model.CalendarEvent
import com.sevenspan.calarm.app.core.model.EventStatus
import kotlinx.serialization.json.Json

@Database(entities = [CalendarEvent::class, AppSettings::class], version = 3, exportSchema = false)
@TypeConverters(EventStatusConverter::class, AttendeeListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun appSettingsDao(): AppSettingsDao
}

class EventStatusConverter {
    @TypeConverter
    fun fromStatus(value: EventStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): EventStatus = EventStatus.valueOf(value)
}

// TypeConverter for List<AttendeeData>
class AttendeeListConverter {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true // Ensure enums are encoded properly if they have default values
    }

    @TypeConverter
    fun fromAttendeeList(attendees: List<AttendeeData>?): String? =
        attendees?.let { json.encodeToString(it) }

    @TypeConverter
    fun toAttendeeList(attendeesString: String?): List<AttendeeData>? =
        attendeesString?.let { json.decodeFromString<List<AttendeeData>>(it) }
}
