package com.example.shared.transfer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.shared.model.QueuedMediaItem
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import java.util.UUID

object NetworkUtils {

    fun getBestLocalIpv4Address(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            val candidates = mutableListOf<Pair<Int, String>>()

            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val name = intf.name.lowercase(Locale.US)
                val priority = when {
                    name.startsWith("p2p") -> 1
                    name.startsWith("ap") || name.contains("hotspot") || name.startsWith("swlan") -> 2
                    name.startsWith("wlan") -> 3
                    name.startsWith("eth") -> 4
                    else -> 5
                }
                for (addr in intf.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        if (!host.startsWith("169.254.")) {
                            candidates.add(priority to host)
                        }
                    }
                }
            }
            candidates.minByOrNull { it.first }?.second ?: "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    fun formatSpeedMbps(bytesPerSec: Long): String {
        val mbPerSec = bytesPerSec / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f", mbPerSec.coerceAtLeast(0.0))
    }

    fun formatDurationSeconds(seconds: Long): String {
        if (seconds <= 0) return "0s"
        val mins = seconds / 60
        val secs = seconds % 60
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    fun resolveMediaItem(context: Context, uri: Uri): QueuedMediaItem {
        val resolver = context.contentResolver
        var displayName = "video_${System.currentTimeMillis()}.mp4"
        var size = 0L

        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }

        if (size <= 0L) {
            try {
                resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    if (afd.length > 0) size = afd.length
                }
            } catch (_: Exception) {
            }
        }

        val mimeType = resolver.getType(uri) ?: guessMimeFromName(displayName)
        val isVideo = mimeType.startsWith("video/") ||
            displayName.endsWith(".mp4", true) ||
            displayName.endsWith(".mkv", true) ||
            displayName.endsWith(".mov", true) ||
            displayName.endsWith(".webm", true) ||
            displayName.endsWith(".avi", true)

        return QueuedMediaItem(
            id = UUID.randomUUID().toString(),
            uriString = uri.toString(),
            name = displayName,
            sizeBytes = size,
            mimeType = mimeType,
            isVideo = isVideo
        )
    }

    private fun guessMimeFromName(name: String): String {
        return when {
            name.endsWith(".mp4", true) -> "video/mp4"
            name.endsWith(".mkv", true) -> "video/x-matroska"
            name.endsWith(".webm", true) -> "video/webm"
            name.endsWith(".mov", true) -> "video/quicktime"
            name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
            name.endsWith(".png", true) -> "image/png"
            name.endsWith(".mp3", true) -> "audio/mpeg"
            name.endsWith(".pdf", true) -> "application/pdf"
            else -> "application/octet-stream"
        }
    }
}
