package com.example.domain.model

sealed interface DeviceConnectivityState {
    data object Offline : DeviceConnectivityState
    data object ConnectedUnvalidated : DeviceConnectivityState
    data object Online : DeviceConnectivityState
}
