package com.sevenspan.calarm.app.core.service

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

interface AnalyticsService {
    fun logEvent(eventName: String, paramsBlock: MutableMap<String, Any>.() -> Unit)
}

class FirebaseAnalyticsService(private val analyticsService: FirebaseAnalytics) :
    AnalyticsService {

    override fun logEvent(eventName: String, paramsBlock: MutableMap<String, Any>.() -> Unit) {
        analyticsService.logEvent(eventName) {
            val params = mutableMapOf<String, Any>()
            params.paramsBlock()
            params.forEach {
                when (it.value) {
                    is String -> {
                        param(it.key, it.value as String)
                    }

                    is Long -> {
                        param(it.key, it.value as Long)
                    }

                    is Double -> {
                        param(it.key, it.value as Double)
                    }

                    else -> throw IllegalArgumentException("Unsupported type")
                }
            }
        }
    }
}
