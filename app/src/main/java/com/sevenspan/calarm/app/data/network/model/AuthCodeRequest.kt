package com.sevenspan.calarm.app.data.network.model

import com.google.gson.annotations.SerializedName

data class AuthCodeRequest(
    @SerializedName("authCode")
    val authCode: String?,
)

data class SubscribeCalendarResponse(
    @SerializedName("sessionId")
    val sessionId: String?,
)
