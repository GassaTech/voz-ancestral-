package com.example.domain.sync

import com.example.domain.model.DeviceConnectivityState
import com.example.domain.model.SyncFailureType

object SyncFailureClassifier {
    fun classify(connectivityState: DeviceConnectivityState): SyncFailureType =
        when (connectivityState) {
            DeviceConnectivityState.Offline -> SyncFailureType.NO_INTERNET
            DeviceConnectivityState.ConnectedUnvalidated -> SyncFailureType.UNVALIDATED_NETWORK
            DeviceConnectivityState.Online -> SyncFailureType.SERVER_UNAVAILABLE
        }
}
