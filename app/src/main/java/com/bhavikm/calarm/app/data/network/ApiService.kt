package com.bhavikm.calarm.app.data.network

import com.bhavikm.calarm.app.data.network.model.AuthCodeRequest
import com.bhavikm.calarm.app.data.network.model.SubscribeCalendarResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("calendar/subscribe")
    suspend fun subscribeToCalendarChanges(
        @Body request: AuthCodeRequest,
    ): Response<SubscribeCalendarResponse>

}
