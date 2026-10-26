package com.example.notally.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notally.contenedorNotally
import com.example.notally.data.model.Ajustes
import com.example.notally.data.model.ModoTema
import com.example.notally.data.repository.RepositorioAjustes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel de Ajustes (§7.12). Persiste cada cambio en DataStore. */
class AjustesViewModel(
    private val repositorio: RepositorioAjustes,
) : ViewModel() {

    val ajustes: StateFlow<Ajustes> = repositorio.ajustes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Ajustes(),
    )

    fun cambiarModoTema(modo: ModoTema) {
        viewModelScope.launch { repositorio.cambiarModoTema(modo) }
    }

    fun cambiarColorDinamico(activo: Boolean) {
        viewModelScope.launch { repositorio.cambiarColorDinamico(activo) }
    }

    fun cambiarSemanaEmpiezaLunes(activo: Boolean) {
        viewModelScope.launch { repositorio.cambiarSemanaEmpiezaLunes(activo) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AjustesViewModel(context.contenedorNotally.repositorioAjustes)
            }
        }
    }
}
