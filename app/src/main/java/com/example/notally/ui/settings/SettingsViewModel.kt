package com.example.notally.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notally.contenedorNotally
import com.example.notally.data.model.Settings
import com.example.notally.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel de Settings (§7.12). Persiste cada cambio en DataStore. */
class SettingsViewModel(
    private val repositorio: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<Settings> = repositorio.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings(),
    )

    fun cambiarSemanaEmpiezaLunes(activo: Boolean) {
        viewModelScope.launch { repositorio.cambiarSemanaEmpiezaLunes(activo) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(context.contenedorNotally.repositorioSettings)
            }
        }
    }
}
