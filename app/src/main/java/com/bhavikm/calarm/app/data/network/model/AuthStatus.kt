package com.bhavikm.calarm.app.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthStatus(
    val hasValidAccess: Boolean,
    val needsScopeConsent: Boolean,
)