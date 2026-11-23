package com.example.data.connectivity

import android.net.ConnectivityManager
import com.example.core.common.ApplicationScope
import com.example.domain.connectivity.NetworkConnectivityObserver
import com.example.domain.model.DeviceConnectivityState
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AndroidNetworkConnectivityObserver internal constructor(
    private val platform: ConnectivityPlatform,
    @ApplicationScope private val applicationScope: CoroutineScope
) : NetworkConnectivityObserver, AutoCloseable {

    constructor(
        connectivityManager: ConnectivityManager,
        applicationScope: CoroutineScope
    ) : this(
        platform = AndroidConnectivityPlatform(connectivityManager),
        applicationScope = applicationScope
    )

    private val stateLock = Any()
    private var currentNetworkId: Any? = null
    private var callbackHandle: AutoCloseable? = null
    private var isClosed: Boolean = false

    private val _connectivityState = MutableStateFlow<DeviceConnectivityState>(
        DeviceConnectivityState.Offline
    )
    override val connectivityState: StateFlow<DeviceConnectivityState> =
        _connectivityState.asStateFlow()

    private val networkCallbackHandle = object : NetworkCallbackHandle {
        override fun onAvailable(networkId: Any, snapshot: NetworkCapabilitiesSnapshot?) {
            synchronized(stateLock) {
                if (isClosed) return
                currentNetworkId = networkId
                updateStateLocked(snapshot.toConnectivityState())
            }
        }

        override fun onCapabilitiesChanged(
            networkId: Any,
            snapshot: NetworkCapabilitiesSnapshot
        ) {
            synchronized(stateLock) {
                if (isClosed) return
                val tracked = currentNetworkId
                val activeId = platform.currentActiveNetworkId()
                if (tracked == null || networkId == tracked || networkId == activeId) {
                    currentNetworkId = networkId
                    updateStateLocked(snapshot.toConnectivityState())
                }
            }
        }

        override fun onLost(networkId: Any) {
            synchronized(stateLock) {
                if (isClosed) return
                val tracked = currentNetworkId
                if (tracked != null && networkId != tracked) {
                    return
                }
                val activeAfterLoss = platform.currentActiveNetworkId()
                if (activeAfterLoss != null && activeAfterLoss != networkId) {
                    currentNetworkId = activeAfterLoss
                    val snapshot = platform.getCapabilitiesSnapshot(activeAfterLoss)
                    updateStateLocked(snapshot.toConnectivityState())
                } else {
                    currentNetworkId = null
                    updateStateLocked(DeviceConnectivityState.Offline)
                }
            }
        }
    }

    init {
        synchronized(stateLock) {
            val initialNetworkId = platform.currentActiveNetworkId()
            currentNetworkId = initialNetworkId
            val initialSnapshot = initialNetworkId?.let { platform.getCapabilitiesSnapshot(it) }
            updateStateLocked(initialSnapshot.toConnectivityState())
            callbackHandle = platform.registerDefaultNetworkCallback(networkCallbackHandle)
        }
        applicationScope.coroutineContext[Job]?.invokeOnCompletion {
            close()
        }
    }

    override fun close() {
        val handleToClose = synchronized(stateLock) {
            if (isClosed) return
            isClosed = true
            currentNetworkId = null
            callbackHandle.also { callbackHandle = null }
        }
        handleToClose?.close()
    }

    private fun updateStateLocked(newState: DeviceConnectivityState) {
        if (_connectivityState.value != newState) {
            _connectivityState.value = newState
        }
    }

    private fun NetworkCapabilitiesSnapshot?.toConnectivityState(): DeviceConnectivityState {
        if (this == null || !hasInternet) {
            return DeviceConnectivityState.Offline
        }
        return if (isValidated) {
            DeviceConnectivityState.Online
        } else {
            DeviceConnectivityState.ConnectedUnvalidated
        }
    }
}
