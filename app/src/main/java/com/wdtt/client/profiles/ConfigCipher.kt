package com.wdtt.client

import android.util.Base64
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

object ConfigCipher {
    const val SECURE_KEY = "rtptun_secure_shield_key_2026"

    fun xorCipher(inputBytes: ByteArray, key: String = SECURE_KEY): ByteArray {
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        val result = ByteArray(inputBytes.size)
        for (i in inputBytes.indices) {
            result[i] = (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return result
    }

    fun encryptProfileToBlob(profile: ConnectionProfile, countryCode: String = "XX", expiresAt: Long = 0L): String {
        val host = PeerAddress.host(profile.peer)
        val port = PeerAddress.port(profile.peer) ?: 56000
        val raw = "$host|$port|${profile.password}|${profile.vkHashes}|${profile.name}|$countryCode|${profile.workersPerHash}|$expiresAt"
        val encrypted = xorCipher(raw.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decryptBlobToProfile(blobText: String): ConnectionProfile? {
        val trimmed = blobText.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val decoded = Base64.decode(trimmed, Base64.DEFAULT)
            val decryptedBytes = xorCipher(decoded, SECURE_KEY)
            val raw = String(decryptedBytes, Charsets.UTF_8).trim()
            val parts = if (raw.contains('|')) raw.split('|') else raw.split(Regex("[:;,]"))
            if (parts.size >= 3) {
                val host = parts[0].trim()
                val port = parts[1].toIntOrNull() ?: 56000
                val pass = parts[2].trim()
                val hashes = parts.getOrNull(3)?.trim() ?: ""
                val rawName = parts.getOrNull(4)?.trim() ?: ""
                val country = parts.getOrNull(5)?.trim() ?: "XX"
                val flag = getFlagEmoji(country)
                val displayName = when {
                    rawName.isNotBlank() -> rawName
                    flag.isNotEmpty() -> "$flag Сервер $host"
                    else -> "Сервер $host"
                }
                val workers = parts.getOrNull(6)?.toIntOrNull()?.coerceIn(1, 108) ?: 18
                if (host.isBlank() || pass.isBlank()) return null

                ConnectionProfile(
                    id = UUID.randomUUID().toString(),
                    name = displayName,
                    peer = "$host:$port",
                    vkHashes = hashes,
                    workersPerHash = workers,
                    listenPort = 9000,
                    password = pass,
                    useGlobalHashes = hashes.isBlank(),
                    isReadOnly = true
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun parseSingleLineConfig(rawLine: String): ConnectionProfile? {
        var line = rawLine.trim()
        if (line.isEmpty()) return null

        var fragmentName: String? = null
        if (line.contains('#')) {
            runCatching {
                fragmentName = URLDecoder.decode(line.substringAfter('#'), "UTF-8").trim()
            }
            line = line.substringBefore('#')
        }

        // 0. Deep Link: rtptun://import?data=... or https://rtptun.com/import?data=...
        if (line.startsWith("rtptun://import", ignoreCase = true) || line.contains("/import?data=", ignoreCase = true)) {
            val uri = android.net.Uri.parse(line)
            val dataParam = uri.getQueryParameter("data") ?: ""
            if (dataParam.isNotBlank()) {
                return parseSingleLineConfig(dataParam)
            }
        }

        // 1. XOR Decryption
        decryptBlobToProfile(line)?.let { return it }

        // 2. URI with Query Params: ptvb://config?, rtptun://config?, wdtt://config?, qwdtt://config?
        if (line.contains("://config") || line.contains(":config")) {
            try {
                val normalized = line
                    .replace("ptvb:config", "ptvb://config")
                    .replace("rtptun:config", "rtptun://config")
                    .replace("wdtt:config", "wdtt://config")
                    .replace("qwdtt:config", "qwdtt://config")
                val uri = android.net.Uri.parse(normalized)
                val name = fragmentName ?: uri.getQueryParameter("name") ?: "Профиль"
                val peerRaw = uri.getQueryParameter("peer") ?: return null
                val dtlsPortParam = uri.getQueryParameter("dtls_port") ?: uri.getQueryParameter("server_port")
                val peer = if (dtlsPortParam != null) {
                    PeerAddress.ensurePort(peerRaw, dtlsPortParam.toIntOrNull()?.coerceIn(1, 65535) ?: 56000)
                } else {
                    PeerAddress.ensurePort(peerRaw, 56000)
                }
                val hashes = uri.getQueryParameter("hashes") ?: uri.getQueryParameter("vkHashes") ?: ""
                val workers = uri.getQueryParameter("workers")?.toIntOrNull() ?: uri.getQueryParameter("workersPerHash")?.toIntOrNull() ?: 18
                val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 9000
                val pass = uri.getQueryParameter("pass") ?: uri.getQueryParameter("password") ?: ""
                return ConnectionProfile(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    peer = peer,
                    vkHashes = hashes,
                    workersPerHash = workers,
                    listenPort = port,
                    password = pass,
                    useGlobalHashes = hashes.isBlank()
                )
            } catch (_: Exception) {}
        }

        // 3. Colon-Separated URI: ptvb://185.22.15.10:56000:wg_port:local_port:password:hash1,hash2
        if (line.startsWith("ptvb://") || line.startsWith("wdtt://") || line.startsWith("qwdtt://") || line.startsWith("rtptun://")) {
            try {
                val schemePrefix = line.substringBefore("://") + "://"
                val parts = line.removePrefix(schemePrefix).split(":")
                if (parts.size >= 5) {
                    val ip = parts[0]
                    val dtlsPort = parts[1]
                    val localPort = parts.getOrNull(3)?.toIntOrNull() ?: 9000
                    val pass = parts[4]
                    val hash = parts.drop(5).joinToString(":")
                    val name = fragmentName ?: "RTpTUN $ip"
                    return ConnectionProfile(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        peer = "$ip:$dtlsPort",
                        vkHashes = hash,
                        workersPerHash = 18,
                        listenPort = localPort,
                        password = pass,
                        useGlobalHashes = hash.isBlank()
                    )
                }
            } catch (_: Exception) {}
        }

        return null
    }

    fun parseMultipleLines(rawText: String): List<ConnectionProfile> {
        val result = mutableListOf<ConnectionProfile>()
        rawText.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            parseSingleLineConfig(line)?.let { profile ->
                result.add(profile)
            }
        }
        return result
    }

    private fun getFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2 || countryCode.equals("XX", ignoreCase = true)) return ""
        val upper = countryCode.uppercase()
        val firstLetter = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }
}
