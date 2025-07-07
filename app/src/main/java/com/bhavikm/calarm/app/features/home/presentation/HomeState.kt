package com.bhavikm.calarm.app.features.home.presentation

import com.bhavikm.calarm.app.core.model.CalendarEvent
import com.google.firebase.auth.FirebaseUser

enum class HomeStatus {
    INITIAL,
    LOADING,
    EMPTY,
    LOADED,
    ERROR,
}

data class HomeState(
    var status: HomeStatus = HomeStatus.INITIAL,
    val events: List<CalendarEvent> = emptyList(),
    val lastSynced: Long? = null,
    val error: String? = null,
    val tick: Long = System.currentTimeMillis(),
    val userData: FirebaseUser? = null,
)
