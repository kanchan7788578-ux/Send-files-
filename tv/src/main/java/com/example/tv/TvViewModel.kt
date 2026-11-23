package com.example.tv

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.BeamDatabase
import com.example.shared.data.LanguagePrefsRepository
import com.example.shared.data.TransferRecordEntity
import com.example.shared.model.AppLanguage
import com.example.shared.model.TransferState
import com.example.shared.model.TvServerInfo
import com.example.shared.transfer.NetworkUtils
import com.example.shared.transfer.NsdDiscoveryManager
import com.example.shared.transfer.TransferServer
import com.example.shared.transfer.WifiDirectManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val langRepo = LanguagePrefsRepository(application)
    private val dao = BeamDatabase.getInstance(application).transferDao()

    private val transferServer = TransferServer(application, viewModelScope) { record ->
        dao.insert(record)
    }
    private val nsdManager = NsdDiscoveryManager(application)
    val wifiDirectManager = WifiDirectManager(application)

    val language: StateFlow<AppLanguage> = langRepo.languageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.BENGALI)

    val receivedRecords: StateFlow<List<TransferRecordEntity>> = dao.observeReceived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receiveState: StateFlow<TransferState> = transferServer.receiveState

    private val _localIp = MutableStateFlow(NetworkUtils.getBestLocalIpv4Address())
    private val _pin = Random.nextInt(1000, 9999).toString()

    val tvServerInfo: StateFlow<TvServerInfo> = combine(
        transferServer.isRunning,
        transferServer.boundPort,
        _localIp,
        wifiDirectManager.state
    ) { running, port, ip, p2p ->
        val effectiveIp = if (p2p.isGroupOwner && !p2p.groupOwnerIp.isNullOrBlank()) {
            p2p.groupOwnerIp!!
        } else {
            ip
        }
        TvServerInfo(
            isRunning = running,
            localIp = effectiveIp,
            port = port,
            deviceName = Build.MODEL ?: "BeamTV TV",
            pin = _pin,
            wifiDirectActive = p2p.isGroupOwner,
            wifiDirectSsid = p2p.groupSsid,
            wifiDirectPass = p2p.groupPassphrase,
            wifiDirectError = p2p.statusMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TvServerInfo())

    init {
        wifiDirectManager.registerReceiver()
        val port = transferServer.start()
        nsdManager.registerTvService(port, Build.MODEL ?: "BeamTV TV")
        refreshNetworkIp()
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (language.value == AppLanguage.BENGALI) AppLanguage.ENGLISH else AppLanguage.BENGALI
            langRepo.setLanguage(next)
        }
    }

    fun refreshNetworkIp() {
        viewModelScope.launch(Dispatchers.IO) {
            _localIp.value = NetworkUtils.getBestLocalIpv4Address()
        }
    }

    fun toggleWifiDirect() {
        if (wifiDirectManager.state.value.isGroupOwner) {
            wifiDirectManager.stopTvGroup()
        } else {
            wifiDirectManager.startTvGroup()
        }
    }

    fun resetReceiveState() {
        transferServer.resetState()
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        nsdManager.unregisterTvService()
        transferServer.stop()
        wifiDirectManager.unregisterReceiver()
    }
}
