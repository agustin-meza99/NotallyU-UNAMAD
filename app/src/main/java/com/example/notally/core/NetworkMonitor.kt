package com.example.notally.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitor reactivo de conectividad de red (ISO/IEC 25010 — Fiabilidad / Disponibilidad).
 *
 * Permite que la UI informe al estudiante cuando trabaja sobre la caché local
 * de Firestore sin conexión a internet y recupere automáticamente el estado en
 * línea al restablecerse la red.
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager

    private val _conectado = MutableStateFlow(verificarConectividadActual())
    val conectado: StateFlow<Boolean> = _conectado.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _conectado.value = true
        }

        override fun onLost(network: Network) {
            _conectado.value = verificarConectividadActual()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            val tieneInternet = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET,
            )
            _conectado.value = tieneInternet || verificarConectividadActual()
        }
    }

    init {
        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, callback)
        }
    }

    private fun verificarConectividadActual(): Boolean {
        val manager = connectivityManager ?: return true
        return runCatching {
            val red = manager.activeNetwork ?: return@runCatching false
            val caps = manager.getNetworkCapabilities(red) ?: return@runCatching false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(true)
    }
}
