package com.sevenspan.calarm.app.features.home.presentation

import com.google.firebase.auth.FirebaseUser
import com.sevenspan.calarm.app.core.model.CalendarEvent

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
