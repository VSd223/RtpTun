package com.wdtt.client.ui
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import android.widget.Toast
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wdtt.client.ProfileSubscription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatTrafficMb(value: Double): String {
    return when {
        value >= 1024 -> String.format(Locale.US, "%.2f ГБ", value / 1024.0)
        value >= 1 -> String.format(Locale.US, "%.1f МБ", value)
        value > 0 -> String.format(Locale.US, "%.2f МБ", value)
        else -> "0 МБ"
    }
}

private fun formatSyncTime(ts: Long): String {
    if (ts <= 0L) return "ещё не обновлялась"
    return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(ts))
}

@Composable
fun AddSubscriptionDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (url: String) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Новая подписка", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Адрес подписки") },
                    placeholder = { Text("https://example.com/sub.json") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving
                )
                Text(
                    "Вставьте ссылку — название и профили загрузятся автоматически.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(urlInput) },
                enabled = !saving && urlInput.isNotBlank()
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Добавить")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!saving) onDismiss() }, enabled = !saving) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun DeleteSubscriptionDialog(
    sub: ProfileSubscription,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить подписку?") },
        text = { Text("«${sub.name}» и папка с профилями будут удалены.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun EditSubscriptionDialog(
    sub: ProfileSubscription,
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit
) {
    var nameInput by remember { mutableStateOf(sub.name) }
    var urlInput by remember { mutableStateOf(sub.url) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать подписку", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Название подписки") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Адрес подписки (URL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nameInput, urlInput) },
                enabled = urlInput.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun formatMaskedUrl(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return ""
    return try {
        val uri = java.net.URI(trimmed)
        val host = uri.host ?: return trimmed
        val scheme = uri.scheme ?: "https"
        val path = uri.path ?: ""
        if (path.length > 12) {
            val start = path.take(6)
            val end = path.takeLast(4)
            "$scheme://$host$start...$end"
        } else {
            "$scheme://$host$path"
        }
    } catch (_: Exception) {
        if (trimmed.length > 30) {
            trimmed.take(18) + "..." + trimmed.takeLast(6)
        } else {
            trimmed
        }
    }
}

@Composable
fun AnimatedDotsLoading(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 133, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 266, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF00E676).copy(alpha = dot1Alpha)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF00E676).copy(alpha = dot2Alpha)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF00E676).copy(alpha = dot3Alpha)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionInfoCard(
    sub: ProfileSubscription,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    onEditSub: ((name: String, url: String) -> Unit)? = null,
    isProfilesHidden: Boolean = false,
    onToggleHideProfiles: (() -> Unit)? = null,
    onOpenGroup: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isExpanded = !isProfilesHidden
    var showEditDialog by remember { mutableStateOf(false) }

    val hasLimit = sub.trafficLimitMb > 0
    val hasTraffic = sub.remoteTrafficUsedMb > 0 || hasLimit
    val progress = if (hasLimit && sub.trafficLimitMb > 0) {
        (sub.remoteTrafficUsedMb / sub.trafficLimitMb).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        onClick = { onToggleHideProfiles?.invoke() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.RssFeed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (sub.name.isNotBlank()) sub.name else formatMaskedUrl(sub.url),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isRefreshing) {
                            AnimatedDotsLoading()
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Обновить",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Свернуть" else "Раскрыть",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val context = LocalContext.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatMaskedUrl(sub.url),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = contentColor.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (sub.description.isNotBlank()) {
                        Text(
                            sub.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.85f)
                        )
                    }

                    if (hasTraffic) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (hasLimit) {
                                    "Трафик: ${formatTrafficMb(sub.remoteTrafficUsedMb)} из ${formatTrafficMb(sub.trafficLimitMb)}"
                                } else {
                                    "Трафик: ${formatTrafficMb(sub.remoteTrafficUsedMb)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor,
                                fontWeight = FontWeight.Medium
                            )
                            if (hasLimit) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = contentColor.copy(alpha = 0.25f),
                                )
                            }
                        }
                    }

                    Text(
                        "Обновлено: ${formatSyncTime(sub.lastSyncAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.75f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Subscription URL", sub.url))
                                Toast.makeText(context, "Ссылка подписки скопирована!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать ссылку", tint = contentColor.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                        }
                        if (onEditSub != null) {
                            IconButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = contentColor.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(
                            onClick = { onDelete() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (sub.lastSyncError.isNotBlank()) {
                        Text(
                            "Ошибка: ${sub.lastSyncError}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog && onEditSub != null) {
        EditSubscriptionDialog(
            sub = sub,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, url ->
                showEditDialog = false
                onEditSub(name, url)
            }
        )
    }
}
