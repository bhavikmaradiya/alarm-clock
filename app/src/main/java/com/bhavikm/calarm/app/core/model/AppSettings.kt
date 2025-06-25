package com.bhavikm.calarm.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val userId: String, // User ID from Firebase/Google Sign-In
    val lastSyncedTime: Long? = null, // Timestamp of the last successful sync
    val defaultDelayBeforeTriggerMinutes: Int = 1, // Default delay in minutes
    val defaultDaysToSyncFromNow: Int = 2, // Default number of days to sync from now
)
