package com.example

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.shared.data.TransferRecordEntity
import com.example.shared.model.DiscoveredTvDevice
import com.example.shared.model.DiscoverySource
import com.example.shared.model.PairingPayload
import com.example.shared.model.QueuedMediaItem
import com.example.shared.model.TransferState
import com.example.shared.transfer.NetworkUtils
import com.example.shared.transfer.TransferClient
import com.example.shared.transfer.TransferServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BeamTransferUnitTest {

    @Test
    fun qrPayloadRoundTrip_parsesCorrectly() {
        val original = PairingPayload(
            ip = "192.168.43.105",
            port = 58920,
            deviceName = "Living Room Android TV",
            pin = "7391",
            wifiSsid = "DIRECT-TV-Beam",
            wifiPass = "pass1234"
        )
        val raw = original.toQrJson()
        val parsed = PairingPayload.fromQrRaw(raw)

        assertNotNull(parsed)
        assertEquals(original.ip, parsed?.ip)
        assertEquals(original.port, parsed?.port)
        assertEquals(original.deviceName, parsed?.deviceName)
        assertEquals(original.pin, parsed?.pin)
        assertEquals(original.wifiSsid, parsed?.wifiSsid)
        assertEquals(original.wifiPass, parsed?.wifiPass)
    }

    @Test
    fun qrPayloadInvalidString_returnsNull() {
        assertNull(PairingPayload.fromQrRaw("https://example.com/not-beamtv"))
        assertNull(PairingPayload.fromQrRaw("random text"))
    }

    @Test
    fun networkUtilsFormatting_isAccurate() {
        assertEquals("512 B", NetworkUtils.formatBytes(512))
        assertEquals("2.0 MB", NetworkUtils.formatBytes(2L * 1024L * 1024L))
        assertEquals("10.0", NetworkUtils.formatSpeedMbps(10L * 1024L * 1024L))
    }

    @Test
    fun tcpSocketTransfer_streamsVideoPayloadEndToEnd() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.app.Application>()
            val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            var receivedRecord: TransferRecordEntity? = null

            val server = TransferServer(context, serverScope) { record ->
                receivedRecord = record
            }
            val port = server.start()

            val sourceFile = File(context.cacheDir, "sample_clip.mp4")
            val payloadBytes = ByteArray(128 * 1024) { (it % 251).toByte() }
            sourceFile.writeBytes(payloadBytes)

            val item = QueuedMediaItem(
                id = "test-1",
                uriString = Uri.fromFile(sourceFile).toString(),
                name = "sample_clip.mp4",
                sizeBytes = payloadBytes.size.toLong(),
                mimeType = "video/mp4",
                isVideo = true
            )

            val target = DiscoveredTvDevice(
                id = "loopback",
                name = "Test TV",
                ip = "127.0.0.1",
                port = port,
                source = DiscoverySource.MANUAL
            )

            var sentRecord: TransferRecordEntity? = null
            val client = TransferClient(context)
            client.sendFiles(target, listOf(item)) { record ->
                sentRecord = record
            }

            assertTrue(client.sendState.value is TransferState.Completed)
            assertNotNull(sentRecord)
            assertNotNull(receivedRecord)
            assertEquals(payloadBytes.size.toLong(), receivedRecord?.sizeBytes)

            val savedFile = File(Uri.parse(receivedRecord!!.filePathOrUri).path!!)
            assertTrue(savedFile.exists())
            assertArrayEquals(payloadBytes, savedFile.readBytes())

            server.stop()
            serverScope.cancel()
            sourceFile.delete()
            savedFile.delete()
        }
    }
}
