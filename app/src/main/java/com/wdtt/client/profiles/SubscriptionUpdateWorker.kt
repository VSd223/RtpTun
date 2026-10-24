package com.wdtt.client.profiles

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wdtt.client.SettingsStore
import com.wdtt.client.TunnelManager
import com.wdtt.client.ProfilesStore
import kotlinx.coroutines.flow.first

class SubscriptionUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i("SubscriptionUpdateWorker", "Starting background subscription auto-update check...")
        val settingsStore = SettingsStore(applicationContext)
        val intervalHours = settingsStore.subscriptionAutoRefreshHours.first()

        if (intervalHours == SettingsStore.SUB_AUTO_REFRESH_NEVER) {
            Log.i("SubscriptionUpdateWorker", "Auto-refresh disabled in settings")
            return Result.success()
        }

        if (TunnelManager.running.value) {
            Log.i("SubscriptionUpdateWorker", "Tunnel is active, skipping subscription update")
            return Result.retry()
        }

        return try {
            val profilesStore = ProfilesStore(applicationContext)
            val res = profilesStore.autoRefreshSubscriptionsIfDue(intervalHours)
            Log.i(
                "SubscriptionUpdateWorker",
                "Subscription update result: ok=${res.refreshedOk}, fail=${res.failed}, skipped=${res.skippedFresh}"
            )
            Result.success()
        } catch (e: Exception) {
            Log.e("SubscriptionUpdateWorker", "Error updating subscriptions in background", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "subscription_auto_update_work"
    }
}
