package com.example.ui

import android.app.Application
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.BeamDatabase
import com.example.shared.data.LanguagePrefsRepository
import com.example.shared.data.TransferRecordEntity
import com.example.shared.model.AppLanguage
import com.example.shared.model.DiscoveredTvDevice
import com.example.shared.model.DiscoverySource
import com.example.shared.model.PairingPayload
import com.example.shared.model.QueuedMediaItem
import com.example.shared.model.TransferState
import com.example.shared.transfer.NetworkUtils
import com.example.shared.transfer.NsdDiscoveryManager
import com.example.shared.transfer.TransferClient
import com.example.shared.transfer.TransferServer
import com.example.shared.transfer.WifiDirectManager
import com.example.shared.transfer.WifiDirectState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MobileViewModel(application: Application) : AndroidViewModel(application) {

    private val langRepo = LanguagePrefsRepository(application)
    private val dao = BeamDatabase.getInstance(application).transferDao()

    private val transferClient = TransferClient(application)
    private val nsdManager = NsdDiscoveryManager(application)
    val wifiDirectManager = WifiDirectManager(application)

    val language: StateFlow<AppLanguage> = langRepo.languageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.BENGALI)

    val historyRecords: StateFlow<List<TransferRecordEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sendState: StateFlow<TransferState> = transferClient.sendState
    val wifiDirectState: StateFlow<WifiDirectState> = wifiDirectManager.state

    private val _selectedQueue = MutableStateFlow<List<QueuedMediaItem>>(emptyList())
    val selectedQueue: StateFlow<List<QueuedMediaItem>> = _selectedQueue.asStateFlow()

    private val _pairedTv = MutableStateFlow<DiscoveredTvDevice?>(null)
    val pairedTv: StateFlow<DiscoveredTvDevice?> = _pairedTv.asStateFlow()

    val discoveredTvs: StateFlow<List<DiscoveredTvDevice>> = combine(
        nsdManager.discoveredDevices,
        wifiDirectManager.state
    ) { nsdList, p2p ->
        val p2pDevices = if (!p2p.isGroupOwner && !p2p.groupOwnerIp.isNullOrBlank()) {
            listOf(
                DiscoveredTvDevice(
                    id = "p2p_${p2p.groupOwnerIp}",
                    name = "Wi-Fi Direct TV",
                    ip = p2p.groupOwnerIp!!,
                    port = TransferServer.DEFAULT_PORT,
                    source = DiscoverySource.WIFI_DIRECT
                )
            )
        } else emptyList()
        (nsdList + p2pDevices).distinctBy { "${it.ip}:${it.port}" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var sendJob: Job? = null

    init {
        wifiDirectManager.registerReceiver()
        nsdManager.startDiscovery()
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (language.value == AppLanguage.BENGALI) AppLanguage.ENGLISH else AppLanguage.BENGALI
            langRepo.setLanguage(next)
        }
    }

    fun refreshDiscovery() {
        nsdManager.startDiscovery()
    }

    fun addMediaUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val resolved = uris.map { uri -> NetworkUtils.resolveMediaItem(app, uri) }
            val existingUris = _selectedQueue.value.map { it.uriString }.toSet()
            val fresh = resolved.filterNot { it.uriString in existingUris }
            _selectedQueue.value = _selectedQueue.value + fresh
        }
    }

    fun removeQueuedItem(id: String) {
        _selectedQueue.value = _selectedQueue.value.filterNot { it.id == id }
    }

    fun clearQueue() {
        _selectedQueue.value = emptyList()
    }

    fun pairFromQrRaw(rawValue: String): Boolean {
        val payload = PairingPayload.fromQrRaw(rawValue) ?: return false
        _pairedTv.value = DiscoveredTvDevice(
            id = "qr_${payload.ip}_${payload.port}",
            name = payload.deviceName,
            ip = payload.ip,
            port = payload.port,
            source = DiscoverySource.QR_SCAN,
            wifiSsid = payload.wifiSsid,
            wifiPass = payload.wifiPass
        )
        return true
    }

    fun selectDiscoveredTv(device: DiscoveredTvDevice) {
        _pairedTv.value = device
    }

    fun connectManualIp(ip: String, port: Int) {
        val cleanIp = ip.trim()
        if (cleanIp.isBlank()) return
        _pairedTv.value = DiscoveredTvDevice(
            id = "manual_${cleanIp}_$port",
            name = "TV ($cleanIp)",
            ip = cleanIp,
            port = port,
            source = DiscoverySource.MANUAL
        )
    }

    fun disconnectTv() {
        _pairedTv.value = null
    }

    fun startSendingQueue() {
        val target = _pairedTv.value ?: return
        val items = _selectedQueue.value
        if (items.isEmpty()) return

        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            transferClient.sendFiles(target, items) { record ->
                dao.insert(record)
            }
        }
    }

    fun cancelSending() {
        sendJob?.cancel()
        sendJob = null
        transferClient.resetState()
    }

    fun resetSendState() {
        transferClient.resetState()
    }

    fun scanWifiDirectPeers() {
        wifiDirectManager.discoverPeers()
    }

    fun connectWifiDirectPeer(device: WifiP2pDevice) {
        wifiDirectManager.connectToPeer(device)
    }

    fun deleteHistoryRecord(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        nsdManager.stopDiscovery()
        wifiDirectManager.unregisterReceiver()
    }
}
