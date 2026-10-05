package com.wdtt.client.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wdtt.client.ConnectionPipelineCard
import com.wdtt.client.LogEntry
import com.wdtt.client.TunnelManager
import com.wdtt.client.WDTTColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsTab() {
    val context = LocalContext.current
    val currentLogs by TunnelManager.logs.collectAsStateWithLifecycle()
    val lastFatalError by TunnelManager.lastFatalError.collectAsStateWithLifecycle()
    val statsText by TunnelManager.stats.collectAsStateWithLifecycle()
    val isRunning by TunnelManager.running.collectAsStateWithLifecycle()
    val isConnecting by TunnelManager.isConnecting.collectAsStateWithLifecycle()
    val connectedSinceMs by TunnelManager.connectedSinceMs.collectAsStateWithLifecycle()
    val pipelineState by TunnelManager.connectionPipeline.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var activeFilter by remember { mutableStateOf("all") } // all, errors, net, vk

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isRunning, connectedSinceMs) {
        if (!isRunning || connectedSinceMs <= 0L) return@LaunchedEffect
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }
    val uptimeText = if (isRunning && connectedSinceMs > 0L) {
        TunnelManager.formatUptime(nowMs - connectedSinceMs)
    } else null

    // Самые свежие логи ВВЕРХУ (новые события первыми, без необходимости прокрутки вниз)
    val topLogs = remember(currentLogs) {
        currentLogs.filter { it.key != "stats" }.reversed()
    }

    val filteredLogs = remember(topLogs, activeFilter) {
        when (activeFilter) {
            "errors" -> topLogs.filter { it.isError }
            "net" -> topLogs.filter { it.key.contains("dns", ignoreCase = true) || it.key.contains("conn", ignoreCase = true) || it.key.contains("socks", ignoreCase = true) }
            "vk" -> topLogs.filter { it.key.contains("vk", ignoreCase = true) || it.key.contains("captcha", ignoreCase = true) }
            else -> topLogs
        }
    }

    val pinnedStatsMessage = remember(statsText, isRunning, isConnecting, currentLogs) {
        val fromLog = currentLogs.firstOrNull { it.key == "stats" }?.message
        val message = when {
            !fromLog.isNullOrBlank() -> fromLog
            (isRunning || isConnecting) && statsText.isNotBlank() -> "[СТАТИСТИКА] $statsText"
            else -> null
        }
        message?.replace(
            Regex("""\s*\|\s*↓\s*[\d.,]+\s*МБ\s*/\s*↑\s*[\d.,]+\s*МБ"""),
            ""
        )
    }

    val isDark = isSystemInDarkTheme()
    val terminalBg = if (isDark) WDTTColors.terminalBgDark else WDTTColors.terminalBg

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ═══ Верхняя панель заголовка ═══
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Лог событий (Свежие вверху)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        isRunning -> "🟢 Туннель активен"
                        isConnecting -> "⏳ Идёт подключение..."
                        lastFatalError != null -> "🔴 Ошибка подключения"
                        else -> "⚪ Готов к работе"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isRunning -> Color(0xFF00E676)
                        isConnecting -> MaterialTheme.colorScheme.primary
                        lastFatalError != null -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row {
                IconButton(onClick = { TunnelManager.clearLogs() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = {
                    val text = buildString {
                        if (pinnedStatsMessage != null) appendLine(pinnedStatsMessage)
                        topLogs.asReversed().forEach { appendLine("${it.message} (x${it.count})") }
                    }.trim()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("RTpTUN Logs", text))
                    Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = {
                    val uiLogText = buildString {
                        if (pinnedStatsMessage != null) appendLine(pinnedStatsMessage)
                        topLogs.asReversed().forEach { appendLine("${it.message} (x${it.count})") }
                    }.trim()
                    exportFullLog(context, uiLogText)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Export logs", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ═══ Фатальная ошибка (Карточка) ═══
        if (lastFatalError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                    Text(
                        text = lastFatalError.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("RTpTUN Error", lastFatalError.orEmpty()))
                        Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy error", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    IconButton(onClick = { TunnelManager.lastFatalError.value = null }) {
                        Icon(Icons.Default.Delete, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // ═══ Чипы фильтрации логов ═══
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeFilter == "all",
                onClick = { activeFilter = "all" },
                label = { Text("Все (${topLogs.size})") }
            )
            FilterChip(
                selected = activeFilter == "errors",
                onClick = { activeFilter = "errors" },
                label = { Text("⚠️ Ошибки (${topLogs.count { it.isError }})") }
            )
            FilterChip(
                selected = activeFilter == "net",
                onClick = { activeFilter = "net" },
                label = { Text("🌐 Сеть & DNS") }
            )
            FilterChip(
                selected = activeFilter == "vk",
                onClick = { activeFilter = "vk" },
                label = { Text("🔒 VK / Капча") }
            )
        }

        // ═══ Терминал логов ═══
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = terminalBg),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (pinnedStatsMessage != null || uptimeText != null) {
                    Surface(
                        color = WDTTColors.terminalBlue.copy(alpha = if (isDark) 0.18f else 0.12f),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (pinnedStatsMessage != null) {
                                Text(
                                    text = pinnedStatsMessage
                                        .removePrefix("[СТАТИСТИКА] ")
                                        .removePrefix("[СТАТИСТИКА]"),
                                    color = WDTTColors.terminalBlue,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 14.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            if (uptimeText != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = WDTTColors.terminalBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = uptimeText,
                                        color = WDTTColors.terminalBlue,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        color = WDTTColors.terminalBlue.copy(alpha = 0.35f),
                        thickness = 1.dp
                    )
                }

                // Визуальные узлы пайплайна подключения
                AnimatedVisibility(
                    visible = pipelineState.visible || isConnecting || isRunning,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column {
                        ConnectionPipelineCard(
                            state = pipelineState,
                            isDark = isDark,
                        )
                        HorizontalDivider(
                            color = WDTTColors.terminalBlue.copy(alpha = 0.20f),
                            thickness = 1.dp
                        )
                    }
                }

                if (filteredLogs.isEmpty() && pinnedStatsMessage == null && uptimeText == null && !pipelineState.visible) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notes,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Логи пусты",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Здесь будут отображаться свежие события туннеля",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LaunchedEffect(filteredLogs.size) {
                        if (filteredLogs.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(filteredLogs, key = { it.key }) { entry ->
                            LogLine(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogLine(entry: LogEntry) {
    val color = when {
        entry.isError -> WDTTColors.terminalRed
        entry.priority <= 2 -> WDTTColors.terminalGreen
        entry.priority == 3 -> WDTTColors.terminalBlue
        else -> WDTTColors.terminalText
    }

    var trigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(entry.count) { trigger++ }

    val animatedScale by animateFloatAsState(
        targetValue = if (trigger % 2 == 0) 1f else 1.05f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "count_bump"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = entry.message,
            color = color,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (entry.count > 1) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = color.copy(alpha = 0.2f),
                modifier = Modifier.graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
            ) {
                Text(
                    text = "x${entry.count}",
                    color = color,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

fun exportFullLog(context: Context, uiLogText: String) {
    try {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "RTpTUN Diagnostic Logs")
            putExtra(android.content.Intent.EXTRA_TEXT, uiLogText)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Поделиться логом"))
    } catch (_: Exception) {
        Toast.makeText(context, "Ошибка экспорта лога", Toast.LENGTH_SHORT).show()
    }
}
