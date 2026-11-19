package com.example.data.connectivity

import com.example.domain.model.DeviceConnectivityState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidNetworkConnectivityObserverTest {

    @Test
    fun `estado inicial e Offline quando nao ha rede ativa`() = runTest {
        val fakePlatform = FakeConnectivityPlatform(initialActiveNetworkId = null)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        try {
            val observer = AndroidNetworkConnectivityObserver(fakePlatform, scope)

            assertEquals(DeviceConnectivityState.Offline, observer.connectivityState.value)
            assertEquals(1, fakePlatform.registerCallCount)
            assertEquals(0, fakePlatform.unregisterCallCount)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `estado inicial e Offline quando rede ativa nao possui NET_CAPABILITY_INTERNET`() = runTest {
        val fakePlatform = FakeConnectivityPlatform(
            initialActiveNetworkId = "net-local",
            initialSnapshots = mapOf(
                "net-local" to NetworkCapabilitiesSnapshot(
                    hasInternet = false,
                    isValidated = false
                )
            )
        )
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        try {
            val observer = AndroidNetworkConnectivityObserver(fakePlatform, scope)

            assertEquals(DeviceConnectivityState.Offline, observer.connectivityState.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `onAvailable sem capacidades nao presume internet ate onCapabilitiesChanged confirmar`() = runTest {
        val fakePlatform = FakeConnectivityPlatform()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        try {
            val observer = AndroidNetworkConnectivityObserver(fakePlatform, scope)
            assertEquals(DeviceConnectivityState.Offline, observer.connectivityState.value)

            fakePlatform.dispatchAvailable("net-1", snapshot = null)
            assertEquals(DeviceConnectivityState.Offline, observer.connectivityState.value)

            fakePlatform.dispatchCapabilitiesChanged(
                networkId = "net-1",
                snapshot = NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = false)
            )
            assertEquals(
                DeviceConnectivityState.ConnectedUnvalidated,
                observer.connectivityState.value
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `transicoes Offline para ConnectedUnvalidated para Online e retorno para ConnectedUnvalidated e Offline`() = runTest {
        val fakePlatform = FakeConnectivityPlatform()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val emissions = mutableListOf<DeviceConnectivityState>()

        try {
            val observer = AndroidNetworkConnectivityObserver(fakePlatform, scope)
            val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                observer.connectivityState.collect { emissions += it }
            }

            // Offline -> ConnectedUnvalidated
            fakePlatform.dispatchAvailable(
                networkId = "wifi-1",
                snapshot = NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = false)
            )
            assertEquals(
                DeviceConnectivityState.ConnectedUnvalidated,
                observer.connectivityState.value
            )

            // ConnectedUnvalidated -> Online
            fakePlatform.dispatchCapabilitiesChanged(
                networkId = "wifi-1",
                snapshot = NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = true)
            )
            assertEquals(DeviceConnectivityState.Online, observer.connectivityState.value)

            // Online -> ConnectedUnvalidated
            fakePlatform.dispatchCapabilitiesChanged(
                networkId = "wifi-1",
                snapshot = NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = false)
            )
            assertEquals(
                DeviceConnectivityState.ConnectedUnvalidated,
                observer.connectivityState.value
            )

            // Volta para Online e depois perde a rede: Online -> Offline
            fakePlatform.dispatchCapabilitiesChanged(
                networkId = "wifi-1",
                snapshot = NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = true)
            )
            assertEquals(DeviceConnectivityState.Online, observer.connectivityState.value)

            fakePlatform.activeNetworkId = null
            fakePlatform.dispatchLost("wifi-1")
            assertEquals(DeviceConnectivityState.Offline, observer.connectivityState.value)

            assertEquals(
                listOf(
                    DeviceConnectivityState.Offline,
                    DeviceConnectivityState.ConnectedUnvalidated,
                    DeviceConnectivityState.Online,
                    DeviceConnectivityState.ConnectedUnvalidated,
                    DeviceConnectivityState.Online,
                    DeviceConnectivityState.Offline
                ),
                emissions
            )

            collectJob.cancel()
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `mudanca entre redes com onLost atrasado da rede antiga preserva estado da nova rede`() = runTest {
        val fakePlatform = FakeConnectivityPlatform(
            initialActiveNetworkId = "wifi-1",
            initialSnapshots = mapOf(
                "wifi-1" to NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = true)
            )
        )
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val emissions = mutableListOf<DeviceConnectivityState>()

        try {
            val observer = AndroidNetworkConnectivityObserver(fakePlatform, scope)
            val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                observer.connectivityState.collect { emissions += it }
            }
            assertEquals(DeviceConnectivityState.Online, observer.connectivityState.value)

            // Nova rede assume antes da entrega de onLost("wifi-1")
            fakePlatform.activeNetworkId = "cell-2"
            fakePlatform.snapshots["cell-2"] = NetworkCapabilitiesSnapshot(
                hasInternet = true,
                isValidated = false
            )
            fakePlatform.dispatchAvailable(
                networkId = "cell-2",
                snapshot = fakePlatform.snapshots["cell-2"]
            )
            assertEquals(
                DeviceConnectivityState.ConnectedUnvalidated,
                observer.connectivityState.value
            )

            // Eventos atrasados da rede antiga ("wifi-1") não podem sobrescrever a rede atual
            fakePlatform.dispatchCapabilitiesChanged(
                networkId = "wifi-1",
                snapshot = NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = true)
            )
            fakePlatform.dispatchLost("wifi-1")
            assertEquals(
                DeviceConnectivityState.ConnectedUnvalidated,
                observer.connectivityState.value
            )

            // Nova rede passa a validada
            fakePlatform.dispatchCapabilitiesChanged(
                networkId = "cell-2",
                snapshot = NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = true)
            )
            assertEquals(DeviceConnectivityState.Online, observer.connectivityState.value)

            assertEquals(
                listOf(
                    DeviceConnectivityState.Online,
                    DeviceConnectivityState.ConnectedUnvalidated,
                    DeviceConnectivityState.Online
                ),
                emissions
            )

            collectJob.cancel()
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `onLost da rede atual consulta activeNetwork caso o sistema ja tenha comutado`() = runTest {
        val fakePlatform = FakeConnectivityPlatform(
            initialActiveNetworkId = "wifi-1",
            initialSnapshots = mapOf(
                "wifi-1" to NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = true)
            )
        )
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        try {
            val observer = AndroidNetworkConnectivityObserver(fakePlatform, scope)
            assertEquals(DeviceConnectivityState.Online, observer.connectivityState.value)

            // Sistema já promoveu "cell-2" como activeNetwork antes de entregar onLost("wifi-1")
            fakePlatform.activeNetworkId = "cell-2"
            fakePlatform.snapshots["cell-2"] = NetworkCapabilitiesSnapshot(
                hasInternet = true,
                isValidated = false
            )
            fakePlatform.dispatchLost("wifi-1")

            assertEquals(
                DeviceConnectivityState.ConnectedUnvalidated,
                observer.connectivityState.value
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `nao emite valores redundantes quando alteracoes resultam no mesmo estado`() = runTest {
        val fakePlatform = FakeConnectivityPlatform()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val emissions = mutableListOf<DeviceConnectivityState>()

        try {
            val observer = AndroidNetworkConnectivityObserver(fakePlatform, scope)
            val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                observer.connectivityState.collect { emissions += it }
            }

            // Eventos consecutivos que permanecem em Offline
            fakePlatform.dispatchAvailable("net-no-internet", null)
            fakePlatform.dispatchCapabilitiesChanged(
                "net-no-internet",
                NetworkCapabilitiesSnapshot(hasInternet = false, isValidated = false)
            )
            fakePlatform.dispatchLost("net-no-internet")

            // Eventos consecutivos que permanecem em Online
            fakePlatform.dispatchAvailable(
                "net-online",
                NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = true)
            )
            fakePlatform.dispatchCapabilitiesChanged(
                "net-online",
                NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = true)
            )
            fakePlatform.dispatchCapabilitiesChanged(
                "net-online",
                NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = true)
            )

            assertEquals(
                listOf(
                    DeviceConnectivityState.Offline,
                    DeviceConnectivityState.Online
                ),
                emissions
            )

            collectJob.cancel()
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `close e cancelamento do escopo removem callback de forma idempotente e ignoram eventos posteriores`() = runTest {
        val fakePlatform = FakeConnectivityPlatform(
            initialActiveNetworkId = "wifi-1",
            initialSnapshots = mapOf(
                "wifi-1" to NetworkCapabilitiesSnapshot(hasInternet = true, isValidated = true)
            )
        )
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val observer = AndroidNetworkConnectivityObserver(fakePlatform, scope)

        assertEquals(DeviceConnectivityState.Online, observer.connectivityState.value)
        assertEquals(1, fakePlatform.registerCallCount)
        assertEquals(0, fakePlatform.unregisterCallCount)

        // Cancelamento do scope dispara remoção automática
        scope.cancel()
        assertEquals(1, fakePlatform.unregisterCallCount)
        assertTrue(fakePlatform.isUnregistered)

        // Fechamento explícito posterior é idempotente
        observer.close()
        assertEquals(1, fakePlatform.unregisterCallCount)

        // Callbacks atrasados entregues após encerramento não alteram mais o StateFlow
        fakePlatform.activeNetworkId = null
        fakePlatform.dispatchLateLost("wifi-1")
        assertEquals(DeviceConnectivityState.Online, observer.connectivityState.value)
    }

    @Test
    fun `close direto antes do cancelamento do escopo desregistra callback imediatamente`() = runTest {
        val fakePlatform = FakeConnectivityPlatform()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        try {
            val observer = AndroidNetworkConnectivityObserver(fakePlatform, scope)
            assertEquals(1, fakePlatform.registerCallCount)

            observer.close()
            assertEquals(1, fakePlatform.unregisterCallCount)
            assertTrue(fakePlatform.isUnregistered)

            scope.cancel()
            assertEquals(1, fakePlatform.unregisterCallCount)
        } finally {
            scope.cancel()
        }
    }

    private class FakeConnectivityPlatform(
        initialActiveNetworkId: Any? = null,
        initialSnapshots: Map<Any, NetworkCapabilitiesSnapshot> = emptyMap()
    ) : ConnectivityPlatform {
        var activeNetworkId: Any? = initialActiveNetworkId
        val snapshots: MutableMap<Any, NetworkCapabilitiesSnapshot> = initialSnapshots.toMutableMap()
        var registerCallCount: Int = 0
            private set
        var unregisterCallCount: Int = 0
            private set
        var isUnregistered: Boolean = false
            private set

        private var activeHandle: NetworkCallbackHandle? = null
        private var lastRegisteredHandle: NetworkCallbackHandle? = null

        override fun currentActiveNetworkId(): Any? = activeNetworkId

        override fun getCapabilitiesSnapshot(networkId: Any): NetworkCapabilitiesSnapshot? =
            snapshots[networkId]

        override fun registerDefaultNetworkCallback(handle: NetworkCallbackHandle): AutoCloseable {
            registerCallCount += 1
            activeHandle = handle
            lastRegisteredHandle = handle
            isUnregistered = false
            return AutoCloseable {
                unregisterCallCount += 1
                isUnregistered = true
                activeHandle = null
            }
        }

        fun dispatchAvailable(networkId: Any, snapshot: NetworkCapabilitiesSnapshot?) {
            val handle = activeHandle
            assertNotNull("Callback precisa estar registrado para despachar onAvailable", handle)
            if (snapshot != null) {
                snapshots[networkId] = snapshot
            }
            handle?.onAvailable(networkId, snapshot)
        }

        fun dispatchCapabilitiesChanged(
            networkId: Any,
            snapshot: NetworkCapabilitiesSnapshot
        ) {
            val handle = activeHandle
            assertNotNull("Callback precisa estar registrado para despachar onCapabilitiesChanged", handle)
            snapshots[networkId] = snapshot
            handle?.onCapabilitiesChanged(networkId, snapshot)
        }

        fun dispatchLost(networkId: Any) {
            val handle = activeHandle
            assertNotNull("Callback precisa estar registrado para despachar onLost", handle)
            snapshots.remove(networkId)
            handle?.onLost(networkId)
        }

        fun dispatchLateLost(networkId: Any) {
            val handle = lastRegisteredHandle
            assertNotNull("Callback precisa ter sido registrado antes do despacho tardio", handle)
            snapshots.remove(networkId)
            handle?.onLost(networkId)
        }
    }
}
