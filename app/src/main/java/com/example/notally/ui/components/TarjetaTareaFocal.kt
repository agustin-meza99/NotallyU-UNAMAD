package com.example.notally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.data.model.Prioridad
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.util.FormatoFecha

/**
 * Tarea focal de Hoy (§7.3.1).
 *
 * Único bloque invertido de la pantalla: `inverseSurface` con texto
 * `inverseOnSurface`, sin borde, sin sombra y sin checkbox. Toda la tarjeta
 * abre el editor y se anuncia como un solo nodo.
 *
 * @param antetitulo grupo ya resuelto ("Atrasadas", "Para hoy", "Sin hora",
 *   "Próximas"); la tarjeta aplica uppercase.
 * @param textoEntrega etiqueta de entrega ("Hoy", "Vie 31 oct", "Sin fecha").
 */
@Composable
fun TarjetaTareaFocal(
    titulo: String,
    antetitulo: String,
    textoMateria: String,
    textoEntrega: String,
    prioridad: Prioridad,
    modifier: Modifier = Modifier,
    alPulsar: () -> Unit = {},
) {
    val colores = MaterialTheme.colorScheme
    var enfocada by remember { mutableStateOf(false) }

    val etiquetaPrioridad = stringResource(
        when (prioridad) {
            Prioridad.ALTA -> R.string.a11y_priority_high
            Prioridad.MEDIA -> R.string.a11y_priority_medium
            Prioridad.BAJA -> R.string.a11y_priority_low
        },
    )
    val separador = stringResource(R.string.task_meta_separator)
    val descripcion = stringResource(
        R.string.a11y_featured_task,
        titulo,
        textoMateria,
        textoEntrega,
        etiquetaPrioridad,
    )
    val etiquetaAbrir = stringResource(R.string.a11y_openTask)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .onFocusChanged { enfocada = it.isFocused }
            .anilloFoco(visible = enfocada, shape = FormaNotally.l)
            .clip(FormaNotally.l)
            .background(colores.inverseSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colores.inverseOnSurface),
                role = Role.Button,
                onClickLabel = etiquetaAbrir,
                onClick = alPulsar,
            )
            .semantics { contentDescription = descripcion }
            .padding(16.dp),
    ) {
        Text(
            text = antetitulo.uppercase(FormatoFecha.LocaleEspanol),
            style = MaterialTheme.typography.labelSmall,
            color = colores.inverseOnSurface,
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .padding(start = anchoRielPrioridad)
                .rielPrioridad(
                    prioridad = prioridad,
                    hecha = false,
                    color = colores.inverseOnSurface,
                ),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = colores.inverseOnSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = textoMateria + separador + textoEntrega,
                style = MaterialTheme.typography.bodySmall,
                color = colores.inverseOnSurface,
            )
        }
    }
}
