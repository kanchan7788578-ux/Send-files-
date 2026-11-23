package com.example.shared.transfer

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import com.example.shared.model.DiscoveredTvDevice
import com.example.shared.model.DiscoverySource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NsdDiscoveryManager(context: Context) {

    companion object {
        private const val SERVICE_TYPE = "_beamtv._tcp."
        private const val TAG = "NsdDiscoveryManager"
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredTvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredTvDevice>> = _discoveredDevices.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun registerTvService(port: Int, deviceName: String = Build.MODEL ?: "BeamTV TV") {
        unregisterTvService()
        val manager = nsdManager ?: return
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "BeamTV-$deviceName"
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(TAG, "NSD TV registered: ${info.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD registration failed: $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }
        registrationListener = listener
        try {
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "registerService failed", e)
        }
    }

    fun unregisterTvService() {
        val listener = registrationListener ?: return
        registrationListener = null
        try {
            nsdManager?.unregisterService(listener)
        } catch (_: Exception) {
        }
    }

    fun startDiscovery() {
        stopDiscovery()
        val manager = nsdManager ?: return
        _discoveredDevices.value = emptyList()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                if (!service.serviceType.contains("beamtv")) return
                resolveService(manager, service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                _discoveredDevices.value = _discoveredDevices.value.filterNot {
                    it.id == "nsd_${service.serviceName}"
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        discoveryListener = listener
        try {
            manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "discoverServices failed", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveService(manager: NsdManager, service: NsdServiceInfo) {
        try {
            manager.resolveService(service, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val host = serviceInfo.host?.hostAddress ?: return
                    val port = serviceInfo.port
                    val cleanName = serviceInfo.serviceName.removePrefix("BeamTV-").ifBlank { "BeamTV TV" }
                    val device = DiscoveredTvDevice(
                        id = "nsd_${serviceInfo.serviceName}",
                        name = cleanName,
                        ip = host,
                        port = port,
                        source = DiscoverySource.NSD
                    )
                    val current = _discoveredDevices.value.filterNot { it.id == device.id }
                    _discoveredDevices.value = current + device
                }
            })
        } catch (_: Exception) {
        }
    }

    fun stopDiscovery() {
        val listener = discoveryListener ?: return
        discoveryListener = null
        try {
            nsdManager?.stopServiceDiscovery(listener)
        } catch (_: Exception) {
        }
    }
}
