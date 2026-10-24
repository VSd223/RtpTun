package com.wdtt.client.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var dnsDropdownExpanded by remember { mutableStateOf(false) }

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

    val dnsPresets = remember {
        listOf(
            "yandex" to "🇷🇺 Яндекс Безопасный (UDP 77.88.8.8)",
            "doh-yandex" to "🔒 Яндекс DoH (Шифрованный HTTPS)",
            "cloudflare" to "⚡ Cloudflare (UDP 1.1.1.1)",
            "doh-cloudflare" to "🔒 Cloudflare DoH (Шифрованный HTTPS)",
            "google" to "🌐 Google DNS (UDP 8.8.8.8)",
            "doh-google" to "🔒 Google DoH (Шифрованный HTTPS)",
            "mts" to "📡 МТС DNS",
            "beeline" to "📡 Билайн DNS",
            "megafon" to "📡 Мегафон DNS",
            "tele2" to "📡 Теле2 DNS",
            "sbermobile" to "📡 Сбермобайл DNS",
            "volna" to "📡 Волна мобайл DNS",
            "custom" to "⚙️ Свой UDP IP адрес",
            "doh-custom" to "⚙️ Свой DoH URL (HTTPS)"
        )
    }

    val currentDnsName = dnsPresets.find { it.first == goDnsPreset }?.second ?: "Выберите DNS"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Шапка
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Параметры и обход",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Гибкое управление защитой, DNS и транспортом",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ═══ 0. Оформление и темы ═══
        AppSectionCard {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .animateContentSize(),
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
                        text = "Оформление интерфейса",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemePreviewCard("indigo", "Индиго", Color(0xFF5B588D), Color(0xFF7E79B5), currentPalette, onPaletteChange)
                    ThemePreviewCard("cyberpunk", "Cyberpunk", Color(0xFF00E5FF), Color(0xFFFF007F), currentPalette, onPaletteChange)
                    ThemePreviewCard("amoled", "Amoled", Color(0xFF00E676), Color(0xFF121212), currentPalette, onPaletteChange)
                    ThemePreviewCard("forest", "Лес", Color(0xFF5F5D68), Color(0xFF2E7D32), currentPalette, onPaletteChange)
                    ThemePreviewCard("espresso", "Espresso", Color(0xFF6D4C41), Color(0xFF8D6E63), currentPalette, onPaletteChange)
                }
            }
        }

        // ═══ 1. Обход Капчи & VK Режим ═══
        AppSectionCard {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Режим обхода (Звонок или Капча)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    
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
                            label = { Text("📞 Звонок (Авто по умолчанию)") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = captchaMode == "wv" || captchaMode == "rjs",
                            onClick = {
                                scope.launch {
                                    settingsStore.saveCaptchaMode("wv")
                                    settingsStore.saveCaptchaSolveMethod("manual")
                                }
                            },
                            label = { Text("🧩 Капча (Ручной ввод)") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = captchaMode == "wv" || captchaMode == "rjs",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Метод ручного ввода капчи:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = captchaMode == "wv",
                                    onClick = {
                                        scope.launch {
                                            settingsStore.saveCaptchaMode("wv")
                                            settingsStore.saveCaptchaSolveMethod("manual")
                                        }
                                    },
                                    label = { Text("🌐 wv (WebView)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                FilterChip(
                                    selected = captchaMode == "rjs",
                                    onClick = {
                                        scope.launch {
                                            settingsStore.saveCaptchaMode("rjs")
                                            settingsStore.saveCaptchaSolveMethod("manual")
                                        }
                                    },
                                    label = { Text("⚡ rjs (RJS)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Путь анонимизации трафика", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = vkAnonPath == "vkcalls",
                            onClick = { scope.launch { settingsStore.saveVkAnonPath("vkcalls") } },
                            label = { Text("vkcalls (Новый)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = vkAnonPath == "legacy",
                            onClick = { scope.launch { settingsStore.saveVkAnonPath("legacy") } },
                            label = { Text("legacy (Старый)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // ═══ 2. Безопасный и Шифрованный DNS ═══
        AppSectionCard {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "Шифрованный DNS (DoH / UDP)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    "Защита от перехвата и подмены DNS-запросов провайдером",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = dnsDropdownExpanded,
                    onExpandedChange = { dnsDropdownExpanded = !dnsDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentDnsName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dnsDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = dnsDropdownExpanded,
                        onDismissRequest = { dnsDropdownExpanded = false }
                    ) {
                        dnsPresets.forEach { (presetKey, presetName) ->
                            DropdownMenuItem(
                                text = { Text(presetName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    dnsDropdownExpanded = false
                                    dnsTestResult = null
                                    scope.launch { settingsStore.saveGoDns(presetKey, goDnsCustomInput, goDnsDohCustomInput) }
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = goDnsPreset == "custom",
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = goDnsCustomInput,
                        onValueChange = {
                            goDnsCustomInput = it
                            dnsTestResult = null
                            scope.launch { settingsStore.saveGoDns(goDnsPreset, it, goDnsDohCustomInput) }
                        },
                        label = { Text("IP адрес кастомного UDP DNS") },
                        placeholder = { Text("например: 77.88.8.8,1.1.1.1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                AnimatedVisibility(
                    visible = goDnsPreset == "doh-custom",
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = goDnsDohCustomInput,
                        onValueChange = {
                            goDnsDohCustomInput = it
                            dnsTestResult = null
                            scope.launch { settingsStore.saveGoDns(goDnsPreset, goDnsCustomInput, it) }
                        },
                        label = { Text("URL кастомного DoH (HTTPS)") },
                        placeholder = { Text("https://dns.google/dns-query") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                val canCheckDns = !dnsTesting && when (goDnsPreset) {
                    "custom" -> goDnsCustomInput.isNotBlank()
                    "doh-custom" -> goDnsDohCustomInput.isNotBlank()
                    else -> true
                }

                Button(
                    onClick = {
                        if (!canCheckDns) return@Button
                        scope.launch {
                            dnsTesting = true
                            dnsTestResult = null
                            try {
                                val res = withContext(Dispatchers.IO) {
                                    GoDnsProbe.checkPreset(
                                        preset = goDnsPreset,
                                        customRaw = goDnsCustomInput,
                                        dohCustomRaw = goDnsDohCustomInput
                                    )
                                }
                                dnsTestResult = if (res.reachable) {
                                    "✓ Соединение стабильно (${res.okHosts.firstOrNull() ?: "OK"})"
                                } else {
                                    "✕ Сервер недоступен"
                                }
                            } catch (e: Exception) {
                                dnsTestResult = "✕ Ошибка проверки"
                            } finally {
                                dnsTesting = false
                            }
                        }
                    },
                    enabled = canCheckDns,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    if (dnsTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Пинг DNS-сервера...")
                    } else {
                        Icon(Icons.Default.Verified, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Проверить доступность DNS")
                    }
                }

                AnimatedVisibility(
                    visible = dnsTestResult != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    dnsTestResult?.let { text ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (text.startsWith("✓")) Color(0xFF43A047).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (text.startsWith("✓")) Color(0xFF43A047) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // ═══ 3. SOCKS5 Прокси ═══
        AppSectionCard {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "Локальный SOCKS5 Прокси",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

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
                    label = { Text("Порт SOCKS5 (по умолчанию 1080)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { settingsStore.saveSocksAuthEnabled(!socksAuthEnabled) }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                "Требовать авторизацию",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Защита прокси логином и паролем",
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
                }

                AnimatedVisibility(
                    visible = socksAuthEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = socksUsernameInput,
                            onValueChange = { value ->
                                if (value.toByteArray().size <= 255) {
                                    socksUsernameInput = value
                                    scope.launch { settingsStore.saveSocksUsername(value) }
                                }
                            },
                            label = { Text("Логин SOCKS5") },
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
                modifier = Modifier
                    .padding(16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Транспорт и Обфускация",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Маскировка RTP пакетов", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = obfsMode == "audio",
                            onClick = { scope.launch { settingsStore.saveObfsMode("audio") } },
                            label = { Text("🎧 Аудио (OPUS)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = obfsMode == "video",
                            onClick = { scope.launch { settingsStore.saveObfsMode("video") } },
                            label = { Text("🎬 Видео (H.264)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Сетевой протокол", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !turnTcpEnabled,
                            onClick = { scope.launch { settingsStore.saveTurnTcpEnabled(false) } },
                            label = { Text("⚡ UDP (Быстрый)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = turnTcpEnabled,
                            onClick = { scope.launch { settingsStore.saveTurnTcpEnabled(true) } },
                            label = { Text("🔒 TCP / TURN-TCP") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Switch cards
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { settingsStore.saveNoDtlsEnabled(!noDtlsEnabled) } }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Обфускация No-DTLS (AEAD)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Защита от глубокого анализа пакетов (DPI)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = noDtlsEnabled,
                            onCheckedChange = { enabled -> scope.launch { settingsStore.saveNoDtlsEnabled(enabled) } }
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { settingsStore.saveDetailedLogs(!detailedLogs) } }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Подробные логи диагностирования", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Запись отладочной информации пакетов", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = detailedLogs,
                            onCheckedChange = { enabled -> scope.launch { settingsStore.saveDetailedLogs(enabled) } }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ThemePreviewCard(
    paletteId: String,
    title: String,
    primaryColor: Color,
    secondaryColor: Color,
    selectedId: String,
    onClick: (String) -> Unit
) {
    val isSelected = paletteId == selectedId
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Surface(
        onClick = { onClick(paletteId) },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier
            .width(88.dp)
            .height(68.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(secondaryColor)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
