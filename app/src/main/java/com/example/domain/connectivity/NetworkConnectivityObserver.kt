package com.example.domain.connectivity

import com.example.domain.model.DeviceConnectivityState
import kotlinx.coroutines.flow.StateFlow

interface NetworkConnectivityObserver {
    val connectivityState: StateFlow<DeviceConnectivityState>
}
