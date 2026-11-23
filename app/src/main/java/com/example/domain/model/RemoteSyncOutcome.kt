package com.example.domain.model

sealed interface RemoteSyncOutcome {
    data class Synced(val territoriesCount: Int) : RemoteSyncOutcome {
        init {
            require(territoriesCount > 0) {
                "RemoteSyncOutcome.Synced exige territoriesCount maior que zero, mas foi $territoriesCount."
            }
        }
    }

    data object Empty : RemoteSyncOutcome
}
