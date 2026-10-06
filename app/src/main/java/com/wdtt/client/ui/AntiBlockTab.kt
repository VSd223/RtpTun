package com.wdtt.client.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wdtt.client.GoDnsProbe
import com.wdtt.client.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiBlockTab(
    themeMode: String = "system",
    onThemeChange: (String) -> Unit = {},
    isDynamicColor: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {},
    currentPalette: String = "indigo",
    onPaletteChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }

    val captchaMode by settingsStore.captchaMode.collectAsStateWithLifecycle(initialValue = "auto")
    val captchaSolveMethod by settingsStore.captchaSolveMethod.collectAsStateWithLifecycle(initialValue = "auto")
    val vkAnonPath by settingsStore.vkAnonPath.collectAsStateWithLifecycle(initialValue = "vkcalls")

    val goDnsPreset by settingsStore.goDnsPreset.collectAsStateWithLifecycle(initialValue = "yandex")
    val goDnsCustomStored by settingsStore.goDnsCustom.collectAsStateWithLifecycle(initialValue = "")
    val goDnsDohCustomStored by settingsStore.goDnsDohCustom.collectAsStateWithLifecycle(initialValue = "")

    var goDnsCustomInput by rememberSaveable { mutableStateOf("") }
    var goDnsDohCustomInput by rememberSaveable { mutableStateOf("") }
    var dnsTesting by remember { mutableStateOf(false) }
    var dnsTestResult by remember { mutableStateOf<String?>(null) }

    val turnTcpEnabled by settingsStore.turnTcpEnabled.collectAsStateWithLifecycle(initialValue = true)
    val noDtlsEnabled by settingsStore.noDtlsEnabled.collectAsStateWithLifecycle(initialValue = false)
    val manualPortsEnabled by settingsStore.manualPortsEnabled.collectAsStateWithLifecycle(initialValue = false)
    val serverDtlsPort by settingsStore.serverDtlsPort.collectAsStateWithLifecycle(initialValue = 56000)
    val serverWgPort by settingsStore.serverWgPort.collectAsStateWithLifecycle(initialValue = 56001)
    val serverRawPort by settingsStore.serverRawPort.collectAsStateWithLifecycle(initialValue = 56003)
    val listenPort by settingsStore.listenPort.collectAsStateWithLifecycle(initialValue = 9000)
    val obfsMode by settingsStore.obfsMode.collectAsStateWithLifecycle(initialValue = "audio")
    val detailedLogs by settingsStore.detailedLogs.collectAsStateWithLifecycle(initialValue = false)

    val socksPort by settingsStore.socksPort.collectAsStateWithLifecycle(initialValue = SettingsStore.DEFAULT_SOCKS_PORT)
    val socksAuthEnabled by settingsStore.socksAuthEnabled.collectAsStateWithLifecycle(initialValue = false)
    val savedSocksUsername by settingsStore.socksUsername.collectAsStateWithLifecycle(initialValue = "")
    val savedSocksPassword by settingsStore.socksPassword.collectAsStateWithLifecycle(initialValue = "")

    var socksPortInput by rememberSaveable { mutableStateOf("1080") }
    var socksUsernameInput by rememberSaveable { mutableStateOf("") }
    var socksPasswordInput by rememberSaveable { mutableStateOf("") }

    var dtlsPortInput by rememberSaveable { mutableStateOf("56000") }
    var wgPortInput by rememberSaveable { mutableStateOf("56001") }
    var rawPortInput by rememberSaveable { mutableStateOf("56003") }
    var listenPortInput by rememberSaveable { mutableStateOf("9000") }

    LaunchedEffect(goDnsCustomStored) { goDnsCustomInput = goDnsCustomStored }
    LaunchedEffect(goDnsDohCustomStored) { goDnsDohCustomInput = goDnsDohCustomStored }
    LaunchedEffect(serverDtlsPort) { dtlsPortInput = serverDtlsPort.toString() }
    LaunchedEffect(serverWgPort) { wgPortInput = serverWgPort.toString() }
    LaunchedEffect(serverRawPort) { rawPortInput = serverRawPort.toString() }
    LaunchedEffect(listenPort) { listenPortInput = listenPort.toString() }

    LaunchedEffect(socksPort) { socksPortInput = socksPort.toString() }
    LaunchedEffect(savedSocksUsername) { socksUsernameInput = savedSocksUsername }
    LaunchedEffect(savedSocksPassword) { socksPasswordInput = savedSocksPassword }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = "Настройки приложения",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // ═══ 0. Оформление и темы ═══
        AppSectionCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Оформление и Тема",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentPalette == "indigo",
                        onClick = { onPaletteChange("indigo") },
                        label = { Text("Индиго") }
                    )
                    FilterChip(
                        selected = currentPalette == "cyberpunk",
                        onClick = { onPaletteChange("cyberpunk") },
                        label = { Text("Cyberpunk / Неон") }
                    )
                    FilterChip(
                        selected = currentPalette == "amoled",
                        onClick = { onPaletteChange("amoled") },
                        label = { Text("Amoled Black") }
                    )
                    FilterChip(
                        selected = currentPalette == "forest",
                        onClick = { onPaletteChange("forest") },
                        label = { Text("Лес") }
                    )
                    FilterChip(
                        selected = currentPalette == "espresso",
                        onClick = { onPaletteChange("espresso") },
                        label = { Text("Espresso") }
                    )
                }
            }
        }

        // ═══ 1. Обход Капчи & VK Режим ═══
        AppSectionCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Обход капчи и VK протокол",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Режим разгадывания капчи VK", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Выберите способ решения капчи при подключении к VK:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = captchaMode == "auto" && captchaSolveMethod == "auto",
                            onClick = {
                                scope.launch {
                                    settingsStore.saveCaptchaMode("auto")
                                    settingsStore.saveCaptchaSolveMethod("auto")
                                }
                            },
                            label = { Text("1. АВТО") }
                        )
                        FilterChip(
                            selected = captchaMode == "auto" && captchaSolveMethod == "manual",
                            onClick = {
                                scope.launch {
                                    settingsStore.saveCaptchaMode("auto")
                                    settingsStore.saveCaptchaSolveMethod("manual")
                                }
                            },
                            label = { Text("2. АВТО + РУЧНОЙ (Запасной WebView)") }
                        )
                        FilterChip(
                            selected = captchaMode == "wv",
                            onClick = {
                                scope.launch {
                                    settingsStore.saveCaptchaMode("wv")
                                    settingsStore.saveCaptchaSolveMethod("manual")
                                }
                            },
                            label = { Text("3. РУЧНОЙ (Только WebView)") }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Путь анонимизации VK", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Способ эмуляции трафика звонков VK (по умолчанию vkcalls)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = vkAnonPath == "vkcalls",
                            onClick = {
                                scope.launch { settingsStore.saveVkAnonPath("vkcalls") }
                            },
                            label = { Text("vkcalls (Новый)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = vkAnonPath == "legacy",
                            onClick = {
                                scope.launch { settingsStore.saveVkAnonPath("legacy") }
                            },
                            label = { Text("legacy (Старый)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ═══ 2. Безопасный и Шифрованный DNS ═══
        AppSectionCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Безопасный и Шифрованный DNS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "Защита от подмены DNS провайдером и утечек запросов через DoH (DNS-over-HTTPS)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val dnsPresets = listOf(
                    "yandex" to "Яндекс Безопасный (UDP 77.88.8.8)",
                    "doh-yandex" to "🔒 Яндекс DoH (HTTPS / Шифрованный)",
                    "cloudflare" to "Cloudflare (UDP 1.1.1.1)",
                    "doh-cloudflare" to "🔒 Cloudflare DoH (HTTPS / Шифрованный)",
                    "google" to "Google DNS (UDP 8.8.8.8)",
                    "doh-google" to "🔒 Google DoH (HTTPS / Шифрованный)",
                    "quad9" to "Quad9 (UDP 9.9.9.9)",
                    "doh-quad9" to "🔒 Quad9 DoH (HTTPS / Шифрованный)",
                    "adguard" to "AdGuard (Блокировка рекламы)",
                    "custom" to "Свой UDP IP адрес",
                    "doh-custom" to "🔒 Свой DoH URL (HTTPS)"
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    dnsPresets.forEach { (presetKey, presetName) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = goDnsPreset == presetKey,
                                onClick = {
                                    scope.launch { settingsStore.saveGoDns(presetKey, goDnsCustomInput, goDnsDohCustomInput) }
                                }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                presetName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (presetKey.startsWith("doh")) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = goDnsPreset == "custom") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = goDnsCustomInput,
                            onValueChange = {
                                goDnsCustomInput = it
                                scope.launch { settingsStore.saveGoDns(goDnsPreset, it, goDnsDohCustomInput) }
                            },
                            label = { Text("IP адрес кастомного UDP DNS") },
                            placeholder = { Text("например: 1.1.1.1,8.8.8.8") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        dnsTesting = true
                                        dnsTestResult = null
                                        val res = withContext(Dispatchers.IO) {
                                            GoDnsProbe.checkPreset("custom:$goDnsCustomInput")
                                        }
                                        dnsTesting = false
                                        dnsTestResult = if (res.reachable) "✓ DNS работает (${res.okHosts.firstOrNull() ?: "OK"})"
                                        else "✕ DNS недоступен"
                                    }
                                },
                                enabled = !dnsTesting && goDnsCustomInput.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (dnsTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Verified, null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(if (dnsTesting) "Проверка..." else "Проверить DNS")
                            }

                            dnsTestResult?.let { text ->
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (text.startsWith("✓")) Color(0xFF43A047) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = goDnsPreset == "doh-custom") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = goDnsDohCustomInput,
                            onValueChange = {
                                goDnsDohCustomInput = it
                                scope.launch { settingsStore.saveGoDns(goDnsPreset, goDnsCustomInput, it) }
                            },
                            label = { Text("URL кастомного DoH (HTTPS)") },
                            placeholder = { Text("https://dns.google/dns-query") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // ═══ 3. SOCKS5 Прокси ═══
        AppSectionCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Router,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Настройки SOCKS5 Прокси",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "Локальный SOCKS5 прокси. Чтобы использовать, выберите режим SOCKS5 на главной вкладке.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = socksPortInput,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 5) {
                            socksPortInput = value
                            value.toIntOrNull()?.let { port ->
                                scope.launch { settingsStore.saveSocksPort(port) }
                            }
                        }
                    },
                    label = { Text("Порт SOCKS5 прокси") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            "Авторизация SOCKS5",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Требовать логин и пароль от клиентов",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = socksAuthEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { settingsStore.saveSocksAuthEnabled(enabled) }
                        }
                    )
                }

                AnimatedVisibility(visible = socksAuthEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = socksUsernameInput,
                            onValueChange = { value ->
                                if (value.toByteArray().size <= 255) {
                                    socksUsernameInput = value
                                    scope.launch { settingsStore.saveSocksUsername(value) }
                                }
                            },
                            label = { Text("Логин SOCKS5 (юзер)") },
                            singleLine = true,
                            isError = socksAuthEnabled && socksUsernameInput.isBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        OutlinedTextField(
                            value = socksPasswordInput,
                            onValueChange = { value ->
                                if (value.toByteArray().size <= 255) {
                                    socksPasswordInput = value
                                    scope.launch { settingsStore.saveSocksPassword(value) }
                                }
                            },
                            label = { Text("Пароль SOCKS5") },
                            singleLine = true,
                            isError = socksAuthEnabled && socksPasswordInput.isBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // ═══ 4. Маскировка трафика & Транспорт ═══
        AppSectionCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Router,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Маскировка трафика и Транспорт",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Маскировка RTP пакетов", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Маскировать трафик под аудио (OPUS) или видео (H.264) звонок VK",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = obfsMode == "audio",
                            onClick = { scope.launch { settingsStore.saveObfsMode("audio") } },
                            label = { Text("Аудио (OPUS)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = obfsMode == "video",
                            onClick = { scope.launch { settingsStore.saveObfsMode("video") } },
                            label = { Text("Видео (H.264)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Режим сетевого транспорта", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "UDP — стандартный быстрый транспорт для звонков и видео. TCP (TURN-TCP) — обход глубоких блокировок UDP на строгих провайдерах.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !turnTcpEnabled,
                            onClick = {
                                scope.launch { settingsStore.saveTurnTcpEnabled(false) }
                            },
                            label = { Text("⚡ UDP (Быстрый)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = turnTcpEnabled,
                            onClick = {
                                scope.launch { settingsStore.saveTurnTcpEnabled(true) }
                            },
                            label = { Text("🔒 TCP / TURN-TCP (Обход)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Обфускация No-DTLS (AEAD)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Защита от глубокого анализа пакетов (DPI)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = noDtlsEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { settingsStore.saveNoDtlsEnabled(enabled) }
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Подробные логи диагностирования", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Записывать детализацию сетевых пакетов и шейкшейка",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = detailedLogs,
                        onCheckedChange = { enabled ->
                            scope.launch { settingsStore.saveDetailedLogs(enabled) }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
