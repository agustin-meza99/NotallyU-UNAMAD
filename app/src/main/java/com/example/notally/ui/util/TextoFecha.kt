package com.example.notally.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.notally.R
import java.time.ZoneId

/**
 * Traducción de las etiquetas puras de [FormatoFecha] a texto visible.
 *
 * Toda la microcopia de fecha vive en `strings.xml`; aquí solo se elige el
 * recurso y se compone el formato de hora.
 */
@Composable
@ReadOnlyComposable
fun textoEtiquetaFecha(etiqueta: EtiquetaFecha): String = when (etiqueta) {
    EtiquetaFecha.Hoy -> stringResource(R.string.task_due_today)
    EtiquetaFecha.Manana -> stringResource(R.string.task_due_tomorrow)
    EtiquetaFecha.Ayer -> stringResource(R.string.task_due_yesterday)
    is EtiquetaFecha.Corta -> etiqueta.texto
    is EtiquetaFecha.Larga -> etiqueta.texto
}

@Composable
@ReadOnlyComposable
fun textoEtiquetaFecha(
    entregaEn: Long,
    ahora: Long = System.currentTimeMillis(),
    zona: ZoneId = ZoneId.systemDefault(),
): String = textoEtiquetaFecha(FormatoFecha.etiquetaFecha(entregaEn, ahora, zona))

/**
 * "Atrasada · ayer" (1 día) o "Atrasada · N días" (N > 1); `null` si no está
 * atrasada. La forma singular/plural la resuelve `<plurals>`.
 */
@Composable
@ReadOnlyComposable
fun textoAtraso(dias: Int): String? = if (dias <= 0) {
    null
} else {
    pluralStringResource(R.plurals.task_overdue_days, dias, dias)
}

/**
 * "Entrega hoy, 23:59" para TalkBack; omite la hora si el instante es
 * medianoche. La fecha se resuelve con recursos y se pasa a minúscula porque
 * la frase la introduce "Entrega".
 */
@Composable
@ReadOnlyComposable
fun textoFraseEntrega(
    entregaEn: Long,
    ahora: Long = System.currentTimeMillis(),
    es24Horas: Boolean = true,
    zona: ZoneId = ZoneId.systemDefault(),
): String {
    val etiqueta = textoEtiquetaFecha(FormatoFecha.etiquetaFecha(entregaEn, ahora, zona))
        .lowercase(FormatoFecha.LocaleEspanol)
    return if (FormatoFecha.tieneHora(entregaEn, zona)) {
        "$etiqueta, ${FormatoFecha.formatearHora(entregaEn, es24Horas, zona)}"
    } else {
        etiqueta
    }
}
