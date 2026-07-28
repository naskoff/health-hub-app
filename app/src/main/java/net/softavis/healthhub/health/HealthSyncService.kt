package net.softavis.healthhub.sync

import net.softavis.healthhub.api.HealthHubApiFactory
import net.softavis.healthhub.api.HealthMetricsRequest
import net.softavis.healthhub.data.SessionStore
import net.softavis.healthhub.health.HealthConnectManager
import net.softavis.healthhub.health.HealthMetricMapper
import java.time.Duration
import java.time.Instant

data class SyncResult(
    val recordsRead: Int,
    val metricsSent: Int,
)

class HealthSyncService(
    private val sessionStore: SessionStore,
    private val healthConnectManager: HealthConnectManager,
) {

    suspend fun sync(): SyncResult {
        val session = requireNotNull(sessionStore.getSession()) {
            "The device is not paired."
        }

        check(healthConnectManager.isAvailable) {
            "Health Connect is not available."
        }

        check(healthConnectManager.hasAllPermissions()) {
            "Health Connect permissions have not been granted."
        }

        val now = Instant.now()

        val startTime = now.minus(Duration.ofDays(1))

        val records = healthConnectManager.readAll(
            startTime = startTime,
            endTime = now,
        )

        val metrics = records.flatMap(HealthMetricMapper::map)

        val api = HealthHubApiFactory.create(session.api)

        metrics
            .chunked(BATCH_SIZE)
            .forEach { batch ->
                api.sendMetrics(
                    authorization = session.authorization,
                    request = HealthMetricsRequest(
                        metrics = batch,
                    ),
                )
            }

        /*
         * Save the upper boundary used for this completed sync, not a new
         * Instant.now(), so records created during the request are not lost.
         */
        sessionStore.saveLastSuccessfulSync(now.toEpochMilli())

        return SyncResult(
            recordsRead = records.size,
            metricsSent = metrics.size,
        )
    }

    companion object {
        private const val BATCH_SIZE = 100
    }
}