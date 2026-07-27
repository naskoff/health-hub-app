package net.softavis.healthhub.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class HealthConnectManager(
    private val context: Context,
) {

    val sdkStatus: Int
        get() = HealthConnectClient.getSdkStatus(context)

    val isAvailable: Boolean
        get() = sdkStatus == HealthConnectClient.SDK_AVAILABLE

    val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /**
     * Health Connect record permissions currently supported by Health Hub.
     *
     * A permission should be added here only when the corresponding record type
     * is also read, mapped and uploaded by the synchronization pipeline.
     */
    private val recordPermissions: Set<String> = setOf(
        // Activity
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),

        // Body measurements
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),

        // Nutrition and hydration
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),

        // Sleep
        HealthPermission.getReadPermission(SleepSessionRecord::class),

        // Vitals
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
    )

    /**
     * Additional Health Connect access capabilities used by the synchronization
     * process. These permissions are not tied to an individual record type.
     */
    private val additionalPermissions: Set<String> = setOf(
        // Allows WorkManager to read Health Connect while the app is not active.
        HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,

        // Allows importing records from other applications that are older than
        // the default 30-day access window.
        HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
    )

    /**
     * Complete set requested by the Health Connect permission contract.
     */
    val permissions: Set<String> = recordPermissions + additionalPermissions

    suspend fun hasAllPermissions(): Boolean {
        if (!isAvailable) {
            return false
        }

        val grantedPermissions =
            client.permissionController.getGrantedPermissions()

        return grantedPermissions.containsAll(permissions)
    }

    suspend fun readAll(
        startTime: Instant,
        endTime: Instant,
    ): List<Record> {
        val records = mutableListOf<Record>()

        records += readType(
            recordType = WeightRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = StepsRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = HeartRateRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = SleepSessionRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = ActiveCaloriesBurnedRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = TotalCaloriesBurnedRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = DistanceRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = HydrationRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = OxygenSaturationRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = BloodPressureRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = BodyFatRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = RespiratoryRateRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = RestingHeartRateRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = ExerciseSessionRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        records += readType(
            recordType = NutritionRecord::class,
            startTime = startTime,
            endTime = endTime,
        )

        return records
    }

    private suspend fun <T : Record> readType(
        recordType: KClass<T>,
        startTime: Instant,
        endTime: Instant,
    ): List<T> {
        val recordTypeName =
            recordType.simpleName ?: recordType.java.name

        Log.i(
            TAG,
            "Reading $recordTypeName",
        )

        val records = readRecords(
            recordType = recordType,
            startTime = startTime,
            endTime = endTime,
        )

        Log.i(
            TAG,
            "Read ${records.size} unique $recordTypeName records",
        )

        delay(DELAY_BETWEEN_RECORD_TYPES)

        return records
    }

    private suspend fun <T : Record> readRecords(
        recordType: KClass<T>,
        startTime: Instant,
        endTime: Instant,
    ): List<T> {
        val recordsById = linkedMapOf<String, T>()
        val usedPageTokens = mutableSetOf<String>()

        var pageToken: String? = null
        var pageNumber = 1

        val recordTypeName =
            recordType.simpleName ?: recordType.java.name

        do {
            if (
                pageToken != null &&
                !usedPageTokens.add(pageToken)
            ) {
                Log.w(
                    TAG,
                    "Repeated page token detected for $recordTypeName. " +
                            "Stopping pagination to prevent an infinite loop.",
                )

                break
            }

            Log.i(
                TAG,
                "Reading page $pageNumber for $recordTypeName",
            )

            val response = readPageWithRetry(
                recordType = recordType,
                startTime = startTime,
                endTime = endTime,
                pageToken = pageToken,
            )

            val recordsBeforePage = recordsById.size

            response.records.forEach { record ->
                recordsById[record.metadata.id] = record
            }

            val uniqueRecordsAdded =
                recordsById.size - recordsBeforePage

            Log.i(
                TAG,
                "Received page $pageNumber for $recordTypeName " +
                        "with ${response.records.size} records, " +
                        "$uniqueRecordsAdded new unique records",
            )

            val nextPageToken = response.pageToken
                ?.takeIf { it.isNotBlank() }

            if (
                nextPageToken != null &&
                nextPageToken == pageToken
            ) {
                Log.w(
                    TAG,
                    "Health Connect returned the same page token for " +
                            "$recordTypeName. Stopping pagination.",
                )

                break
            }

            if (
                response.records.isNotEmpty() &&
                uniqueRecordsAdded == 0
            ) {
                Log.w(
                    TAG,
                    "Health Connect returned only duplicate records for " +
                            "$recordTypeName. Stopping pagination.",
                )

                break
            }

            pageToken = nextPageToken

            if (pageToken != null) {
                delay(DELAY_BETWEEN_PAGES)
                pageNumber++
            }
        } while (pageToken != null)

        return recordsById.values.toList()
    }

    private suspend fun <T : Record> readPageWithRetry(
        recordType: KClass<T>,
        startTime: Instant,
        endTime: Instant,
        pageToken: String?,
    ): ReadRecordsResponse<T> {
        var attempt = 0

        val recordTypeName =
            recordType.simpleName ?: recordType.java.name

        while (true) {
            try {
                Log.i(
                    TAG,
                    "Calling Health Connect for $recordTypeName, " +
                            "attempt=${attempt + 1}, " +
                            "startTime=$startTime, " +
                            "endTime=$endTime, " +
                            "hasPageToken=${pageToken != null}",
                )

                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = recordType,
                        timeRangeFilter = TimeRangeFilter.between(
                            startTime,
                            endTime,
                        ),
                        pageSize = PAGE_SIZE,
                        pageToken = pageToken,
                    ),
                )

                Log.i(
                    TAG,
                    "Health Connect returned for $recordTypeName, " +
                            "records=${response.records.size}, " +
                            "hasNextPage=${response.pageToken != null}",
                )

                Log.i(
                    TAG,
                    "current='${pageToken ?: "<null>"}' (${pageToken?.length ?: 0}), " +
                            "next='${response.pageToken ?: "<null>"}' (${response.pageToken?.length ?: 0})",
                )

                return response
            } catch (exception: CancellationException) {
                Log.w(
                    TAG,
                    "Health Connect read cancelled for $recordTypeName",
                    exception,
                )

                throw exception
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Health Connect read failed for $recordTypeName, " +
                            "attempt=${attempt + 1}",
                    exception,
                )

                if (
                    !exception.isHealthConnectRateLimit() ||
                    attempt >= MAX_RETRY_ATTEMPTS
                ) {
                    throw exception
                }

                val retryDelay =
                    INITIAL_RETRY_DELAY * (1 shl attempt)

                Log.w(
                    TAG,
                    "Health Connect rate limit while reading " +
                            "$recordTypeName. Retrying in $retryDelay.",
                    exception,
                )

                delay(retryDelay)
                attempt++
            }
        }
    }

    private fun Throwable.isHealthConnectRateLimit(): Boolean {
        var current: Throwable? = this

        while (current != null) {
            val message = current.message.orEmpty()

            if (
                message.contains(
                    "rate limited",
                    ignoreCase = true,
                ) ||
                message.contains(
                    "rate limit",
                    ignoreCase = true,
                ) ||
                message.contains(
                    "request quota",
                    ignoreCase = true,
                ) ||
                message.contains(
                    "quota has been exceeded",
                    ignoreCase = true,
                )
            ) {
                return true
            }

            current = current.cause
        }

        return false
    }

    companion object {
        private const val TAG =
            "HealthHubHealthConnect"

        private const val PAGE_SIZE = 1_000

        private val DELAY_BETWEEN_PAGES: Duration =
            500.milliseconds

        private val DELAY_BETWEEN_RECORD_TYPES: Duration =
            2.seconds

        private const val MAX_RETRY_ATTEMPTS = 5

        private val INITIAL_RETRY_DELAY: Duration =
            5.seconds
    }
}