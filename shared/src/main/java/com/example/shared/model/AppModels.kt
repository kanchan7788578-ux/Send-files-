package com.example.shared.model

import android.os.Build
import org.json.JSONObject

enum class AppLanguage {
    BENGALI,
    ENGLISH
}

enum class DiscoverySource {
    QR_SCAN,
    NSD,
    WIFI_DIRECT,
    MANUAL
}

data class QueuedMediaItem(
    val id: String,
    val uriString: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val isVideo: Boolean
)

data class PairingPayload(
    val ip: String,
    val port: Int,
    val deviceName: String,
    val pin: String,
    val wifiSsid: String? = null,
    val wifiPass: String? = null
) {
    fun toQrJson(): String {
        val json = JSONObject().apply {
            put("app", "BeamTV")
            put("v", 1)
            put("ip", ip)
            put("port", port)
            put("name", deviceName)
            put("pin", pin)
            if (!wifiSsid.isNullOrBlank()) put("ssid", wifiSsid)
            if (!wifiPass.isNullOrBlank()) put("pass", wifiPass)
        }
        return json.toString()
    }

    companion object {
        fun fromQrRaw(raw: String): PairingPayload? {
            return try {
                val trimmed = raw.trim()
                if (trimmed.startsWith("{")) {
                    val obj = JSONObject(trimmed)
                    if (obj.optString("app") != "BeamTV" && !obj.has("ip")) return null
                    val ip = obj.optString("ip")
                    val port = obj.optInt("port", 58920)
                    if (ip.isBlank() || port <= 0) return null
                    PairingPayload(
                        ip = ip,
                        port = port,
                        deviceName = obj.optString("name", "Android TV"),
                        pin = obj.optString("pin", ""),
                        wifiSsid = obj.optString("ssid").takeIf { it.isNotBlank() },
                        wifiPass = obj.optString("pass").takeIf { it.isNotBlank() }
                    )
                } else if (trimmed.contains(":")) {
                    val parts = trimmed.split(":")
                    val ip = parts[0]
                    val port = parts.getOrNull(1)?.toIntOrNull() ?: 58920
                    if (ip.count { it == '.' } == 3) {
                        PairingPayload(ip = ip, port = port, deviceName = "TV ($ip)", pin = "")
                    } else null
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class DiscoveredTvDevice(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val source: DiscoverySource,
    val wifiSsid: String? = null,
    val wifiPass: String? = null
)

sealed interface TransferState {
    data object Idle : TransferState

    data class Connecting(val targetName: String, val targetIp: String) : TransferState

    data class Transferring(
        val peerName: String,
        val currentFileName: String,
        val currentFileIndex: Int,
        val totalFiles: Int,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long,
        val isSending: Boolean
    ) : TransferState {
        val progress: Float
            get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

        val etaSeconds: Long
            get() {
                if (speedBytesPerSec <= 0L || totalBytes <= bytesTransferred) return 0L
                return (totalBytes - bytesTransferred) / speedBytesPerSec
            }
    }

    data class Completed(
        val peerName: String,
        val fileCount: Int,
        val totalBytes: Long,
        val averageSpeedBytesPerSec: Long,
        val isSending: Boolean
    ) : TransferState

    data class Error(val message: String) : TransferState
}

data class TvServerInfo(
    val isRunning: Boolean = false,
    val localIp: String = "0.0.0.0",
    val port: Int = 58920,
    val deviceName: String = Build.MODEL ?: "BeamTV TV",
    val pin: String = "4829",
    val wifiDirectActive: Boolean = false,
    val wifiDirectSsid: String? = null,
    val wifiDirectPass: String? = null,
    val wifiDirectError: String? = null
) {
    fun toPairingPayload(): PairingPayload = PairingPayload(
        ip = localIp,
        port = port,
        deviceName = deviceName,
        pin = pin,
        wifiSsid = wifiDirectSsid,
        wifiPass = wifiDirectPass
    )
}
