package com.example.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VpnStatus(
    val isVpnConnected: Boolean = false,
    val interfaceName: String? = null,
    val activeTransports: List<String> = emptyList(),
    val isValidated: Boolean = false,
)

class VpnMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _status = MutableStateFlow(inspectCurrentStatus())
    val status: StateFlow<VpnStatus> = _status.asStateFlow()

    private var onVpnStateChangedListener: ((VpnStatus) -> Unit)? = null
    private var isRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refreshAndNotify()
        }

        override fun onLost(network: Network) {
            refreshAndNotify()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            refreshAndNotify()
        }
    }

    fun startMonitoring(onStateChanged: ((VpnStatus) -> Unit)? = null) {
        onVpnStateChangedListener = onStateChanged
        if (isRegistered) return
        try {
            val request = NetworkRequest.Builder()
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isRegistered = true
        } catch (_: Exception) {
            // Ignore if registration limit reached; synchronous inspection still works
        }
        refreshAndNotify()
    }

    fun refreshNow(): VpnStatus {
        val current = inspectCurrentStatus()
        _status.value = current
        return current
    }

    fun isVpnConnectedNow(): Boolean = inspectCurrentStatus().isVpnConnected

    private fun refreshAndNotify() {
        val previous = _status.value
        val updated = inspectCurrentStatus()
        _status.value = updated
        if (previous.isVpnConnected != updated.isVpnConnected) {
            onVpnStateChangedListener?.invoke(updated)
        }
    }

    @Suppress("DEPRECATION")
    private fun inspectCurrentStatus(): VpnStatus {
        val transports = linkedSetOf<String>()
        var vpnConnected = false
        var vpnInterface: String? = null
        var vpnValidated = false

        try {
            val allNetworks = connectivityManager.allNetworks
            for (network in allNetworks) {
                val caps = connectivityManager.getNetworkCapabilities(network) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    vpnConnected = true
                    transports.add("VPN")
                    val linkProps = connectivityManager.getLinkProperties(network)
                    if (vpnInterface == null && !linkProps?.interfaceName.isNullOrBlank()) {
                        vpnInterface = linkProps?.interfaceName
                    }
                    if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        vpnValidated = true
                    }
                }
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    transports.add("Wi-Fi")
                }
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    transports.add("Cellular")
                }
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    transports.add("Ethernet")
                }
            }
        } catch (_: Exception) {
            // Fallback to active network only
            val active = connectivityManager.activeNetwork
            val caps = active?.let { connectivityManager.getNetworkCapabilities(it) }
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                vpnConnected = true
                transports.add("VPN")
            }
        }

        return VpnStatus(
            isVpnConnected = vpnConnected,
            interfaceName = vpnInterface,
            activeTransports = transports.toList(),
            isValidated = vpnValidated,
        )
    }
}
