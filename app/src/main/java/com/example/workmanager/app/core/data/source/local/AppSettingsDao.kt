package com.example.workmanager.app.core.data.source.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.workmanager.app.core.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface AppSettingsDao {

    /**
     * Internal function to retrieve settings which might be null.
     */
    @Query("SELECT * FROM app_settings WHERE userId = :userId LIMIT 1")
    fun getSettingsNullable(userId: String): Flow<AppSettings?>

    /**
     * Retrieves the application settings for a specific user.
     * If no settings are found for the user, it returns a default AppSettings instance.
     *
     * @param userId The ID of the user whose settings are to be retrieved.
     * @return A Flow emitting the AppSettings for the given user (never null).
     */
    fun getSettings(userId: String): Flow<AppSettings> {
        return getSettingsNullable(userId).map { settings ->
            settings ?: AppSettings(userId = userId)
        }
    }

    @Query("SELECT * FROM app_settings WHERE userId = :userId LIMIT 1")
    suspend fun getSettingsOnce(userId: String): AppSettings?

    /**
     * Inserts or updates the application settings for a user.
     * If settings for the user already exist, they will be replaced.
     *
     * @param settings The AppSettings object to upsert.
     */
    @Upsert
    suspend fun upsertSettings(settings: AppSettings)
}
