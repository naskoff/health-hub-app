package net.softavis.healthhub.sync

import android.util.Log
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import net.softavis.healthhub.api.HealthHubApi
import net.softavis.healthhub.api.HealthHubApiFactory
import net.softavis.healthhub.api.HealthMetricAction
import net.softavis.healthhub.api.HealthMetricOperation
import net.softavis.healthhub.api.HealthMetricsRequest
import net.softavis.healthhub.data.SessionStore
import net.softavis.healthhub.health.HealthConnectManager
import net.softavis.healthhub.health.HealthMetricMapper
import java.time.Duration
import java.time.Instant

data class SyncResult(
    val inserted: Int,
    val deleted: Int,
)

class HealthSyncService(
    private val sessionStore: SessionStore,
    private val healthConnectManager: HealthConnectManager,
) {

    /**
     * Full/manual synchronization.
     *
     * Reads the current Health Connect snapshot for the configured time range.
     * It does not advance an existing Changes token.
     */
    suspend fun sync(): SyncResult {
        val session = requireSessionAndPermissions()

        val now = Instant.now()
        val startTime = now.minus(Duration.ofDays(30))

        /*
         * When this is the first synchronization, obtain the Changes token
         * before reading the snapshot. Save it only after the snapshot has
         * been uploaded successfully.
         */
        val initialChangesToken = if (sessionStore.getChangesToken() == null) {
            healthConnectManager.getChangesToken()
        } else {
            null
        }

        val records = healthConnectManager.readAll(
            startTime = startTime,
            endTime = now,
        )

        val operations = records.map { record ->
            val metrics = HealthMetricMapper.map(record)

            if (metrics.isEmpty()) {
                HealthMetricOperation(
                    action = HealthMetricAction.Delete,
                    sourceId = record.metadata.id,
                )
            } else {
                HealthMetricOperation(
                    action = HealthMetricAction.Insert,
                    sourceId = record.metadata.id,
                    metrics = metrics,
                )
            }
        }

        val api = HealthHubApiFactory.create(session.api)

        val result = sendOperations(
            api = api,
            authorization = session.authorization,
            operations = operations,
        )

        initialChangesToken?.let(
            sessionStore::saveChangesToken,
        )

        sessionStore.saveLastSuccessfulSync(
            now.toEpochMilli(),
        )

        return result
    }

    /**
     * Incremental/automatic synchronization.
     *
     * Uses the Health Connect Changes API and advances the token only after
     * the corresponding operations have been accepted by the backend.
     */
    suspend fun syncChanges(): SyncResult {
        var inserted = 0
        var deleted = 0

        val session = requireSessionAndPermissions()

        var token: String = sessionStore.getChangesToken()
            ?: run {
                val newToken = healthConnectManager.getChangesToken()

                sessionStore.saveChangesToken(newToken)

                Log.i(
                    TAG,
                    "Created initial Health Connect Changes token.",
                )

                return SyncResult(
                    inserted = 0,
                    deleted = 0,
                )
            }

        val api = HealthHubApiFactory.create(session.api)

        do {
            val response = healthConnectManager.getChanges(token)

            if (response.changesTokenExpired) {
                sessionStore.clearChangesToken()

                throw IllegalStateException(
                    "Health Connect Changes token expired. " +
                            "A full synchronization is required.",
                )
            }

            val operations = response.changes.map { change ->
                when (change) {
                    is UpsertionChange -> {
                        val record = change.record
                        val metrics = HealthMetricMapper.map(record)

                        Log.i(
                            TAG,
                            "UPSERT " +
                                    "type=${record::class.simpleName} " +
                                    "id=${record.metadata.id} " +
                                    "provider=${record.metadata.dataOrigin.packageName} " +
                                    "lastModified=${record.metadata.lastModifiedTime}",
                        )

                        if (metrics.isEmpty()) {
                            HealthMetricOperation(
                                action = HealthMetricAction.Delete,
                                sourceId = record.metadata.id,
                            )
                        } else {
                            HealthMetricOperation(
                                action = HealthMetricAction.Insert,
                                sourceId = record.metadata.id,
                                metrics = metrics,
                            )
                        }
                    }

                    is DeletionChange -> {
                        Log.i(
                            TAG,
                            "DELETE id=${change.recordId}",
                        )

                        HealthMetricOperation(
                            action = HealthMetricAction.Delete,
                            sourceId = change.recordId,
                        )
                    }

                    else -> error(
                        "Unsupported Health Connect change: ${change::class}",
                    )
                }
            }

            val result = sendOperations(
                api = api,
                authorization = session.authorization,
                operations = operations,
            )

            inserted += result.inserted
            deleted += result.deleted

            /*
             * This page is fully committed to the backend, so its next token
             * is now safe to persist.
             */
            token = response.nextChangesToken
            sessionStore.saveChangesToken(token)
        } while (response.hasMore)

        sessionStore.saveLastSuccessfulSync(
            Instant.now().toEpochMilli(),
        )

        return SyncResult(
            inserted = inserted,
            deleted = deleted,
        )
    }

    private suspend fun sendOperations(
        api: HealthHubApi,
        authorization: String,
        operations: List<HealthMetricOperation>,
    ): SyncResult {
        var inserted = 0
        var deleted = 0

        chunkOperations(
            operations = operations,
            maxMetricsPerBatch = MAX_METRICS_PER_BATCH,
        ).forEach { batch ->
            val response = api.sendMetrics(
                authorization = authorization,
                request = HealthMetricsRequest(
                    data = batch,
                ),
            )

            inserted += response.data.inserted
            deleted += response.data.deleted
        }

        return SyncResult(
            inserted = inserted,
            deleted = deleted,
        )
    }

    private fun chunkOperations(
        operations: List<HealthMetricOperation>,
        maxMetricsPerBatch: Int,
    ): List<List<HealthMetricOperation>> {
        val batches = mutableListOf<MutableList<HealthMetricOperation>>()
        var currentBatch = mutableListOf<HealthMetricOperation>()
        var currentMetrics = 0

        for (operation in operations) {
            val operationMetrics = operation.metrics?.size ?: 0

            if (
                currentBatch.isNotEmpty() &&
                currentMetrics + operationMetrics > maxMetricsPerBatch
            ) {
                batches += currentBatch
                currentBatch = mutableListOf()
                currentMetrics = 0
            }

            currentBatch += operation
            currentMetrics += operationMetrics
        }

        if (currentBatch.isNotEmpty()) {
            batches += currentBatch
        }

        return batches
    }

    private suspend fun requireSessionAndPermissions() =
        requireNotNull(sessionStore.getSession()) {
            "The device is not paired."
        }.also {
            check(healthConnectManager.isAvailable) {
                "Health Connect is not available."
            }

            check(healthConnectManager.hasAllPermissions()) {
                "Health Connect permissions have not been granted."
            }
        }

    companion object {
        private const val TAG = "HealthHubSync"
        private const val MAX_METRICS_PER_BATCH = 500
    }
}