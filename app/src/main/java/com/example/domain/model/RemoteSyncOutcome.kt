package com.example.domain.model

sealed interface RemoteSyncOutcome {
    data class Synced(val entriesCount: Int) : RemoteSyncOutcome {
        init {
            require(entriesCount > 0) {
                "RemoteSyncOutcome.Synced exige entriesCount maior que zero, mas foi $entriesCount."
            }
        }
    }

    data object Empty : RemoteSyncOutcome
}
