package com.example.notally.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notally.R
import com.example.notally.data.aula.ErrorAula
import com.example.notally.ui.components.anilloFoco
import com.example.notally.ui.theme.EspaciadoNotally
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.margenHorizontalPantalla
import com.example.notally.ui.utils.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pantalla Settings (§7.12).
 *
 * Ajustes de agenda, importación del Aula Virtual y versión; el tema es
 * siempre claro y no se configura.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(LocalContext.current),
    ),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val estadoAula by viewModel.estadoAula.collectAsStateWithLifecycle()
    val margen = margenHorizontalPantalla
    val contexto = LocalContext.current
    val tituloPantalla = stringResource(R.string.nav_ajustes)

    var hojaAulaVisible by rememberSaveable { mutableStateOf(false) }
    var confirmarDesconexion by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.eventosAula.collect { evento ->
            when (evento) {
                is EventoAula.Importado -> {
                    hojaAulaVisible = false
                    val mensaje = if (evento.errores > 0) {
                        contexto.getString(
                            R.string.settings_aula_summary_errors,
                            evento.cursos,
                            evento.tareas,
                            evento.errores,
                        )
                    } else {
                        contexto.getString(
                            R.string.settings_aula_summary,
                            evento.cursos,
                            evento.tareas,
                        )
                    }
                    snackbar.showSnackbar(mensaje)
                }
            }
        }
    }

    val nombreVersion = remember(contexto) {
        runCatching {
            contexto.packageManager.getPackageInfo(contexto.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 24.dp)
            .semantics { paneTitle = tituloPantalla },
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = EspaciadoNotally.anchoMaximoContenido)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = margen, end = margen, bottom = 32.dp),
        ) {
            item(key = "agenda_label") {
                EtiquetaSeccion(stringResource(R.string.settings_agenda))
            }
            item(key = "week_row") {
                FilaInterruptorSettings(
                    etiqueta = stringResource(R.string.settings_week),
                    soporte = stringResource(R.string.settings_week_support),
                    marcado = settings.semanaEmpiezaLunes,
                    alCambiar = viewModel::cambiarSemanaEmpiezaLunes,
                )
            }

            item(key = "aula_label") {
                EtiquetaSeccion(
                    stringResource(R.string.settings_aula),
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            if (estadoAula.conectado) {
                item(key = "aula_connected_row") {
                    FilaAulaConectada(
                        estado = estadoAula,
                        alImportar = viewModel::importarAulaAhora,
                        alDesconectar = { confirmarDesconexion = true },
                    )
                }
            } else {
                item(key = "aula_connect_row") {
                    FilaConectarAula(
                        habilitada = !estadoAula.cargandoInicial && !estadoAula.importando,
                        alPulsar = {
                            viewModel.descartarErrorAula()
                            hojaAulaVisible = true
                        },
                    )
                }
            }

            if (estadoAula.importando) {
                item(key = "aula_loading") {
                    AulaCargando(
                        progreso = estadoAula.progreso,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            estadoAula.error?.let { error ->
                item(key = "aula_error") {
                    AulaError(
                        error = error,
                        alReintentar = if (error.reintentable) {
                            viewModel::reintentarAula
                        } else {
                            null
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            item(key = "about_label") {
                EtiquetaSeccion(
                    stringResource(R.string.settings_about),
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            item(key = "version_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_version, nombreVersion),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = margen, vertical = 16.dp),
        )
    }

    if (hojaAulaVisible) {
        HojaConectarAula(
            estado = estadoAula,
            alConectar = viewModel::conectarAula,
            alDescartar = {
                hojaAulaVisible = false
                viewModel.descartarErrorAula()
            },
        )
    }

    if (confirmarDesconexion) {
        AlertDialog(
            onDismissRequest = { confirmarDesconexion = false },
            shape = FormaNotally.xl,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = {
                Text(
                    text = stringResource(R.string.settings_aula_disconnect_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_aula_disconnect_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            dismissButton = {
                TextButton(onClick = { confirmarDesconexion = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarDesconexion = false
                        viewModel.desconectarAula()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.settings_aula_disconnect))
                }
            },
        )
    }
}

@Composable
private fun FilaConectarAula(
    habilitada: Boolean,
    alPulsar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    var enfocada by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (habilitada) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = alPulsar,
                    )
                } else {
                    Modifier
                },
            )
            .onFocusChanged { enfocada = it.isFocused }
            .then(
                if (habilitada) {
                    Modifier.anilloFoco(visible = enfocada, shape = FormaNotally.s)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_aula_connect),
                style = MaterialTheme.typography.bodyLarge,
                color = if (habilitada) colores.onSurface else colores.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_aula_connect_support),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilaAulaConectada(
    estado: EstadoAula,
    alImportar: () -> Unit,
    alDesconectar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_aula_connected),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colores.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = estado.ultimaImportacion?.let { millis ->
                        stringResource(
                            R.string.settings_aula_last_import,
                            formatearUltimaImportacion(millis),
                        )
                    } ?: stringResource(R.string.settings_aula_never_imported),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = alImportar,
                enabled = !estado.importando,
            ) {
                Text(stringResource(R.string.settings_aula_import_now))
            }
            TextButton(
                onClick = alDesconectar,
                enabled = !estado.importando,
                colors = ButtonDefaults.textButtonColors(contentColor = colores.error),
            ) {
                Text(stringResource(R.string.settings_aula_disconnect))
            }
        }
    }
}

@Composable
private fun AulaCargando(
    progreso: com.example.notally.data.aula.ProgresoAula?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            text = progreso?.let {
                stringResource(
                    R.string.settings_aula_progress,
                    it.cursoNombre,
                    it.cursoActual,
                    it.cursosTotales,
                )
            } ?: stringResource(R.string.settings_aula_importing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AulaError(
    error: ErrorAulaUi,
    alReintentar: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = mensajeErrorAula(error),
            style = MaterialTheme.typography.bodySmall,
            color = colores.error,
            modifier = Modifier.weight(1f),
        )
        if (alReintentar != null) {
            TextButton(
                onClick = alReintentar,
                colors = ButtonDefaults.textButtonColors(contentColor = colores.error),
            ) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
private fun mensajeErrorAula(error: ErrorAulaUi): String {
    val delServidor = error.mensajeServidor?.takeIf { it.isNotBlank() }
    if (delServidor != null) return delServidor
    return when (error.tipo) {
        ErrorAula.CREDENCIALES -> stringResource(R.string.settings_aula_error_credentials)
        ErrorAula.BLOQUEO -> stringResource(R.string.settings_aula_error_blocked)
        ErrorAula.RED -> stringResource(R.string.settings_aula_error_network)
        ErrorAula.SESION_CADUCADA -> stringResource(R.string.settings_aula_error_session)
        ErrorAula.SIN_CURSOS -> stringResource(R.string.settings_aula_error_no_courses)
        ErrorAula.SERVIDOR -> stringResource(R.string.settings_aula_error_server)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaConectarAula(
    estado: EstadoAula,
    alConectar: (usuario: String, clave: String) -> Unit,
    alDescartar: () -> Unit,
) {
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    val importando = estado.importando
    val puedeConectar = usuario.isNotBlank() && clave.isNotBlank() && !importando

    ModalBottomSheet(
        onDismissRequest = alDescartar,
        shape = FormaNotally.xl,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_aula_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = usuario,
                onValueChange = { usuario = it },
                label = { Text(stringResource(R.string.settings_aula_user)) },
                singleLine = true,
                enabled = !importando,
                shape = FormaNotally.s,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = clave,
                onValueChange = { clave = it },
                label = { Text(stringResource(R.string.settings_aula_password)) },
                singleLine = true,
                enabled = !importando,
                shape = FormaNotally.s,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (puedeConectar) alConectar(usuario, clave)
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_aula_credentials_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_aula_password_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (importando) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(20.dp).height(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = estado.progreso?.let {
                            stringResource(
                                R.string.settings_aula_progress,
                                it.cursoNombre,
                                it.cursoActual,
                                it.cursosTotales,
                            )
                        } ?: stringResource(R.string.settings_aula_importing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            estado.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                AulaError(
                    error = error,
                    alReintentar = if (error.reintentable && puedeConectar) {
                        { alConectar(usuario, clave) }
                    } else {
                        null
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = alDescartar,
                    enabled = !importando,
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = { alConectar(usuario, clave) },
                    enabled = puedeConectar,
                ) {
                    Text(stringResource(R.string.settings_aula_connect_import))
                }
            }
        }
    }
}

@Composable
private fun EtiquetaSeccion(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = texto.uppercase(DateFormat.LocaleEspanol),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun FilaInterruptorSettings(
    etiqueta: String,
    soporte: String?,
    marcado: Boolean,
    alCambiar: (Boolean) -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    var enfocada by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .anilloFoco(visible = enfocada, shape = FormaNotally.s)
            .onFocusChanged { enfocada = it.isFocused }
            .toggleable(
                value = marcado,
                role = Role.Switch,
                onValueChange = alCambiar,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.bodyLarge,
                color = colores.onSurface,
            )
            if (soporte != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = soporte,
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        // El switch no maneja el cambio: la fila completa es el target de 48 dp.
        Switch(
            checked = marcado,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                uncheckedTrackColor = colores.surfaceVariant,
                uncheckedBorderColor = colores.outline,
                uncheckedThumbColor = colores.outline,
            ),
        )
    }
}

private val formatoUltimaImportacion: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", DateFormat.LocaleEspanol)

private fun formatearUltimaImportacion(millis: Long): String =
    formatoUltimaImportacion.format(
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()),
    )
