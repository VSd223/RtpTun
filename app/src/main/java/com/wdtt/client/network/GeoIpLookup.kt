package com.wdtt.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object GeoIpLookup {
    private val ipRanges = listOf(
        // Germany (DE)
        "185.22." to "DE", "185.220." to "DE", "188.165." to "DE", "178.63." to "DE", "138.201." to "DE", "31.172." to "DE", "185.243." to "DE", "194.36." to "DE",
        // Netherlands (NL)
        "185.107." to "NL", "213.108." to "NL", "45.138." to "NL", "185.182." to "NL", "31.220." to "NL", "185.246." to "NL", "185.102." to "NL", "193.38." to "NL",
        // Finland (FI)
        "95.216." to "FI", "65.108." to "FI", "65.109." to "FI", "37.27." to "FI", "135.181." to "FI", "185.228." to "FI",
        // Russia (RU)
        "185.178." to "RU", "185.80." to "RU", "91.200." to "RU", "194.58." to "RU", "193.124." to "RU", "31.76." to "RU", "185.177." to "RU", "188.120." to "RU",
        // USA (US)
        "104.16." to "US", "104.28." to "US", "198.51." to "US", "140.82." to "US", "143.198." to "US", "172.67." to "US", "104.21." to "US",
        // France (FR)
        "51.15." to "FR", "163.172." to "FR", "51.254." to "FR", "162.19." to "FR", "185.207." to "FR",
        // UK (GB)
        "51.89." to "GB", "178.62." to "GB", "138.68." to "GB", "185.216." to "GB",
        // Sweden (SE)
        "185.213." to "SE", "89.238." to "SE", "185.189." to "SE", "193.180." to "SE",
        // Turkey (TR)
        "185.255." to "TR", "185.126." to "TR", "185.110." to "TR",
        // Kazakhstan (KZ)
        "185.100." to "KZ", "185.120." to "KZ", "2.72." to "KZ"
    )

    fun resolveCountryCode(peer: String, name: String = ""): String {
        val n = name.lowercase()
        when {
            n.contains("германия") || n.contains("germany") || n.contains(" de ") || n.endsWith("de") -> return "DE"
            n.contains("нидерланд") || n.contains("netherlands") || n.contains(" nl ") || n.endsWith("nl") -> return "NL"
            n.contains("финлянди") || n.contains("finland") || n.contains(" fi ") || n.endsWith("fi") -> return "FI"
            n.contains("россия") || n.contains("russia") || n.contains(" ru ") || n.endsWith("ru") -> return "RU"
            n.contains("сша") || n.contains("usa") || n.contains(" us ") || n.endsWith("us") -> return "US"
            n.contains("франци") || n.contains("france") || n.contains(" fr ") -> return "FR"
            n.contains("швеци") || n.contains("sweden") || n.contains(" se ") -> return "SE"
            n.contains("турци") || n.contains("turkey") || n.contains(" tr ") -> return "TR"
            n.contains("казахстан") || n.contains("kazakhstan") || n.contains(" kz ") -> return "KZ"
        }

        val host = PeerAddress.host(peer).trim()
        for ((prefix, code) in ipRanges) {
            if (host.startsWith(prefix)) return code
        }
        return "XX"
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
    val code = GeoIpLookup.resolveCountryCode(peer, name)
    val flag = GeoIpLookup.countryFlagEmoji(code)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(flag, fontSize = 13.sp)
            Text(
                text = code.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
