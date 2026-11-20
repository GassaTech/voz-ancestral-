package com.example.core.common

enum class RemoteErrorKind {
    DNS_FAILURE,
    CONNECTION_FAILURE,
    TIMEOUT,
    SSL_HANDSHAKE_FAILURE,
    IO_FAILURE,
    HTTP_CLIENT_ERROR,
    HTTP_SERVER_ERROR,
    HTTP_OTHER_ERROR,
    DESERIALIZATION_ERROR,
    UNEXPECTED_ERROR
}

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val remoteErrorKind: RemoteErrorKind? = null
    ) : AppResult<Nothing>
}
