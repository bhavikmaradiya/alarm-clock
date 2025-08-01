package com.bhavikm.calarm.app.data.network.model

import com.google.gson.annotations.SerializedName

data class AuthCodeRequest(
    @SerializedName("authCode") // Match your backend's expected field names
    val authCode: String,
    @SerializedName("userId")
    val userId: String,
)

data class SubscribeCalendarResponse(
    @SerializedName("sessionId") // Match your backend's expected field names
    val sessionId: String?,
)