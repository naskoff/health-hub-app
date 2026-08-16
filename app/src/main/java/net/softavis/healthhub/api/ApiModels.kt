package net.softavis.healthhub.api

import com.google.gson.annotations.SerializedName

data class PairDeviceRequest(
    val token: String,
    val name: String,
)

data class PairDeviceResponse(
    val data: PairDeviceData,
)

data class PairDeviceData(
    val id: Long,
    val name: String,

    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("token_type")
    val tokenType: String,
)

data class HealthMetricsRequest(
    val data: List<HealthMetricOperation>,
)

data class HealthMetricsResponse(
    val data: HealthMetricsResponseData,
)

data class HealthMetricsResponseData(
    val status: String,
)

data class HealthMetricOperation(
    val action: HealthMetricAction,

    @SerializedName("source_id")
    val sourceId: String,

    val metrics: List<HealthMetricPayload>? = null,
)

enum class HealthMetricAction {
    @SerializedName("insert")
    Insert,

    @SerializedName("delete")
    Delete,
}

data class HealthMetricPayload(
    @SerializedName("idempotency_key")
    val idempotencyKey: String,

    val type: String,

    val value: Double?,

    val unit: String?,

    @SerializedName("measured_at")
    val measuredAt: String?,

    val data: Map<String, Any?>,
)
