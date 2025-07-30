package com.sevenspan.calarm.app.core.service

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar

class SubscriptionService {

    companion object {
        const val PRO_ENTITLEMENT_ID = "pro"
        const val ALARM_LIMIT_NON_PRO = 3
    }

    fun getTodayMillisRange(): Pair<Long, Long> = run {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDayMillis = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDayMillis = calendar.timeInMillis
        Pair(startOfDayMillis, endOfDayMillis)
    }

    suspend fun isPremiumUser(): Boolean = suspendCancellableCoroutine { continuation ->
        continuation.resume(true) { cause, _, _ -> null }
        /*Purchases.sharedInstance.getCustomerInfoWith(
            fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
            onError = { error ->
                if (continuation.isActive) {
                    continuation.resume(false) { cause, _, _ -> null }
                }
            },
            onSuccess = { customerInfo ->
                val isPro = customerInfo.entitlements[PRO_ENTITLEMENT_ID]?.isActive == true
                if (continuation.isActive) {
                    continuation.resume(isPro) { cause, _, _ -> null }
                }
            }
        )*/
    }
}