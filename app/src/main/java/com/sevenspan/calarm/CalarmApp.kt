package com.sevenspan.calarm

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import com.meticha.triggerx.dsl.TriggerX
import com.meticha.triggerx.provider.TriggerXDataProvider
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.sevenspan.calarm.app.core.data.source.local.CalendarEventDao
import com.sevenspan.calarm.app.core.di.appModule
import com.sevenspan.calarm.app.core.model.CalendarEventBundleConverter.toBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import java.net.HttpURLConnection
import java.net.URL

class CalarmApp :
    Application(),
    KoinComponent {

    companion object Companion {
        private const val TAG = "App"

        suspend fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            val hasNetwork = when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else                                                               -> false
            }

            if (!hasNetwork) return false

            return isInternetAvailable()
        }

        suspend fun isInternetAvailable(): Boolean {
            return withContext(Dispatchers.IO) {
                return@withContext try {
                    val url =
                        URL("https://www.google.com/generate_204")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 2000
                    connection.readTimeout = 2000
                    connection.connect()
                    connection.responseCode == 204
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        Purchases.logLevel = LogLevel.VERBOSE
        Purchases.configure(
            PurchasesConfiguration.Builder(this, getString(R.string.revenuecat_api_key)).build()
        )

        startKoin {
            androidLogger()
            androidContext(this@CalarmApp)
            workManagerFactory()
            modules(appModule)
        }

        TriggerX.init(this) {
            useDefaultNotification(
                title = "Alarm running",
                message = "Tap to open",
                channelName = "Alarm Notifications",

                )

            activityClass = AlarmActivity::class.java

            alarmDataProvider = object : TriggerXDataProvider {
                override suspend fun provideData(alarmId: Int, alarmType: String): Bundle {
                    Log.d(TAG, "Providing data for alarmId: $alarmId, alarmType: $alarmType")
                    // TODO: Implement actual data provision based on alarmId and alarmType
                    val calendarEventDao: CalendarEventDao by inject()
                    val eventFromDb = calendarEventDao.getEventById(alarmId)
                    return if (eventFromDb != null) {
                        Log.d(TAG, "Providing data for event: ${eventFromDb.eventName}")
                        eventFromDb.toBundle()
                    } else {
                        Log.w(TAG, "No event found for alarmId: $alarmId to provide data.")
                        Bundle.EMPTY
                    }
                }
            }
        }
    }
}
