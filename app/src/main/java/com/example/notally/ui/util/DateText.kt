package com.example.notally.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.notally.R
import java.time.ZoneId

/**
 * Traducción de las etiquetas puras de [DateFormat] a texto visible.
 *
 * Toda la microcopia de fecha vive en `strings.xml`; aquí solo se elige el
 * recurso y se compone el formato de hora.
 */
@Composable
@ReadOnlyComposable
fun dateLabelText(label: DateLabel): String = when (label) {
    DateLabel.Today -> stringResource(R.string.task_due_today)
    DateLabel.Tomorrow -> stringResource(R.string.task_due_tomorrow)
    DateLabel.Yesterday -> stringResource(R.string.task_due_yesterday)
    is DateLabel.Short -> label.text
    is DateLabel.Full -> label.text
}

@Composable
@ReadOnlyComposable
fun dateLabelText(
    dueAt: Long,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): String = dateLabelText(DateFormat.dateLabel(dueAt, now, zone))

/**
 * "Atrasada · ayer" (1 día) o "Atrasada · N días" (N > 1); `null` si no está
 * atrasada. La forma singular/plural la resuelve `<plurals>`.
 */
@Composable
@ReadOnlyComposable
fun overdueText(days: Int): String? = if (days <= 0) {
    null
} else {
    pluralStringResource(R.plurals.task_overdue_days, days, days)
}

/**
 * "Entrega hoy, 23:59" para TalkBack; omite la hora si el instante es
 * medianoche. La fecha se resuelve con recursos y se pasa a minúscula porque
 * la frase la introduce "Entrega".
 */
@Composable
@ReadOnlyComposable
fun duePhraseText(
    dueAt: Long,
    now: Long = System.currentTimeMillis(),
    is24Hour: Boolean = true,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val label = dateLabelText(DateFormat.dateLabel(dueAt, now, zone))
        .lowercase(DateFormat.SpanishLocale)
    return if (DateFormat.hasTimeOfDay(dueAt, zone)) {
        "$label, ${DateFormat.formatTime(dueAt, is24Hour, zone)}"
    } else {
        label
    }
}
