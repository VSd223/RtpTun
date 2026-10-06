// ################################################## 
// FILE: MainActivity.kt 
// FULL PATH: app/src/main/java/com/wdtt/client/MainActivity.kt 
// ################################################## 

package com.wdtt.client

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.rtptun.client.BuildConfig
import com.wdtt.client.ui.AppUpdateDialog
import com.wdtt.client.ui.ProfilesTab
import com.wdtt.client.ui.LogsTab
import com.wdtt.client.ui.SettingsTab
import com.wdtt.client.ui.ExceptionsTab
import com.wdtt.client.ui.AntiBlockTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    var notifGrantedState by mutableStateOf(false)
        private set
    var batteryIgnoredState by mutableStateOf(false)
        private set
    var vpnGrantedState by mutableStateOf(false)
        private set

    fun refreshPermissionStates() {
        notifGrantedState = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        batteryIgnoredState = pm.isIgnoringBatteryOptimizations(packageName)

        vpnGrantedState = android.net.VpnService.prepare(this) == null
    }

    private val batteryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshPermissionStates()
    }

    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshPermissionStates()
        checkAndRequestBattery()
    }

    /**
     * VPN consent живёт на Activity, а не во вкладке Settings:
     * иначе при auto-switch на «Логи» launcher снимается и после «Разрешить» старт зависает.
     */
    @Volatile
    private var pendingAfterVpnGranted: (() -> Unit)? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPermissionStates()
        val cont = pendingAfterVpnGranted
        pendingAfterVpnGranted = null
        if (cont == null) return@registerForActivityResult
        if (android.net.VpnService.prepare(this) == null) {
            cont()
        } else {
            TunnelManager.cancelConnectingIfNeeded()
            Toast.makeText(this, "VPN-разрешение не выдано", Toast.LENGTH_SHORT).show()
        }
    }

    fun prepareVpnThen(onGranted: () -> Unit) {
        val vpnIntent = android.net.VpnService.prepare(this)
        if (vpnIntent != null) {
            pendingAfterVpnGranted = onGranted
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            onGranted()
        }
    }

    companion object {
        var activeActivities = 0
        var isForeground: Boolean
            get() = activeActivities > 0
            set(value) {}

        // Статическая ссылка на текущую Activity
        var currentActivity: MainActivity? = null

        // URI файла .rtptun, ожидающего импорта
        val pendingFileUri = mutableStateOf<Uri?>(null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            AppShortcuts.ACTION_START_TUNNEL -> startTunnelFromShortcut()
            AppShortcuts.ACTION_STOP_TUNNEL -> TunnelControl.stop(applicationContext)
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) {
                    pendingFileUri.value = uri
                }
            }
        }
    }

    private fun startTunnelFromShortcut() {
        if (TunnelManager.running.value || TunnelManager.isConnecting.value) return
        prepareVpnThen { TunnelControl.startFromSavedSettings(applicationContext) }
    }

    override fun onStart() {
        super.onStart()
        activeActivities++
        currentActivity = this
        ManlCaptchaWebViewManager.checkAndShowPendingCaptcha(this)
        VkAuthWebViewManager.checkAndShowPendingAuth(this)
    }

    override fun onStop() {
        super.onStop()
        activeActivities--
        if (currentActivity == this) {
            currentActivity = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestNotifications()
        handleIncomingIntent(intent)

        setContent {
            val settingsStore = remember { SettingsStore(this) }
            var isConfigReady by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    SettingsStore.awaitMigrations(applicationContext)
                    settingsStore.preloadInitialConfig()
                    ProfilesStore(applicationContext).preload()
                }
                refreshPermissionStates()
                isConfigReady = true
            }

            val themeMode by settingsStore.themeMode.collectAsStateWithLifecycle(initialValue = "system")
            val isDynamicColor by settingsStore.isDynamicColor.collectAsStateWithLifecycle(initialValue = false)
            val themePalette by settingsStore.themePalette.collectAsStateWithLifecycle(initialValue = "indigo")
            val firstRunCompletedState by settingsStore.firstRunCompleted.collectAsStateWithLifecycle(initialValue = null)
            var showFirstRunSetup by remember(firstRunCompletedState) { mutableStateOf(firstRunCompletedState == false) }
            val scope = rememberCoroutineScope()

            RTpTUNTheme(themeMode = themeMode, dynamicColor = isDynamicColor, themePalette = themePalette) {
                if (!isConfigReady) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AppBackdrop(modifier = Modifier.matchParentSize())
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    MainScreen(
                        settingsStore = settingsStore,
                        themeMode = themeMode,
                        onThemeChange = { mode ->
                            scope.launch {
                                settingsStore.saveThemeMode(mode)
                            }
                        },
                        isDynamicColor = isDynamicColor,
                        onDynamicColorChange = { enabled ->
                            scope.launch { settingsStore.saveDynamicColor(enabled) }
                        },
                        currentPalette = themePalette,
                        onPaletteChange = { palette ->
                            scope.launch { settingsStore.saveThemePalette(palette) }
                        }
                    )

                    if (showFirstRunSetup) {
                        FirstRunPermissionDialog(
                            notifGranted = notifGrantedState,
                            batteryIgnored = batteryIgnoredState,
                            vpnGranted = vpnGrantedState,
                            onRequestNotif = { requestNotificationPermissionIfNeeded() },
                            onRequestBattery = { checkAndRequestBattery() },
                            onRequestVpn = { prepareVpnThen {} },
                            onComplete = {
                                showFirstRunSetup = false
                                scope.launch { settingsStore.saveFirstRunCompleted(true) }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestNotifications() {
        NotificationHelper.ensureTunnelChannel(this)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkAndRequestBattery()
            }
        } else {
            checkAndRequestBattery()
        }
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun openNotificationSettings() {
        NotificationHelper.openAppNotificationSettings(this)
    }

    private fun checkAndRequestBattery() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                batteryLauncher.launch(intent)
            } catch (_: Exception) {
                // Не удалось показать диалог — пропускаем.
            }
        }
    }
}

@Composable
private fun FirstRunPermissionDialog(
    notifGranted: Boolean,
    batteryIgnored: Boolean,
    vpnGranted: Boolean,
    onRequestNotif: () -> Unit,
    onRequestBattery: () -> Unit,
    onRequestVpn: () -> Unit,
    onComplete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* mandatory initial setup */ },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Первоначальная настройка", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Для работы VPN без сбоев выдайте необходимые разрешения:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // 1. Уведомления
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (notifGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("🔔 Уведомления", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Для капчи и статуса туннеля", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (notifGranted) {
                            Text("✓ Выдано", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        } else {
                            Button(onClick = onRequestNotif, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                                Text("Выдать", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 2. Работа в фоне
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (batteryIgnored) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("🔋 Работа в фоне", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Чтобы MIUI/Android не отключали VPN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (batteryIgnored) {
                            Text("✓ Выдано", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        } else {
                            Button(onClick = onRequestBattery, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                                Text("Выдать", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 3. VPN
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (vpnGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("🛡 VPN-разрешение", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Системное разрешение на VPN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (vpnGranted) {
                            Text("✓ Выдано", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        } else {
                            Button(onClick = onRequestVpn, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                                Text("Выдать", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (notifGranted && batteryIgnored && vpnGranted) "Готово — Начать работу" else "Продолжить")
            }
        }
    )
}

// ═══ Навигация ═══

private data class NavItem(
    val id: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val navItems = listOf(
    NavItem(0, "Подключение", Icons.Filled.PowerSettingsNew, Icons.Outlined.PowerSettingsNew),
    NavItem(1, "Профили", Icons.Filled.Folder, Icons.Outlined.Folder),
    NavItem(2, "Настройки", Icons.Filled.Settings, Icons.Outlined.Settings),
    NavItem(3, "Обход", Icons.Filled.FilterList, Icons.Outlined.FilterList),
    NavItem(4, "Логи", Icons.Filled.Terminal, Icons.Outlined.Terminal),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    settingsStore: SettingsStore,
    themeMode: String = "system",
    onThemeChange: (String) -> Unit = {},
    isDynamicColor: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {},
    currentPalette: String = "indigo",
    onPaletteChange: (String) -> Unit = {}
) {
    val unreadErrors by TunnelManager.unreadErrorCount.collectAsStateWithLifecycle()
    val tunnelRunning by TunnelManager.running.collectAsStateWithLifecycle()
    val openAppSettingsRequest by TunnelManager.openAppSettingsRequest.collectAsStateWithLifecycle()
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var dragTargetIndex by remember { mutableIntStateOf(-1) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val updateCheckIntervalHours by settingsStore.updateCheckIntervalHours.collectAsStateWithLifecycle(
        initialValue = DEFAULT_UPDATE_CHECK_INTERVAL_HOURS
    )
    val includeBetaUpdates by settingsStore.includeBetaUpdates.collectAsStateWithLifecycle(initialValue = false)
    val autoSwitchToLogs by settingsStore.autoSwitchToLogs.collectAsStateWithLifecycle(initialValue = true)
    var pendingSwitchToLogs by remember { mutableStateOf(false) }
    val activeNavItems = navItems
    var pendingRelease by remember { mutableStateOf<AppReleaseInfo?>(null) }
    var showSupportNotice by remember { mutableStateOf(false) }
    val currentVersion = remember { "v${BuildConfig.VERSION_NAME.removePrefix("v")}" }
    val safeBottomInset = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + 96.dp

    LaunchedEffect(selectedTab) {
        if (selectedTab == 4) TunnelManager.clearUnreadErrors()
    }

    // Переход на вкладку "Логи" при запуске подключения
    LaunchedEffect(pendingSwitchToLogs, autoSwitchToLogs) {
        if (pendingSwitchToLogs && autoSwitchToLogs) {
            pendingSwitchToLogs = false
            selectedTab = 4
        }
    }

    // Если туннель успешно запущен — возвращаем пользователя на главный экран (Подключение, индекс 0)
    // Если произошла ошибка (tunnelRunning не стал true) — остаёмся на вкладке "Логи"
    LaunchedEffect(tunnelRunning) {
        if (tunnelRunning && selectedTab == 4) {
            delay(800)
            selectedTab = 0
        }
    }

    LaunchedEffect(activeNavItems, selectedTab) {
        if (activeNavItems.none { it.id == selectedTab }) {
            selectedTab = activeNavItems.firstOrNull()?.id ?: 0
        }
    }

    LaunchedEffect(openAppSettingsRequest) {
        if (openAppSettingsRequest > 0L) {
            selectedTab = 2
        }
    }

    val pendingFileUri = MainActivity.pendingFileUri.value
    LaunchedEffect(pendingFileUri) {
        if (pendingFileUri != null) {
            selectedTab = 1
        }
    }

    var requestCreateProfile by remember { mutableStateOf(false) }

    // Тихое автообновление подписок при открытии (не во время туннеля).
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        val intervalHours = settingsStore.subscriptionAutoRefreshHours.first()
        if (intervalHours == SettingsStore.SUB_AUTO_REFRESH_NEVER) return@LaunchedEffect
        if (TunnelManager.running.value) return@LaunchedEffect

        val profilesStore = ProfilesStore(context)
        val result = runCatching {
            profilesStore.autoRefreshSubscriptionsIfDue(intervalHours)
        }.getOrElse {
            Log.w("RTpTUN", "Subscription auto-refresh failed: ${it.message}")
            null
        } ?: return@LaunchedEffect

        if (result.refreshedOk == 0 && result.failed == 0) return@LaunchedEffect

        Log.i(
            "WDTT",
            "Subscription auto-refresh: ok=${result.refreshedOk} fail=${result.failed} skipped=${result.skippedFresh}"
        )
        when {
            result.failed > 0 && result.refreshedOk == 0 -> {
                Toast.makeText(
                    context,
                    "Не удалось обновить подписки",
                    Toast.LENGTH_SHORT
                ).show()
            }
            result.failed > 0 -> {
                Toast.makeText(
                    context,
                    "Подписки: обновлено ${result.refreshedOk}, ошибок ${result.failed}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            result.refreshedOk > 0 -> {
                Toast.makeText(
                    context,
                    "Подписки обновлены (${result.refreshedOk})",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun dismissSupportNotice() {
        showSupportNotice = false
        scope.launch {
            settingsStore.saveSupportNoticeShownVersionCode(BuildConfig.VERSION_CODE)
        }
    }

    LaunchedEffect(updateCheckIntervalHours, includeBetaUpdates) {
        if (updateCheckIntervalHours == UPDATE_CHECK_NEVER) return@LaunchedEffect

        val intervalMillis = updateIntervalHoursToMillis(updateCheckIntervalHours)
            ?: updateIntervalHoursToMillis(DEFAULT_UPDATE_CHECK_INTERVAL_HOURS)
            ?: 12L * 60L * 60L * 1000L

        suspend fun runUpdateCheck(reason: String) {
            val checkedAt = System.currentTimeMillis()
            val includeBeta = settingsStore.includeBetaUpdates.first()
            val release = fetchLatestReleaseInfo(currentVersion, includeBeta)
            settingsStore.saveUpdateState(
                lastCheckAt = checkedAt,
                latestVersion = release?.versionTag ?: "",
                error = if (release == null) "Не удалось проверить" else ""
            )

            if (release == null) {
                Log.w("RTpTUN", "[WARN] Update check: no release info, local=$currentVersion reason=$reason")
                return
            }

            val hasUpdate = isNewerVersion(currentVersion, release.versionTag, includeBeta)
            val postponeVer = settingsStore.updatePostponeVersion.first()
            val postponeUntil = settingsStore.updatePostponeUntil.first()
            val isPostponed = postponeVer == release.versionTag && checkedAt < postponeUntil
            Log.i(
                "RTpTUN",
                "Update check: local=$currentVersion remote=${release.versionTag} newer=$hasUpdate postponed=$isPostponed reason=$reason"
            )

            if (hasUpdate && !isPostponed) {
                pendingRelease = release
            }
        }

        kotlinx.coroutines.delay(2000)
        runUpdateCheck("startup")

        while (isActive) {
            val now = System.currentTimeMillis()
            val lastCheck = settingsStore.updateLastCheckAt.first()
            val nextCheckAt = lastCheck + intervalMillis
            val waitMs = (nextCheckAt - now).coerceAtLeast(intervalMillis)
            delay(waitMs)
            if (isActive) {
                runUpdateCheck("periodic")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackdrop(modifier = Modifier.matchParentSize())

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            containerColor = Color.Transparent,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (androidx.compose.animation.slideInHorizontally(animationSpec = tween(220, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) { width -> direction * width / 3 } + androidx.compose.animation.fadeIn(animationSpec = tween(220)))
                            .togetherWith(
                                androidx.compose.animation.slideOutHorizontally(animationSpec = tween(220, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { width -> -direction * width / 3 } + androidx.compose.animation.fadeOut(animationSpec = tween(180))
                            )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = navOverlayReserve),
                    label = "tab_content"
                ) { tab ->
                    when (tab) {
                        0 -> SettingsTab(
                            themeMode = themeMode,
                            onThemeChange = onThemeChange,
                            isDynamicColor = isDynamicColor,
                            onDynamicColorChange = onDynamicColorChange,
                            currentPalette = currentPalette,
                            onPaletteChange = onPaletteChange,
                            onConnectRequested = { pendingSwitchToLogs = true },
                            onOpenProfiles = { selectedTab = 1 },
                            onOpenSettings = { selectedTab = 2 }
                        )
                        1 -> ProfilesTab(
                            onProfileApplied = { selectedTab = 0 },
                            importFileUri = MainActivity.pendingFileUri.value,
                            onImportHandled = { MainActivity.pendingFileUri.value = null },
                            requestCreateProfile = requestCreateProfile,
                            onCreateProfileHandled = { requestCreateProfile = false }
                        )
                        2 -> AntiBlockTab(
                            themeMode = themeMode,
                            onThemeChange = onThemeChange,
                            isDynamicColor = isDynamicColor,
                            onDynamicColorChange = onDynamicColorChange,
                            currentPalette = currentPalette,
                            onPaletteChange = onPaletteChange
                        )
                        3 -> ExceptionsTab()
                        4 -> LogsTab()
                    }
                }

                ProxyNavigationBar(
                    navItems = activeNavItems,
                    selectedTab = selectedTab,
                    dragTargetIndex = dragTargetIndex,
                    dragProgress = dragProgress,
                    unreadErrors = unreadErrors,
                    tunnelRunning = tunnelRunning,
                    onTabSelected = { visualIndex ->
                        val tabId = activeNavItems.getOrNull(visualIndex)?.id ?: return@ProxyNavigationBar
                        if (selectedTab != tabId) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            selectedTab = tabId
                            if (tabId == 4) TunnelManager.clearUnreadErrors()
                        }
                        dragTargetIndex = -1
                        dragProgress = 0f
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ProxyNavigationBar(
    navItems: List<NavItem>,
    selectedTab: Int,
    dragTargetIndex: Int,
    dragProgress: Float,
    unreadErrors: Int,
    tunnelRunning: Boolean,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.22f
    val selectedColor = colors.primary
    val unselectedColor = colors.onSurfaceVariant.copy(alpha = 0.55f)
    val shellColor = if (isDark) {
        colors.surface.copy(alpha = 0.78f)
    } else {
        lerp(colors.surface, colors.surfaceVariant, 0.48f).copy(alpha = 0.95f)
    }
    val shellBorder = if (isDark) {
        colors.outlineVariant.copy(alpha = 0.42f)
    } else {
        colors.outline.copy(alpha = 0.16f)
    }
    val indicatorColor = if (isDark) {
        colors.primary.copy(alpha = 0.18f)
    } else {
        lerp(colors.primaryContainer, colors.surface, 0.18f).copy(alpha = 0.94f)
    }
    val indicatorIndex = remember { Animatable(0f) }
    val selectedVisualIndex = navItems.indexOfFirst { it.id == selectedTab }.coerceAtLeast(0)
    val dragVisualIndex = indicatorIndex.value

    LaunchedEffect(selectedVisualIndex, navItems) {
        if (dragTargetIndex !in navItems.indices) {
            indicatorIndex.animateTo(
                targetValue = selectedVisualIndex.toFloat(),
                animationSpec = tween(
                    durationMillis = 720,
                    easing = CubicBezierEasing(0.2f, 0.9f, 0.24f, 1f)
                )
            )
        }
    }

    LaunchedEffect(selectedVisualIndex, dragTargetIndex, dragProgress, navItems) {
        if (dragTargetIndex in navItems.indices) {
            val target = selectedVisualIndex.toFloat() + (dragTargetIndex - selectedVisualIndex) * dragProgress
            indicatorIndex.snapTo(target)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        val trackPadding = 8.dp
        val itemWidth = (maxWidth - trackPadding * 2) / navItems.size
        val indicatorOffset = trackPadding + itemWidth * dragVisualIndex

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = shellColor,
            border = BorderStroke(1.dp, shellBorder),
            tonalElevation = 0.dp,
            shadowElevation = if (isDark) 10.dp else 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = indicatorColor,
                    modifier = Modifier
                        .offset { androidx.compose.ui.unit.IntOffset(x = indicatorOffset.roundToPx(), y = 0) }
                        .padding(vertical = 8.dp)
                        .width(itemWidth)
                        .fillMaxHeight()
                ) {}

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = trackPadding, vertical = 6.dp)
                ) {
                    navItems.forEachIndexed { index, item ->
                        val emphasis = (1f - abs(index - dragVisualIndex)).coerceIn(0f, 1f)
                        val iconColor = lerp(unselectedColor, selectedColor, emphasis)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { onTabSelected(index) },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(22.dp),
                                    tint = iconColor
                                )
                                if (item.id == 4 && unreadErrors > 0) {
                                    Badge(
                                        containerColor = if (tunnelRunning) colors.primary else RTpTUNColors.warning,
                                        contentColor = colors.onPrimary,
                                        modifier = Modifier.offset(x = 12.dp, y = (-8).dp)
                                    ) {
                                        Text("$unreadErrors")
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    letterSpacing = 0.1.sp
                                ),
                                fontWeight = if (emphasis > 0.55f) FontWeight.SemiBold else FontWeight.Normal,
                                color = iconColor.copy(alpha = if (emphasis > 0.4f) 1f else 0.5f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun openReleaseUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
    }
}

private fun android16OrbShape(points: Int, innerRatio: Float): Shape = GenericShape { size, _ ->
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val outerRadius = min(size.width, size.height) / 2f
    val innerRadius = outerRadius * innerRatio

    for (i in 0 until points * 2) {
        val angle = (-PI / 2.0) + (i * PI / points)
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val x = centerX + (radius * cos(angle)).toFloat()
        val y = centerY + (radius * sin(angle)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private val Android16OrbLarge: Shape = android16OrbShape(points = 18, innerRatio = 0.90f)
private val Android16OrbMedium: Shape = android16OrbShape(points = 20, innerRatio = 0.92f)
private val Android16OrbSmall: Shape = android16OrbShape(points = 16, innerRatio = 0.88f)

@Composable
private fun AppBackdrop(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.22f
    val baseBrush = remember(colors.background, colors.surface, colors.surfaceVariant) {
        Brush.verticalGradient(
            colors = if (isDark) {
                listOf(
                    lerp(colors.background, colors.surface, 0.18f),
                    colors.background,
                    lerp(colors.surfaceVariant, colors.background, 0.72f)
                )
            } else {
                listOf(
                    lerp(colors.background, colors.surface, 0.78f),
                    colors.background,
                    lerp(colors.surfaceVariant, colors.background, 0.30f)
                )
            }
        )
    }
    val topGlow = colors.primary.copy(alpha = if (isDark) 0.04f else 0.065f)
    val leftGlow = if (isDark) {
        colors.tertiary.copy(alpha = 0.03f)
    } else {
        lerp(colors.tertiary, colors.secondaryContainer, 0.74f).copy(alpha = 0.16f)
    }
    val bottomGlow = if (isDark) {
        colors.primary.copy(alpha = 0.028f)
    } else {
        lerp(colors.secondary, colors.primaryContainer, 0.70f).copy(alpha = 0.14f)
    }
    val lightOrbOutline = colors.outlineVariant.copy(alpha = 0.18f)
    val topOrbGlow = if (isDark) {
        topGlow
    } else {
        lerp(colors.primary, colors.primaryContainer, 0.72f).copy(alpha = 0.22f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBrush)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-86).dp, y = (-126).dp)
                .size(258.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(topOrbGlow)
                .then(
                    if (isDark) Modifier else Modifier.border(1.dp, lightOrbOutline, androidx.compose.foundation.shape.CircleShape)
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-44).dp, y = 28.dp)
                .size(146.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(leftGlow)
                .then(
                    if (isDark) Modifier else Modifier.border(1.dp, lightOrbOutline.copy(alpha = 0.22f), androidx.compose.foundation.shape.CircleShape)
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 62.dp, y = (-208).dp)
                .size(198.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(bottomGlow)
                .then(
                    if (isDark) Modifier else Modifier.border(1.dp, lightOrbOutline.copy(alpha = 0.20f), androidx.compose.foundation.shape.CircleShape)
                )
        )
    }
}