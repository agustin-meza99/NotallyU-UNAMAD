package com.example.notally.ui.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.notally.R
import com.example.notally.ui.components.EmptyState

@Composable
internal fun EmptyStateHoy(
    vacio: VacioHoy,
    alCrearTarea: () -> Unit,
    alMostrarTodas: () -> Unit,
    alMostrarPendientes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (vacio) {
        VacioHoy.Total -> EmptyState(
            titulo = stringResource(R.string.today_empty_title),
            cuerpo = stringResource(R.string.today_empty_body),
            etiquetaAccion = stringResource(R.string.today_empty_cta),
            alAccionar = alCrearTarea,
            modifier = modifier,
        )

        VacioHoy.FiltroPendientes -> EmptyState(
            titulo = stringResource(R.string.filter_empty_pending_title),
            cuerpo = stringResource(R.string.filter_empty_pending_body),
            etiquetaAccion = stringResource(R.string.filter_empty_pending_cta),
            alAccionar = alMostrarTodas,
            modifier = modifier,
        )

        VacioHoy.FiltroCompletadas -> EmptyState(
            titulo = stringResource(R.string.filter_empty_done_title),
            cuerpo = stringResource(R.string.filter_empty_done_body),
            etiquetaAccion = stringResource(R.string.filter_empty_done_cta),
            alAccionar = alMostrarPendientes,
            modifier = modifier,
        )
    }
}
