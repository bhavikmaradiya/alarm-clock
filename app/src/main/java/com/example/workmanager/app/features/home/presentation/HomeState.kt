package com.example.workmanager.app.features.home.presentation

import com.example.workmanager.app.core.domain.model.CalendarEvent


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
    val error: String? = null,
)
