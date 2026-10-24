package com.wdtt.client

import android.app.Application
import android.content.Context
import com.wireguard.android.backend.GoBackend
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WdttApplication : Application() {
    @Volatile
    private var backendInstance: GoBackend? = null

    val backend: GoBackend
        get() = getBackend(this)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureTunnelChannel(this)
        AppShortcuts.refreshAsync(this)

        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        scope.launch {
            val settingsStore = SettingsStore(this@WdttApplication)
            val intervalHours = settingsStore.subscriptionAutoRefreshHours.first()
            com.wdtt.client.profiles.SubscriptionUpdateScheduler.scheduleOrCancel(this@WdttApplication, intervalHours)
        }
    }

    fun getBackend(context: Context): GoBackend {
        return backendInstance ?: synchronized(this) {
            backendInstance ?: GoBackend(context.applicationContext).also { backendInstance = it }
        }
    }
}
