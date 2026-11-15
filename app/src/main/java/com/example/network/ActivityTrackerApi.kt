package com.example.network

import com.example.model.HealthResponse
import com.example.model.LiveStatusResponse
import com.example.model.SummaryTodayResponse
import com.example.model.WidgetTodayResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ActivityTrackerApi {

    @GET("api/v1/widget/today")
    suspend fun getWidgetToday(
        @Query("top") top: Int = 4,
    ): WidgetTodayResponse

    @GET("api/v1/summary/today")
    suspend fun getSummaryToday(): SummaryTodayResponse

    @GET("api/v1/status")
    suspend fun getLiveStatus(): LiveStatusResponse

    @GET("api/v1/health")
    suspend fun getHealth(): HealthResponse
}
