package com.example.notally.ui.editor

import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.notally.R
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Curso
import com.example.notally.data.model.Prioridad
import com.example.notally.ui.components.OptionRow
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.util.DateFormat
import com.example.notally.ui.util.textoEtiquetaFecha
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Hoja abierta desde las propiedades del editor. */
enum class HojaEditor {
    CURSO,
    ENTREGA,
    PRIORIDAD,
    ESTADO,
}

/**
 * Hojas de propiedad del editor (§7.5, §7.9).
 *
 * Cada hoja se cierra al elegir una opción, salvo la de Entrega: tras fijar la
 * fecha se queda abierta para permitir la hora opcional, y se cierra al
 * terminar o al tocar fuera.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnfitrionEditorSheets(
    hoja: HojaEditor?,
    borrador: BorradorTarea,
    cursos: List<Curso>,
    colorCursoPorDefecto: Long,
    errorNuevoCurso: ErrorCursoEditor?,
    alDescartar: () -> Unit,
    alSeleccionarCurso: (Long?) -> Unit,
    alCrearCurso: (String, Long) -> Unit,
    alLimpiarErrorCurso: () -> Unit,
    alCambiarEntrega: (Long?) -> Unit,
    alCambiarPrioridad: (Prioridad) -> Unit,
    alCambiarEstado: (Estado) -> Unit,
) {
    val actual = hoja ?: return
    ModalBottomSheet(
        onDismissRequest = alDescartar,
        shape = FormaNotally.xl,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
    ) {
        when (actual) {
            HojaEditor.CURSO -> ContenidoSelectorCurso(
                cursos = cursos,
                idSeleccionado = borrador.cursoId,
                colorPorDefecto = colorCursoPorDefecto,
                error = errorNuevoCurso,
                alSeleccionar = {
                    alSeleccionarCurso(it)
                    alDescartar()
                },
                alCrear = alCrearCurso,
                alLimpiarError = alLimpiarErrorCurso,
            )

            HojaEditor.ENTREGA -> ContenidoEntrega(
                entregaEn = borrador.entregaEn,
                alCambiarEntrega = alCambiarEntrega,
                alCerrar = alDescartar,
            )

            HojaEditor.PRIORIDAD -> {
                TituloHoja(stringResource(R.string.editor_property_priority))
                Prioridad.entries.forEach { prioridad ->
                    OptionRow(
                        etiqueta = etiquetaPrioridad(prioridad),
                        seleccionado = borrador.prioridad == prioridad,
                        alPulsar = {
                            alCambiarPrioridad(prioridad)
                            alDescartar()
                        },
                        mostrarPuntoError = prioridad == Prioridad.ALTA,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            HojaEditor.ESTADO -> {
                TituloHoja(stringResource(R.string.editor_property_state))
                Estado.entries.forEach { estado ->
                    OptionRow(
                        etiqueta = etiquetaEstado(estado),
                        seleccionado = borrador.estado == estado,
                        alPulsar = {
                            alCambiarEstado(estado)
                            alDescartar()
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TituloHoja(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun ContenidoSelectorCurso(
    cursos: List<Curso>,
    idSeleccionado: Long?,
    colorPorDefecto: Long,
    error: ErrorCursoEditor?,
    alSeleccionar: (Long?) -> Unit,
    alCrear: (String, Long) -> Unit,
    alLimpiarError: () -> Unit,
) {
    var creando by rememberSaveable { mutableStateOf(false) }
    var nombre by rememberSaveable { mutableStateOf("") }

    TituloHoja(stringResource(R.string.editor_property_subject))
    OptionRow(
        etiqueta = stringResource(R.string.task_noSubject),
        seleccionado = idSeleccionado == null,
        alPulsar = { alSeleccionar(null) },
    )
    cursos.forEach { curso ->
        OptionRow(
            etiqueta = curso.nombre,
            seleccionado = idSeleccionado == curso.id,
            alPulsar = { alSeleccionar(curso.id) },
        )
    }
    if (!creando) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    alLimpiarError()
                    creando = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.subjects_new))
            }
        }
    } else {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    if (error != null) alLimpiarError()
                },
                label = { Text(stringResource(R.string.subject_name)) },
                singleLine = true,
                isError = error != null,
                supportingText = when (error) {
                    ErrorCursoEditor.DUPLICADO -> {
                        { Text(stringResource(R.string.subject_name_duplicate)) }
                    }

                    ErrorCursoEditor.GENERICO -> {
                        { Text(stringResource(R.string.subject_save_error)) }
                    }

                    null -> null
                },
                shape = FormaNotally.s,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { creando = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
                TextButton(
                    onClick = { alCrear(nombre.trim(), colorPorDefecto) },
                    enabled = nombre.isNotBlank(),
                ) {
                    Text(stringResource(R.string.action_add))
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContenidoEntrega(
    entregaEn: Long?,
    alCambiarEntrega: (Long?) -> Unit,
    alCerrar: () -> Unit,
) {
    val contexto = LocalContext.current
    val zona = remember { ZoneId.systemDefault() }
    val es24Horas = remember(contexto) { AndroidDateFormat.is24HourFormat(contexto) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    var mostrarSelectorHora by remember { mutableStateOf(false) }

    val textoFecha = entregaEn?.let { textoEtiquetaFecha(it, zona = zona) }
    val textoHora = entregaEn?.let { DateFormat.formatearHora(it, es24Horas, zona) }
    val tieneHora = entregaEn != null && DateFormat.tieneHora(entregaEn, zona)

    TituloHoja(stringResource(R.string.editor_property_due))
    Text(
        text = when {
            entregaEn == null -> stringResource(R.string.task_noDate)
            textoHora != null -> "$textoFecha · $textoHora"
            else -> textoFecha.orEmpty()
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
    )
    OptionRow(
        etiqueta = stringResource(R.string.task_noDate),
        seleccionado = entregaEn == null,
        alPulsar = {
            alCambiarEntrega(null)
            alCerrar()
        },
    )
    OptionRow(
        etiqueta = stringResource(R.string.editor_due_pick_date),
        seleccionado = false,
        alPulsar = { mostrarSelectorFecha = true },
    )
    if (entregaEn != null) {
        OptionRow(
            etiqueta = stringResource(R.string.editor_due_pick_time),
            seleccionado = false,
            alPulsar = { mostrarSelectorHora = true },
        )
        if (tieneHora) {
            OptionRow(
                etiqueta = stringResource(R.string.editor_due_clear_time),
                seleccionado = false,
                alPulsar = {
                    val fecha = Instant.ofEpochMilli(entregaEn).atZone(zona).toLocalDate()
                    alCambiarEntrega(
                        fecha.atStartOfDay(zona).toInstant().toEpochMilli(),
                    )
                },
            )
        }
    }
    Spacer(Modifier.height(8.dp))

    if (mostrarSelectorFecha) {
        val inicial = entregaEn?.let {
            Instant.ofEpochMilli(it).atZone(zona).toLocalDate()
        }
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = inicial
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val seleccionada = estadoFecha.selectedDateMillis
                        if (seleccionada != null) {
                            val fecha = Instant.ofEpochMilli(seleccionada)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            alCambiarEntrega(combinarFechaYHora(entregaEn, fecha, zona))
                        }
                        mostrarSelectorFecha = false
                    },
                    enabled = estadoFecha.selectedDateMillis != null,
                ) {
                    Text(stringResource(R.string.action_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFecha = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = estadoFecha)
        }
    }

    if (mostrarSelectorHora) {
        val zoned = entregaEn?.let { Instant.ofEpochMilli(it).atZone(zona) }
        val estadoHora = rememberTimePickerState(
            initialHour = zoned?.hour ?: 9,
            initialMinute = zoned?.minute ?: 0,
            is24Hour = es24Horas,
        )
        Dialog(
            onDismissRequest = { mostrarSelectorHora = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                shape = FormaNotally.xl,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.editor_time_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = estadoHora)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { mostrarSelectorHora = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        TextButton(
                            onClick = {
                                val base = entregaEn ?: return@TextButton
                                val fecha = Instant.ofEpochMilli(base)
                                    .atZone(zona)
                                    .toLocalDate()
                                val conHora = fecha.atTime(estadoHora.hour, estadoHora.minute)
                                    .atZone(zona)
                                    .toInstant()
                                    .toEpochMilli()
                                alCambiarEntrega(conHora)
                                mostrarSelectorHora = false
                            },
                        ) {
                            Text(stringResource(R.string.action_accept))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun etiquetaPrioridad(prioridad: Prioridad): String = stringResource(
    when (prioridad) {
        Prioridad.ALTA -> R.string.priority_high
        Prioridad.MEDIA -> R.string.priority_medium
        Prioridad.BAJA -> R.string.priority_low
    },
)

@Composable
private fun etiquetaEstado(estado: Estado): String = stringResource(
    when (estado) {
        Estado.PENDIENTE -> R.string.state_pending
        Estado.EN_CURSO -> R.string.state_doing
        Estado.COMPLETADA -> R.string.state_done
    },
)

/**
 * Combina la fecha elegida con la hora que ya tuviera la tarea; si no había
 * hora, la medianoche local (la UI muestra solo la fecha).
 */
internal fun combinarFechaYHora(
    entregaEnExistente: Long?,
    fecha: LocalDate,
    zona: ZoneId,
): Long {
    val hora = entregaEnExistente?.let {
        val zoned = Instant.ofEpochMilli(it).atZone(zona)
        LocalTime.of(zoned.hour, zoned.minute)
    } ?: LocalTime.MIDNIGHT
    return fecha.atTime(hora).atZone(zona).toInstant().toEpochMilli()
}
