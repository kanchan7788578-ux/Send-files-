package com.example.shared.transfer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.shared.data.TransferRecordEntity
import com.example.shared.model.TransferState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket

class TransferServer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onFileReceived: suspend (TransferRecordEntity) -> Unit
) {
    companion object {
        const val DEFAULT_PORT = 58920
        private const val BUFFER_SIZE = 256 * 1024
        private val MAGIC = byteArrayOf('B'.code.toByte(), 'M'.code.toByte(), 'T'.code.toByte(), 'V'.code.toByte())
    }

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val _boundPort = MutableStateFlow(DEFAULT_PORT)
    val boundPort: StateFlow<Int> = _boundPort.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _receiveState = MutableStateFlow<TransferState>(TransferState.Idle)
    val receiveState: StateFlow<TransferState> = _receiveState.asStateFlow()

    fun start(): Int {
        if (_isRunning.value && serverSocket != null && !serverSocket!!.isClosed) {
            return _boundPort.value
        }
        stop()

        val socket = try {
            ServerSocket(DEFAULT_PORT).apply {
                reuseAddress = true
                receiveBufferSize = 512 * 1024
            }
        } catch (_: Exception) {
            ServerSocket(0).apply {
                reuseAddress = true
                receiveBufferSize = 512 * 1024
            }
        }
        serverSocket = socket
        _boundPort.value = socket.localPort
        _isRunning.value = true

        acceptJob = scope.launch(Dispatchers.IO) {
            while (isActive && !socket.isClosed) {
                try {
                    val client = socket.accept()
                    handleClient(client)
                } catch (e: Exception) {
                    if (!socket.isClosed) {
                        Log.e("TransferServer", "Accept error", e)
                    }
                }
            }
        }
        return socket.localPort
    }

    private suspend fun handleClient(socket: Socket) {
        socket.use { client ->
            try {
                client.tcpNoDelay = true
                client.receiveBufferSize = 512 * 1024
                client.soTimeout = 30_000

                val input = DataInputStream(BufferedInputStream(client.getInputStream(), BUFFER_SIZE))
                val output = DataOutputStream(BufferedOutputStream(client.getOutputStream()))

                val magicBuf = ByteArray(4)
                input.readFully(magicBuf)
                if (!magicBuf.contentEquals(MAGIC)) return

                val headerLen = input.readInt()
                if (headerLen <= 0 || headerLen > 64 * 1024) return
                val headerBytes = ByteArray(headerLen)
                input.readFully(headerBytes)

                val meta = JSONObject(String(headerBytes, Charsets.UTF_8))
                val rawFileName = meta.optString("fileName", "video_${System.currentTimeMillis()}.mp4")
                val safeName = sanitizeFileName(rawFileName)
                val expectedSize = meta.optLong("size", 0L)
                val mimeType = meta.optString("mimeType", "video/mp4")
                val senderName = meta.optString("senderName", "Mobile")
                val fileIndex = meta.optInt("fileIndex", 1)
                val totalFiles = meta.optInt("totalFiles", 1)
                val isVideo = mimeType.startsWith("video/") ||
                    safeName.endsWith(".mp4", true) ||
                    safeName.endsWith(".mkv", true) ||
                    safeName.endsWith(".mov", true) ||
                    safeName.endsWith(".webm", true)

                val receivedDir = File(context.filesDir, "received_media").apply { mkdirs() }
                val destFile = resolveUniqueFile(receivedDir, safeName)

                val buffer = ByteArray(BUFFER_SIZE)
                var receivedBytes = 0L
                val startTime = System.currentTimeMillis()
                var lastEmitTime = startTime
                var lastEmitBytes = 0L

                FileOutputStream(destFile).buffered(BUFFER_SIZE).use { fos ->
                    while (receivedBytes < expectedSize) {
                        val toRead = minOf(buffer.size.toLong(), expectedSize - receivedBytes).toInt()
                        val read = input.read(buffer, 0, toRead)
                        if (read == -1) break
                        fos.write(buffer, 0, read)
                        receivedBytes += read

                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime >= 180L || receivedBytes == expectedSize) {
                            val deltaBytes = receivedBytes - lastEmitBytes
                            val deltaSec = ((now - lastEmitTime).coerceAtLeast(1L)) / 1000.0
                            val speed = (deltaBytes / deltaSec).toLong()
                            _receiveState.value = TransferState.Transferring(
                                peerName = senderName,
                                currentFileName = destFile.name,
                                currentFileIndex = fileIndex,
                                totalFiles = totalFiles,
                                bytesTransferred = receivedBytes,
                                totalBytes = expectedSize,
                                speedBytesPerSec = speed,
                                isSending = false
                            )
                            lastEmitTime = now
                            lastEmitBytes = receivedBytes
                        }
                    }
                    fos.flush()
                }

                if (receivedBytes == expectedSize) {
                    output.writeByte('O'.code)
                    output.writeByte('K'.code)
                    output.flush()

                    val totalElapsedSec = ((System.currentTimeMillis() - startTime).coerceAtLeast(1L)) / 1000.0
                    val avgSpeed = (receivedBytes / totalElapsedSec).toLong()

                    val record = TransferRecordEntity(
                        fileName = destFile.name,
                        filePathOrUri = Uri.fromFile(destFile).toString(),
                        mimeType = mimeType,
                        sizeBytes = receivedBytes,
                        direction = "RECEIVED",
                        peerName = senderName,
                        averageSpeedBytesPerSec = avgSpeed,
                        timestampMs = System.currentTimeMillis(),
                        isVideo = isVideo
                    )
                    onFileReceived(record)

                    if (fileIndex >= totalFiles) {
                        _receiveState.value = TransferState.Completed(
                            peerName = senderName,
                            fileCount = totalFiles,
                            totalBytes = receivedBytes,
                            averageSpeedBytesPerSec = avgSpeed,
                            isSending = false
                        )
                    }
                } else {
                    destFile.delete()
                    _receiveState.value = TransferState.Error("Incomplete transfer ($receivedBytes / $expectedSize)")
                }
            } catch (e: Exception) {
                Log.e("TransferServer", "Receive error", e)
                _receiveState.value = TransferState.Error(e.message ?: "Receive failed")
            }
        }
    }

    fun resetState() {
        _receiveState.value = TransferState.Idle
    }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        _isRunning.value = false
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._\\- ()]"), "_").ifBlank { "media_${System.currentTimeMillis()}.mp4" }
    }

    private fun resolveUniqueFile(dir: File, name: String): File {
        var candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var count = 1
        while (candidate.exists()) {
            candidate = File(dir, "${base}_$count$ext")
            count++
        }
        return candidate
    }
}
