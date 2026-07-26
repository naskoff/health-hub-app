package net.softavis.healthhub.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import net.softavis.healthhub.R
import net.softavis.healthhub.data.SessionStore
import net.softavis.healthhub.health.HealthConnectManager

class HealthSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(
    appContext,
    workerParameters,
) {

    override suspend fun doWork(): Result {
        val sessionStore = SessionStore(applicationContext)

        if (sessionStore.getSession() == null) {
            return Result.success()
        }

        setForeground(createForegroundInfo())

        return runCatching {
            val healthConnectManager =
                HealthConnectManager(applicationContext)

            check(healthConnectManager.isAvailable) {
                "Health Connect is not available."
            }

            check(healthConnectManager.hasAllPermissions()) {
                "Health Connect permissions have not been granted."
            }

            val syncService = HealthSyncService(
                sessionStore = sessionStore,
                healthConnectManager = healthConnectManager,
            )

            val result = syncService.sync()

            Result.success(
                workDataOf(
                    KEY_RECORDS_READ to result.recordsRead,
                    KEY_METRICS_SENT to result.metricsSent,
                ),
            )
        }.getOrElse { exception ->
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(
                        KEY_ERROR to (
                                exception.message
                                    ?: "Synchronization failed."
                                ),
                    ),
                )
            }
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(
            applicationContext,
            NOTIFICATION_CHANNEL_ID,
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Health Hub")
            .setContentText("Synchronizing health data…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager =
            applicationContext.getSystemService(
                NotificationManager::class.java,
            )

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Health synchronization",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description =
                "Shows progress while health data is synchronized."
        }

        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_RECORDS_READ = "records_read"
        const val KEY_METRICS_SENT = "metrics_sent"
        const val KEY_ERROR = "error"

        private const val NOTIFICATION_CHANNEL_ID =
            "health_sync"

        private const val NOTIFICATION_ID = 1001
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}