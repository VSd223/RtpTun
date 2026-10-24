package com.wdtt.client.profiles

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.wdtt.client.SettingsStore
import java.util.concurrent.TimeUnit

object SubscriptionUpdateScheduler {

    fun scheduleOrCancel(context: Context, intervalHours: Int) {
        val workManager = WorkManager.getInstance(context)

        if (intervalHours == SettingsStore.SUB_AUTO_REFRESH_NEVER || intervalHours == SettingsStore.SUB_AUTO_REFRESH_EVERY_OPEN) {
            Log.i("SubscriptionUpdateScheduler", "Cancelling periodic background subscription updates (interval=$intervalHours)")
            workManager.cancelUniqueWork(SubscriptionUpdateWorker.WORK_NAME)
            return
        }

        val repeatInterval = intervalHours.coerceAtLeast(1).toLong()
        Log.i("SubscriptionUpdateScheduler", "Scheduling periodic subscription update every $repeatInterval hours")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<SubscriptionUpdateWorker>(
            repeatInterval, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            SubscriptionUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }
}
