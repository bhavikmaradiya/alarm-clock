package com.sevenspan.calarm.app.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Replace with your actual base URL
    private const val BASE_URL = "https://1121cecb569e.ngrok-free.app/"

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    private lateinit var retrofit: Retrofit

    fun initRetrofit(baseUrl: String) {
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Or Level.NONE for release builds
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS) // Optional: Set connection timeout
            .readTimeout(30, TimeUnit.SECONDS) // Optional: Set read timeout
            .writeTimeout(30, TimeUnit.SECONDS) // Optional: Set write timeout
            .build()
    }

    fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
}
