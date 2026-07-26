package net.softavis.healthhub.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface HealthHubApi {

    @POST("devices")
    suspend fun pairDevice(
        @Body request: PairDeviceRequest,
    ): PairDeviceResponse

    @POST("health-metrics")
    suspend fun sendMetrics(
        @Header("Authorization") authorization: String,
        @Body request: HealthMetricsRequest,
    )
}