package com.wdtt.client

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object GeoIpLookup {
    private val memoryCache = ConcurrentHashMap<String, String>()

    // Быстрое определение по названию, чтобы флаг появлялся мгновенно, если страна указана в имени
    fun guessCountryCodeFromName(name: String): String? {
        val n = name.lowercase()
        return when {
            n.contains("германия") || n.contains("germany") || n.contains(" de ") || n.endsWith("de") -> "DE"
            n.contains("нидерланд") || n.contains("netherlands") || n.contains(" nl ") || n.endsWith("nl") -> "NL"
            n.contains("финлянди") || n.contains("finland") || n.contains(" fi ") || n.endsWith("fi") -> "FI"
            n.contains("россия") || n.contains("russia") || n.contains(" ru ") || n.endsWith("ru") -> "RU"
            n.contains("сша") || n.contains("usa") || n.contains(" us ") || n.endsWith("us") -> "US"
            n.contains("франци") || n.contains("france") || n.contains(" fr ") -> "FR"
            n.contains("швеци") || n.contains("sweden") || n.contains(" se ") -> "SE"
            n.contains("турци") || n.contains("turkey") || n.contains(" tr ") -> "TR"
            n.contains("казахстан") || n.contains("kazakhstan") || n.contains(" kz ") -> "KZ"
            n.contains("польша") || n.contains("poland") || n.contains(" pl ") -> "PL"
            n.contains("украина") || n.contains("ukraine") || n.contains(" ua ") -> "UA"
            n.contains("беларусь") || n.contains("belarus") || n.contains(" by ") -> "BY"
            else -> null
        }
    }

    suspend fun resolveCountryCodeAsync(context: Context, peer: String, name: String): String {
        // 1. Проверяем по имени (быстро)
        guessCountryCodeFromName(name)?.let { return it }

        val host = PeerAddress.host(peer).trim()
        if (host.isEmpty()) return "XX"

        // 2. Проверяем кэш в памяти
        memoryCache[host]?.let { return it }

        // 3. Проверяем постоянный кэш
        val prefs = context.getSharedPreferences("geoip_cache", Context.MODE_PRIVATE)
        prefs.getString(host, null)?.let {
            memoryCache[host] = it
            return it
        }

        // 4. Запрашиваем через API
        return withContext(Dispatchers.IO) {
            try {
                // Игнорируем локальные IP
                if (host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("127.")) {
                    return@withContext "XX"
                }

                val url = URL("https://api.country.is/$host")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val country = json.optString("country", "XX").uppercase()
                    
                    if (country != "XX" && country.isNotEmpty()) {
                        memoryCache[host] = country
                        prefs.edit().putString(host, country).apply()
                        return@withContext country
                    }
                }
            } catch (e: Exception) {
                Log.e("GeoIpLookup", "Failed to resolve IP $host: ${e.message}")
            }
            "XX"
        }
    }

    fun countryFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2 || countryCode.equals("XX", ignoreCase = true)) return "🌐"
        val upper = countryCode.uppercase()
        val firstLetter = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }
}

@Composable
fun CountryFlagBadge(peer: String, name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // Сначала ставим флаг из имени или "глобус"
    var code by remember(peer, name) { 
        mutableStateOf(GeoIpLookup.guessCountryCodeFromName(name) ?: "XX") 
    }

    // Асинхронно запрашиваем по IP (если закэшировано - вернет сразу, иначе скачает не блокируя UI)
    LaunchedEffect(peer, name) {
        val resolved = GeoIpLookup.resolveCountryCodeAsync(context, peer, name)
        if (resolved != "XX") {
            code = resolved
        }
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        if (code == "XX") {
            Text(
                text = "🌐",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        } else {
            coil.compose.AsyncImage(
                model = "https://flagcdn.com/w40/${code.lowercase()}.png",
                contentDescription = code,
                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
    }
}
