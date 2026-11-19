package com.example.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import com.example.di.ConnectivityModule
import com.example.domain.model.DeviceConnectivityState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AndroidConnectivityPlatformRobolectricTest {

    @Test
    fun `ConnectivityModule obtem ConnectivityManager real e observer registra e remove defaultNetworkCallback`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val connectivityManager = ConnectivityModule.provideConnectivityManager(context)
        val systemManager = context.getSystemService(ConnectivityManager::class.java)
        assertSame(systemManager, connectivityManager)

        val shadowCm = shadowOf(connectivityManager)
        @Suppress("DEPRECATION")
        shadowCm.setDefaultNetworkActive(false)

        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
        val observer = ConnectivityModule.provideAndroidNetworkConnectivityObserver(
            connectivityManager = connectivityManager,
            applicationScope = scope
        )

        try {
            assertEquals(1, shadowCm.networkCallbacks.size)
            val registeredCallback = shadowCm.networkCallbacks.first()
            @Suppress("DEPRECATION")
            val activeNetwork: Network = connectivityManager.activeNetwork
                ?: connectivityManager.allNetworks.firstOrNull()
                ?: ShadowNetwork.newInstance(100)

            val unvalidatedCaps = ShadowNetworkCapabilities.newInstance()
            shadowOf(unvalidatedCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            shadowCm.setNetworkCapabilities(activeNetwork, unvalidatedCaps)

            registeredCallback.onAvailable(activeNetwork)
            assertEquals(
                DeviceConnectivityState.ConnectedUnvalidated,
                observer.connectivityState.value
            )

            val validatedCaps = ShadowNetworkCapabilities.newInstance()
            shadowOf(validatedCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            shadowOf(validatedCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            shadowCm.setNetworkCapabilities(activeNetwork, validatedCaps)

            registeredCallback.onCapabilitiesChanged(activeNetwork, validatedCaps)
            assertEquals(DeviceConnectivityState.Online, observer.connectivityState.value)

            registeredCallback.onCapabilitiesChanged(activeNetwork, unvalidatedCaps)
            assertEquals(
                DeviceConnectivityState.ConnectedUnvalidated,
                observer.connectivityState.value
            )

            // Sem rede ativa após perda da rede
            shadowCm.clearAllNetworks()
            registeredCallback.onLost(activeNetwork)
            assertEquals(DeviceConnectivityState.Offline, observer.connectivityState.value)
        } finally {
            scope.cancel()
        }

        assertEquals(0, shadowCm.networkCallbacks.size)
    }
}
