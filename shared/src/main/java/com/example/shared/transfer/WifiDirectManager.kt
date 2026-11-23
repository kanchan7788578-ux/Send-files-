package com.example.shared.transfer

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WifiDirectState(
    val isSupported: Boolean = false,
    val isGroupOwner: Boolean = false,
    val groupSsid: String? = null,
    val groupPassphrase: String? = null,
    val groupOwnerIp: String? = null,
    val peers: List<WifiP2pDevice> = emptyList(),
    val statusMessage: String? = null
)

class WifiDirectManager(private val context: Context) {

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: WifiP2pManager.Channel? =
        manager?.initialize(context, Looper.getMainLooper(), null)

    private val _state = MutableStateFlow(WifiDirectState(isSupported = manager != null && channel != null))
    val state: StateFlow<WifiDirectState> = _state.asStateFlow()

    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    if (hasWifiDirectPermission()) {
                        manager?.requestPeers(channel) { peerList ->
                            _state.value = _state.value.copy(peers = peerList.deviceList.toList())
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    requestConnectionAndGroupInfo()
                }
            }
        }
    }

    fun hasWifiDirectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
    }

    fun registerReceiver() {
        if (receiverRegistered || manager == null) return
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        receiverRegistered = true
    }

    fun unregisterReceiver() {
        if (!receiverRegistered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
        receiverRegistered = false
    }

    @SuppressLint("MissingPermission")
    fun startTvGroup() {
        val mgr = manager ?: return
        val ch = channel ?: return
        if (!hasWifiDirectPermission()) {
            _state.value = _state.value.copy(statusMessage = "Nearby Wi-Fi permission required")
            return
        }
        mgr.createGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                requestConnectionAndGroupInfo()
            }

            override fun onFailure(reason: Int) {
                _state.value = _state.value.copy(
                    statusMessage = "Wi-Fi Direct busy (code $reason). Local Wi-Fi/Hotspot still works."
                )
            }
        })
    }

    fun stopTvGroup() {
        val mgr = manager ?: return
        val ch = channel ?: return
        mgr.removeGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _state.value = _state.value.copy(
                    isGroupOwner = false,
                    groupSsid = null,
                    groupPassphrase = null,
                    groupOwnerIp = null,
                    statusMessage = null
                )
            }

            override fun onFailure(reason: Int) {
                _state.value = _state.value.copy(isGroupOwner = false)
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        val mgr = manager ?: return
        val ch = channel ?: return
        if (!hasWifiDirectPermission()) return
        mgr.discoverPeers(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _state.value = _state.value.copy(statusMessage = "Scanning Wi-Fi Direct peers...")
            }

            override fun onFailure(reason: Int) {
                _state.value = _state.value.copy(statusMessage = "Wi-Fi Direct scan code $reason")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(device: WifiP2pDevice) {
        val mgr = manager ?: return
        val ch = channel ?: return
        if (!hasWifiDirectPermission()) return
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        mgr.connect(ch, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _state.value = _state.value.copy(statusMessage = "Connecting to ${device.deviceName}...")
            }

            override fun onFailure(reason: Int) {
                _state.value = _state.value.copy(statusMessage = "Connect failed ($reason)")
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun requestConnectionAndGroupInfo() {
        val mgr = manager ?: return
        val ch = channel ?: return
        mgr.requestConnectionInfo(ch) { info ->
            val ownerIp = info?.groupOwnerAddress?.hostAddress
            if (hasWifiDirectPermission()) {
                mgr.requestGroupInfo(ch) { group ->
                    _state.value = _state.value.copy(
                        isGroupOwner = info?.isGroupOwner == true,
                        groupSsid = group?.networkName,
                        groupPassphrase = group?.passphrase,
                        groupOwnerIp = ownerIp,
                        statusMessage = if (group != null) "Wi-Fi Direct Active" else null
                    )
                }
            } else {
                _state.value = _state.value.copy(
                    isGroupOwner = info?.isGroupOwner == true,
                    groupOwnerIp = ownerIp
                )
            }
        }
    }
}
