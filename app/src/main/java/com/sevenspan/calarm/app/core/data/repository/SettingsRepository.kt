package com.sevenspan.calarm.app.core.data.repository

import com.sevenspan.calarm.app.core.data.source.local.AppSettingsDao
import com.sevenspan.calarm.app.core.model.AppSettings
import com.sevenspan.calarm.app.core.service.AuthService
import kotlinx.coroutines.flow.Flow

/*interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
}*/

class SettingsRepository(
    private val authService: AuthService,
    private val settingsDao: AppSettingsDao,
) {
    fun getSettings(): Flow<AppSettings> {
        val userId = requireNotNull(authService.currentUser?.uid)
        return settingsDao.getSettings(userId)
    }

    suspend fun saveSettings(settings: AppSettings) {
        val userId = requireNotNull(authService.currentUser?.uid)
        settingsDao.upsertSettings(settings.copy(userId = userId))
    }
}
