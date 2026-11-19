package com.example.data.connectivity

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.util.concurrent.atomic.AtomicBoolean

internal data class NetworkCapabilitiesSnapshot(
    val hasInternet: Boolean,
    val isValidated: Boolean
)

internal interface NetworkCallbackHandle {
    fun onAvailable(networkId: Any, snapshot: NetworkCapabilitiesSnapshot?)
    fun onCapabilitiesChanged(networkId: Any, snapshot: NetworkCapabilitiesSnapshot)
    fun onLost(networkId: Any)
}

internal interface ConnectivityPlatform {
    fun currentActiveNetworkId(): Any?
    fun getCapabilitiesSnapshot(networkId: Any): NetworkCapabilitiesSnapshot?
    fun registerDefaultNetworkCallback(handle: NetworkCallbackHandle): AutoCloseable
}

internal class AndroidConnectivityPlatform(
    private val connectivityManager: ConnectivityManager
) : ConnectivityPlatform {

    override fun currentActiveNetworkId(): Any? = connectivityManager.activeNetwork

    override fun getCapabilitiesSnapshot(networkId: Any): NetworkCapabilitiesSnapshot? {
        val network = networkId as? Network ?: return null
        return connectivityManager.getNetworkCapabilities(network)?.toSnapshot()
    }

    override fun registerDefaultNetworkCallback(handle: NetworkCallbackHandle): AutoCloseable {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handle.onAvailable(network, getCapabilitiesSnapshot(network))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                handle.onCapabilitiesChanged(network, networkCapabilities.toSnapshot())
            }

            override fun onLost(network: Network) {
                handle.onLost(network)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        val unregistered = AtomicBoolean(false)
        return AutoCloseable {
            if (unregistered.compareAndSet(false, true)) {
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (_: IllegalArgumentException) {
                    // Callback já removido pelo sistema operacional.
                }
            }
        }
    }

    private fun NetworkCapabilities.toSnapshot(): NetworkCapabilitiesSnapshot =
        NetworkCapabilitiesSnapshot(
            hasInternet = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            isValidated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        )
}
