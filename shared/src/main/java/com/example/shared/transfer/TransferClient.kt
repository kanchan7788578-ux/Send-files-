package com.example.shared.transfer

import android.content.Context
import android.net.Uri
import android.os.Build
import com.example.shared.data.TransferRecordEntity
import com.example.shared.model.DiscoveredTvDevice
import com.example.shared.model.QueuedMediaItem
import com.example.shared.model.TransferState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

class TransferClient(private val context: Context) {

    companion object {
        private const val BUFFER_SIZE = 256 * 1024
        private val MAGIC = byteArrayOf('B'.code.toByte(), 'M'.code.toByte(), 'T'.code.toByte(), 'V'.code.toByte())
    }

    private val _sendState = MutableStateFlow<TransferState>(TransferState.Idle)
    val sendState: StateFlow<TransferState> = _sendState.asStateFlow()

    suspend fun sendFiles(
        target: DiscoveredTvDevice,
        items: List<QueuedMediaItem>,
        onRecordSaved: suspend (TransferRecordEntity) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        val totalBatchBytes = items.sumOf { it.sizeBytes.coerceAtLeast(1L) }
        var batchBytesSent = 0L
        val batchStartTime = System.currentTimeMillis()
        val senderName = Build.MODEL ?: "Mobile"

        _sendState.value = TransferState.Connecting(target.name, target.ip)

        try {
            items.forEachIndexed { index, item ->
                currentCoroutineContext().ensureActive()
                val uri = Uri.parse(item.uriString)
                val fileSize = item.sizeBytes.takeIf { it > 0L }
                    ?: NetworkUtils.resolveMediaItem(context, uri).sizeBytes

                Socket().use { socket ->
                    socket.tcpNoDelay = true
                    socket.sendBufferSize = 512 * 1024
                    socket.connect(InetSocketAddress(target.ip, target.port), 6000)
                    socket.soTimeout = 30_000

                    val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE))
                    val input = DataInputStream(BufferedInputStream(socket.getInputStream()))

                    val headerJson = JSONObject().apply {
                        put("fileName", item.name)
                        put("size", fileSize)
                        put("mimeType", item.mimeType)
                        put("senderName", senderName)
                        put("fileIndex", index + 1)
                        put("totalFiles", items.size)
                    }.toString().toByteArray(Charsets.UTF_8)

                    output.write(MAGIC)
                    output.writeInt(headerJson.size)
                    output.write(headerJson)

                    val buffer = ByteArray(BUFFER_SIZE)
                    var fileBytesSent = 0L
                    var lastEmitTime = System.currentTimeMillis()
                    var lastEmitBytes = 0L
                    val fileStartTime = lastEmitTime

                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Cannot open file: ${item.name}")

                    inputStream.use { stream ->
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = stream.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            fileBytesSent += read
                            batchBytesSent += read

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 160L || fileBytesSent >= fileSize) {
                                val deltaBytes = fileBytesSent - lastEmitBytes
                                val deltaSec = ((now - lastEmitTime).coerceAtLeast(1L)) / 1000.0
                                val speed = (deltaBytes / deltaSec).toLong()
                                _sendState.value = TransferState.Transferring(
                                    peerName = target.name,
                                    currentFileName = item.name,
                                    currentFileIndex = index + 1,
                                    totalFiles = items.size,
                                    bytesTransferred = batchBytesSent,
                                    totalBytes = maxOf(totalBatchBytes, batchBytesSent),
                                    speedBytesPerSec = speed,
                                    isSending = true
                                )
                                lastEmitTime = now
                                lastEmitBytes = fileBytesSent
                            }
                        }
                    }
                    output.flush()

                    val ack1 = input.readByte().toInt().toChar()
                    val ack2 = input.readByte().toInt().toChar()
                    if (ack1 != 'O' || ack2 != 'K') {
                        throw IllegalStateException("TV did not acknowledge receipt of ${item.name}")
                    }

                    val fileElapsedSec = ((System.currentTimeMillis() - fileStartTime).coerceAtLeast(1L)) / 1000.0
                    val fileAvgSpeed = (fileBytesSent / fileElapsedSec).toLong()

                    onRecordSaved(
                        TransferRecordEntity(
                            fileName = item.name,
                            filePathOrUri = item.uriString,
                            mimeType = item.mimeType,
                            sizeBytes = fileBytesSent,
                            direction = "SENT",
                            peerName = target.name,
                            averageSpeedBytesPerSec = fileAvgSpeed,
                            timestampMs = System.currentTimeMillis(),
                            isVideo = item.isVideo
                        )
                    )
                }
            }

            val totalElapsedSec = ((System.currentTimeMillis() - batchStartTime).coerceAtLeast(1L)) / 1000.0
            val avgBatchSpeed = (batchBytesSent / totalElapsedSec).toLong()
            _sendState.value = TransferState.Completed(
                peerName = target.name,
                fileCount = items.size,
                totalBytes = batchBytesSent,
                averageSpeedBytesPerSec = avgBatchSpeed,
                isSending = true
            )
        } catch (ce: CancellationException) {
            _sendState.value = TransferState.Idle
            throw ce
        } catch (e: Exception) {
            _sendState.value = TransferState.Error(e.message ?: "Connection failed")
        }
    }

    fun resetState() {
        _sendState.value = TransferState.Idle
    }
}
