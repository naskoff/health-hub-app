package net.softavis.healthhub.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class HealthSyncScheduler(
    context: Context,
) {

    private val workManager =
        WorkManager.getInstance(context.applicationContext)

    fun syncNow() {
        val request =
            OneTimeWorkRequestBuilder<HealthSyncWorker>()
                .setInputData(
                    Data.Builder()
                        .putBoolean(
                            HealthSyncWorker.KEY_FULL_SYNC,
                            true,
                        )
                        .build(),
                )
                .setConstraints(networkConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS,
                )
                .build()

        workManager.enqueueUniqueWork(
            UNIQUE_IMMEDIATE_SYNC,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun schedulePeriodicSync() {
        val request =
            PeriodicWorkRequestBuilder<HealthSyncWorker>(
                1,
                TimeUnit.HOURS,
            )
                .setConstraints(networkConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS,
                )
                .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(
            UNIQUE_IMMEDIATE_SYNC,
        )

        workManager.cancelUniqueWork(
            UNIQUE_PERIODIC_SYNC,
        )
    }

    private fun networkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(
                NetworkType.CONNECTED,
            )
            .setRequiresBatteryNotLow(true)
            .build()
    }

    companion object {
        const val UNIQUE_IMMEDIATE_SYNC =
            "health-sync-now"

        const val UNIQUE_PERIODIC_SYNC =
            "health-sync-periodic"
    }
}