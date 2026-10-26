package com.example.notally.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notally.R
import com.example.notally.data.model.ModoTema
import com.example.notally.ui.components.FilaOpcion
import com.example.notally.ui.components.anilloFoco
import com.example.notally.ui.theme.EspaciadoNotally
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.margenHorizontalPantalla
import com.example.notally.ui.util.FormatoFecha

/**
 * Pantalla Ajustes (§7.12).
 *
 * Cambiar el tema no reinicia la pantalla ni pierde el scroll: el estado del
 * `LazyColumn` y el `ViewModel` viven por encima del `TemaNotally`.
 */
@Composable
fun PantallaAjustes(
    modifier: Modifier = Modifier,
    viewModel: AjustesViewModel = viewModel(
        factory = AjustesViewModel.factory(LocalContext.current),
    ),
) {
    val ajustes by viewModel.ajustes.collectAsStateWithLifecycle()
    val margen = margenHorizontalPantalla
    val contexto = LocalContext.current
    var mostrarHojaTema by remember { mutableStateOf(false) }

    val dinamicoSoportado = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val nombreVersion = remember(contexto) {
        runCatching {
            contexto.packageManager.getPackageInfo(contexto.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = EspaciadoNotally.anchoMaximoContenido)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = margen, end = margen, bottom = 32.dp),
        ) {
            item(key = "title") {
                Text(
                    text = stringResource(R.string.nav_ajustes),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .semantics { heading() },
                )
            }

            item(key = "appearance_label") {
                EtiquetaSeccion(stringResource(R.string.settings_appearance))
            }
            item(key = "theme_row") {
                FilaAjustes(
                    etiqueta = stringResource(R.string.settings_theme),
                    valor = etiquetaTema(ajustes.modoTema),
                    alPulsar = { mostrarHojaTema = true },
                )
            }
            item(key = "divider_appearance") { SeparadorFila() }
            item(key = "dynamic_row") {
                FilaInterruptorAjustes(
                    etiqueta = stringResource(R.string.settings_dynamic),
                    soporte = stringResource(
                        if (dinamicoSoportado) {
                            R.string.settings_dynamic_support
                        } else {
                            R.string.settings_dynamic_unsupported
                        },
                    ),
                    marcado = ajustes.usarColorDinamico && dinamicoSoportado,
                    activo = dinamicoSoportado,
                    alCambiar = viewModel::cambiarColorDinamico,
                )
            }

            item(key = "agenda_label") {
                EtiquetaSeccion(
                    stringResource(R.string.settings_agenda),
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            item(key = "week_row") {
                FilaInterruptorAjustes(
                    etiqueta = stringResource(R.string.settings_week),
                    soporte = stringResource(R.string.settings_week_support),
                    marcado = ajustes.semanaEmpiezaLunes,
                    activo = true,
                    alCambiar = viewModel::cambiarSemanaEmpiezaLunes,
                )
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
    }

    if (mostrarHojaTema) {
        HojaTema(
            actual = ajustes.modoTema,
            alDescartar = { mostrarHojaTema = false },
            alSeleccionar = { modo ->
                viewModel.cambiarModoTema(modo)
                mostrarHojaTema = false
            },
        )
    }
}

@Composable
private fun EtiquetaSeccion(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = texto.uppercase(FormatoFecha.LocaleEspanol),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SeparadorFila() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun FilaAjustes(
    etiqueta: String,
    valor: String? = null,
    soporte: String? = null,
    alPulsar: (() -> Unit)? = null,
) {
    val colores = MaterialTheme.colorScheme
    var enfocada by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .anilloFoco(visible = enfocada, shape = FormaNotally.s)
            .onFocusChanged { enfocada = it.isFocused }
            .then(
                if (alPulsar != null) {
                    Modifier.clickable(role = Role.Button, onClick = alPulsar)
                } else {
                    Modifier
                },
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
        if (valor != null) {
            Spacer(Modifier.width(16.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilaInterruptorAjustes(
    etiqueta: String,
    soporte: String?,
    marcado: Boolean,
    activo: Boolean,
    alCambiar: (Boolean) -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val alfa = if (activo) 1f else 0.38f
    var enfocada by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .anilloFoco(visible = enfocada, shape = FormaNotally.s)
            .onFocusChanged { enfocada = it.isFocused }
            .toggleable(
                value = marcado,
                enabled = activo,
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
                color = colores.onSurface.copy(alpha = alfa),
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
            enabled = activo,
            colors = SwitchDefaults.colors(
                uncheckedTrackColor = colores.surfaceVariant,
                uncheckedBorderColor = colores.outline,
                uncheckedThumbColor = colores.outline,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaTema(
    actual: ModoTema,
    alDescartar: () -> Unit,
    alSeleccionar: (ModoTema) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = alDescartar,
        shape = FormaNotally.xl,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
    ) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
        )
        ModoTema.entries.forEach { modo ->
            FilaOpcion(
                etiqueta = etiquetaTema(modo),
                seleccionado = actual == modo,
                alPulsar = { alSeleccionar(modo) },
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun etiquetaTema(modo: ModoTema): String = stringResource(
    when (modo) {
        ModoTema.SISTEMA -> R.string.settings_system
        ModoTema.CLARO -> R.string.settings_light
        ModoTema.OSCURO -> R.string.settings_dark
    },
)
