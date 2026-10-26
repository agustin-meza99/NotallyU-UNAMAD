package com.example.notally.ui.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emite el instante actual y luego en cada cambio de minuto.
 *
 * El primer tick es inmediato y los siguientes esperan el resto del minuto en
 * curso, así que todos los ViewModels que muestran fechas relativas se
 * refrescan en el mismo instante lógico.
 */
internal fun clockFlow(clock: () -> Long): Flow<Long> = flow {
    while (true) {
        val now = clock()
        emit(now)
        delay(60_000L - (now % 60_000L))
    }
}
