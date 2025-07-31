package com.sevenspan.calarm.app.core.service

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import kotlin.coroutines.resume

class SubscriptionService(private val authService: AuthService) {
    private val database: FirebaseDatabase = Firebase.database

    companion object {
        const val PRO_ENTITLEMENT_ID = "pro"
        const val ALARM_LIMIT_NON_PRO = 3
        private const val TAG = "SubscriptionService"
        private const val TRIAL_PERIOD_DAYS = 7
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

    suspend fun isPremiumUser(): Boolean {
        val currentUser = authService.currentUser
        if (currentUser == null) {
            Log.d(TAG, "No current user, considered not premium.")
            return false
        }

        return suspendCancellableCoroutine { continuation ->
            val createdAtRef = database.reference
                .child("users")
                .child(currentUser.uid)
                .child("createdAt")

            createdAtRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var isWithinTrialPeriod = false
                    val createdAt = snapshot.getValue(Long::class.java)

                    if (createdAt != null) {
                        val trialDurationMillis = TRIAL_PERIOD_DAYS * 24 * 60 * 60 * 1000L
                        val trialEndTimeMillis = createdAt + trialDurationMillis
                        isWithinTrialPeriod = System.currentTimeMillis() < trialEndTimeMillis
                        Log.d(
                            TAG,
                            "User ${currentUser.uid}: createdAt = $createdAt, trialEndsAt = $trialEndTimeMillis, isWithinTrialPeriod = $isWithinTrialPeriod"
                        )
                    } else {
                        Log.d(TAG, "User ${currentUser.uid}: createdAt not found in Firebase.")
                    }

                    fetchRevenueCatStatus(continuation, isWithinTrialPeriod)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w(
                        TAG,
                        "User ${currentUser.uid}: Firebase createdAt fetch cancelled: ${databaseError.message}"
                    )
                    fetchRevenueCatStatus(continuation, false)
                }
            })

            continuation.invokeOnCancellation {
                Log.d(TAG, "isPremiumUser coroutine was cancelled.")
            }
        }
    }

    private fun fetchRevenueCatStatus(
        continuation: CancellableContinuation<Boolean>,
        isWithinTrialPeriod: Boolean,
    ) {
        if (!continuation.isActive) {
            Log.d(TAG, "RevenueCat check skipped, coroutine no longer active.")
            return
        }

        Purchases.sharedInstance.getCustomerInfoWith(
            fetchPolicy = CacheFetchPolicy.FETCH_CURRENT, // Or CACHED_OR_FETCHED if desired
            onError = { rcError ->
                Log.w(TAG, "RevenueCat getCustomerInfo error: ${rcError.message}")
                if (continuation.isActive) {
                    Log.d(
                        TAG,
                        "RevenueCat failed. Resolving premium status based on trial: $isWithinTrialPeriod"
                    )
                    continuation.resume(isWithinTrialPeriod)
                }
            },
            onSuccess = { customerInfo ->
                val isProEntitled = customerInfo.entitlements[PRO_ENTITLEMENT_ID]?.isActive == true
                Log.d(
                    TAG,
                    "RevenueCat success. isProEntitled: $isProEntitled. Combined with trial ($isWithinTrialPeriod)."
                )
                if (continuation.isActive) {
                    continuation.resume(isProEntitled || isWithinTrialPeriod)
                }
            }
        )
    }
}