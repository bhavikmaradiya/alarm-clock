package com.bhavikm.calarm.app.data.network

import com.bhavikm.calarm.app.core.data.model.CalendarEvent
import com.bhavikm.calarm.app.data.network.model.AuthCodeRequest
import com.bhavikm.calarm.app.data.network.model.AuthStatus
import com.bhavikm.calarm.app.data.network.model.SubscribeCalendarResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("calendar/subscribe")
    suspend fun subscribeToCalendarChanges(
        @Body request: AuthCodeRequest,
    ): Response<SubscribeCalendarResponse>

    @GET("auth/status")
    suspend fun shouldShowAuthScreen(
        @Header("X-User-Id") userId: String,
    ): Response<AuthStatus>

    @GET("calendar/events")
    suspend fun getCalendarEvents(
        @Header("X-User-Id") userId: String,
        @Query("timeMin") timeMin: String?,
        @Query("timeMax") timeMax: String?,
    ): Response<List<CalendarEvent>?>

}
