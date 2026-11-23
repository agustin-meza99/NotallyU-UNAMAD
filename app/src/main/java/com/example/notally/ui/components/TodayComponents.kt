package com.example.notally.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.data.firebase.UsuarioFirebase
import com.example.notally.data.model.Prioridad
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.TipografiaNotally
import com.example.notally.ui.theme.unamad
import com.example.notally.ui.today.ResumenHoy
import com.example.notally.ui.today.SaludoDia
import com.example.notally.ui.today.saludoSegunHora
import com.example.notally.ui.utils.DateFormat
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Encabezado Hero institucional UNAMAD para Hoy.
 *
 * Presenta identidad universitaria (verde amazónico + acento dorado), saludo
 * contextual según la hora del día, avatar interactivo, anillo de progreso
 * animado y métricas rápidas (pendientes, completadas y promedio de notas).
 */
@Composable
fun TodayHeroBanner(
    antetitulo: String,
    ahora: Long,
    resumen: ResumenHoy?,
    promedioNotas: Double?,
    usuario: UsuarioFirebase?,
    alPulsarPerfil: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unamadColors = MaterialTheme.unamad
    val saludo = when (saludoSegunHora(ahora)) {
        SaludoDia.MANANA -> stringResource(R.string.greeting_morning)
        SaludoDia.TARDE -> stringResource(R.string.greeting_afternoon)
        SaludoDia.NOCHE -> stringResource(R.string.greeting_evening)
    }
    val primerNombre = usuario?.displayName
        ?.trim()
        ?.substringBefore(" ")
        ?.takeIf { it.isNotBlank() }
        ?: usuario?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.greeting_student)

    val objetivoFraccion = if (resumen != null && resumen.total > 0) {
        (resumen.completadas.toFloat() / resumen.total).coerceIn(0f, 1f)
    } else {
        0f
    }
    val fraccionAnimada by animateFloatAsState(
        targetValue = objetivoFraccion,
        animationSpec = MovimientoNotally.espacialNormal(),
        label = "heroProgress",
    )
    val porcentaje = (fraccionAnimada * 100f).roundToInt()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(FormaNotally.xl)
            .background(
                Brush.linearGradient(
                    colors = listOf(unamadColors.heroStart, unamadColors.heroEnd),
                ),
            )
            .border(
                width = 1.dp,
                color = unamadColors.goldAccent.copy(alpha = 0.35f),
                shape = FormaNotally.xl,
            )
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Fila superior: badge institucional UNAMAD + fecha + avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(unamadColors.heroBadgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_school),
                        contentDescription = null,
                        tint = unamadColors.heroBadgeContent,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.brand_unamad_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = unamadColors.heroBadgeContent,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (usuario != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = alPulsarPerfil),
                    ) {
                        AvatarCircle(usuario = usuario)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Fila principal: saludo + fecha + anillo de avance animado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$saludo, $primerNombre",
                        style = MaterialTheme.typography.titleLarge,
                        color = unamadColors.heroOnContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (antetitulo.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = antetitulo,
                            style = MaterialTheme.typography.labelSmall,
                            color = unamadColors.heroOnContent.copy(alpha = 0.78f),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val subtitulo = when {
                        resumen != null && resumen.total > 0 && resumen.completadas >= resumen.total ->
                            stringResource(R.string.today_hero_all_done)

                        resumen != null -> pluralStringResource(
                            R.plurals.today_summary,
                            resumen.total,
                            resumen.completadas,
                            resumen.total,
                        )

                        else -> stringResource(R.string.today_hero_start_prompt)
                    }
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = unamadColors.heroOnContent.copy(alpha = 0.90f),
                    )
                }

                if (resumen != null && resumen.total > 0) {
                    Spacer(Modifier.width(14.dp))
                    Box(
                        modifier = Modifier.size(58.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = { fraccionAnimada },
                            modifier = Modifier.fillMaxSize(),
                            color = unamadColors.goldAccent,
                            trackColor = unamadColors.heroOnContent.copy(alpha = 0.18f),
                            strokeWidth = 5.dp,
                            strokeCap = StrokeCap.Round,
                        )
                        Text(
                            text = "$porcentaje%",
                            style = TipografiaNotally.numerico,
                            color = unamadColors.heroOnContent,
                        )
                    }
                }
            }

            // Píldoras de resumen rápido (solo si hay tareas o calificaciones)
            if (resumen != null || promedioNotas != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (resumen != null) {
                        val pendientes = (resumen.total - resumen.completadas).coerceAtLeast(0)
                        HeroStatPill(
                            etiqueta = stringResource(R.string.today_stat_pending),
                            valor = pendientes.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        HeroStatPill(
                            etiqueta = stringResource(R.string.today_stat_done),
                            valor = resumen.completadas.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (promedioNotas != null) {
                        HeroStatPill(
                            etiqueta = stringResource(R.string.today_stat_average),
                            valor = String.format(Locale.US, "%.1f", promedioNotas),
                            resaltar = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatPill(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    resaltar: Boolean = false,
) {
    val unamadColors = MaterialTheme.unamad
    Column(
        modifier = modifier
            .clip(FormaNotally.m)
            .background(
                if (resaltar) {
                    unamadColors.goldAccent.copy(alpha = 0.22f)
                } else {
                    unamadColors.heroOnContent.copy(alpha = 0.12f)
                },
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = valor,
            style = MaterialTheme.typography.titleMedium,
            color = if (resaltar) unamadColors.heroBadgeContent else unamadColors.heroOnContent,
            maxLines = 1,
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = unamadColors.heroOnContent.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Indicador discreto de modo offline (ISO/IEC 25010 — Fiabilidad). */
@Composable
fun OfflineStatusPill(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(MovimientoNotally.colorMs)) + expandVertically(),
        exit = fadeOut(tween(MovimientoNotally.colorMs)) + shrinkVertically(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FormaNotally.m)
                .background(colores.secondaryContainer)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cloud_off),
                contentDescription = null,
                tint = colores.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.today_offline_banner),
                style = MaterialTheme.typography.labelMedium,
                color = colores.onSecondaryContainer,
            )
        }
    }
}

/** Barra de búsqueda instantánea (ISO/IEC 25010 — Adecuación Funcional y Usabilidad). */
@Composable
fun NotallySearchBar(
    consulta: String,
    placeholder: String,
    alCambiarConsulta: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    var enfocado by remember { mutableStateOf(false) }
    val colorBorde by animateColorAsState(
        targetValue = if (enfocado) colores.primary else colores.outlineVariant,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "searchBorder",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = FormaNotally.l,
        color = colores.surfaceContainerLowest,
        border = BorderStroke(if (enfocado) 1.5.dp else 1.dp, colorBorde),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = if (enfocado) colores.primary else colores.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (consulta.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = consulta,
                    onValueChange = alCambiarConsulta,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colores.onSurface),
                    cursorBrush = SolidColor(colores.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { enfocado = it.isFocused },
                )
            }
            if (consulta.isNotEmpty()) {
                IconButton(
                    onClick = { alCambiarConsulta("") },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.today_search_clear),
                        tint = colores.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** Tarjeta de bienvenida cuando no hay sesión iniciada en Firebase. */
@Composable
fun WelcomeAuthCard(
    alIrAjustes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val unamadColors = MaterialTheme.unamad
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FormaNotally.xl,
        color = colores.surfaceContainerLow,
        border = BorderStroke(1.dp, unamadColors.goldAccent.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colores.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_school),
                        contentDescription = null,
                        tint = colores.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.today_welcome_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colores.onSurface,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.today_welcome_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = alIrAjustes,
                shape = FormaNotally.m,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colores.primary,
                    contentColor = colores.onPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.today_welcome_cta),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** Un día de la tira semanal (§7.1.1). */
data class DiaTira(
    val fecha: LocalDate,
    val pendientes: Int,
    val esHoy: Boolean,
    val recursoEtiquetaSeccion: Int?,
)

/** Tira de siete días de la semana en curso (§7.1.1). */
@Composable
fun DayStrip(
    dias: List<DiaTira>,
    fechaSeleccionada: LocalDate?,
    alElegirDia: (DiaTira) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estadoScroll = rememberScrollState()
    var tiraX by remember { mutableStateOf(0f) }
    var anchoTira by remember { mutableStateOf(0) }
    var hoyX by remember { mutableStateOf<Float?>(null) }
    var anchoHoy by remember { mutableStateOf(0) }
    var hoyCentrado by remember { mutableStateOf(false) }

    LaunchedEffect(dias, anchoTira, hoyX, estadoScroll.maxValue) {
        val celdaX = hoyX ?: return@LaunchedEffect
        if (hoyCentrado || anchoTira <= 0) return@LaunchedEffect
        val centroCelda = celdaX - tiraX + anchoHoy / 2f
        val destino = (centroCelda - anchoTira / 2f).roundToInt()
            .coerceIn(0, estadoScroll.maxValue)
        estadoScroll.scrollTo(destino)
        hoyCentrado = true
    }

    Row(
        modifier = modifier
            .onGloballyPositioned {
                tiraX = it.positionInRoot().x
                anchoTira = it.size.width
            }
            .horizontalScroll(estadoScroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        dias.forEach { dia ->
            DayCell(
                dia = dia,
                seleccionado = dia.fecha == fechaSeleccionada,
                alPulsar = { alElegirDia(dia) },
                modifier = if (dia.esHoy) {
                    Modifier.onGloballyPositioned {
                        hoyX = it.positionInRoot().x
                        anchoHoy = it.size.width
                    }
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun DayCell(
    dia: DiaTira,
    seleccionado: Boolean,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val unamadColors = MaterialTheme.unamad
    var enfocado by remember { mutableStateOf(false) }

    val esClickeable = dia.recursoEtiquetaSeccion != null
    val contenedor by animateColorAsState(
        targetValue = when {
            seleccionado -> colores.primary
            dia.esHoy -> colores.primaryContainer.copy(alpha = 0.65f)
            esClickeable -> colores.surfaceContainerLow
            else -> Color.Transparent
        },
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "dayCellContainer",
    )
    val colorBorde by animateColorAsState(
        targetValue = when {
            seleccionado -> colores.primary
            dia.esHoy -> colores.primary.copy(alpha = 0.55f)
            esClickeable -> colores.outlineVariant.copy(alpha = 0.6f)
            else -> Color.Transparent
        },
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "dayCellBorder",
    )
    val colorDiaSemana by animateColorAsState(
        targetValue = when {
            seleccionado -> colores.onPrimary.copy(alpha = 0.9f)
            dia.esHoy -> colores.onPrimaryContainer
            else -> colores.onSurfaceVariant
        },
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "dayCellWeekday",
    )
    val colorNumero by animateColorAsState(
        targetValue = when {
            seleccionado -> colores.onPrimary
            dia.esHoy -> colores.onPrimaryContainer
            dia.recursoEtiquetaSeccion == null -> colores.onSurfaceVariant
            else -> colores.onSurface
        },
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "dayCellNumber",
    )
    val colorBarra by animateColorAsState(
        targetValue = when {
            seleccionado -> unamadColors.goldAccent
            dia.esHoy -> colores.primary
            else -> unamadColors.goldAccent
        },
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "dayCellBar",
    )

    val textoPendientes = if (dia.pendientes == 0) {
        stringResource(R.string.today_pending_none_label)
    } else {
        pluralStringResource(
            R.plurals.today_pending_count,
            dia.pendientes,
            dia.pendientes,
        )
    }
    val etiqueta = stringResource(
        R.string.a11y_week_day,
        DateFormat.nombreDia(dia.fecha),
        dia.fecha.dayOfMonth,
        DateFormat.nombreMes(dia.fecha),
        textoPendientes,
    )
    val etiquetaAccion = dia.recursoEtiquetaSeccion?.let { recursoSeccion ->
        stringResource(R.string.a11y_week_open, stringResource(recursoSeccion))
    }
    val estadoSeleccionado = stringResource(R.string.a11y_week_selected)

    val modificadorClic = if (etiquetaAccion != null) {
        Modifier.clickable(
            role = Role.Button,
            onClickLabel = etiquetaAccion,
            onClick = alPulsar,
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .widthIn(min = 50.dp)
            .heightIn(min = 66.dp)
            .onFocusChanged { enfocado = it.isFocused }
            .anilloFoco(visible = enfocado, shape = FormaNotally.m)
            .clip(FormaNotally.m)
            .background(contenedor)
            .border(1.dp, colorBorde, FormaNotally.m)
            .then(modificadorClic)
            .semantics(mergeDescendants = true) {
                contentDescription = etiqueta
                if (seleccionado) stateDescription = estadoSeleccionado
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = DateFormat.abreviaturaDia(dia.fecha),
                style = MaterialTheme.typography.labelSmall,
                color = colorDiaSemana,
                maxLines = 1,
            )
            if (dia.esHoy) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (seleccionado) unamadColors.goldAccent else colores.primary),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = dia.fecha.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = colorNumero,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (dia.pendientes > 0) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(colorBarra),
                )
            }
        }
    }
}

/**
 * Tarea focal de Hoy renovada con identidad UNAMAD: fondo verde selva oscuro,
 * acento dorado institucional, insignia de prioridad y respuesta táctil fluida.
 */
@Composable
fun FocalTaskCard(
    titulo: String,
    antetitulo: String,
    textoCurso: String,
    textoEntrega: String,
    prioridad: Prioridad,
    modifier: Modifier = Modifier,
    alPulsar: () -> Unit = {},
) {
    val colores = MaterialTheme.colorScheme
    val unamadColors = MaterialTheme.unamad
    var enfocada by remember { mutableStateOf(false) }
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val pulsada by fuenteInteraccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pulsada) 0.985f else 1f,
        animationSpec = MovimientoNotally.espacialRapida(),
        label = "focalScale",
    )

    val etiquetaPrioridad = priorityLabelForA11y(prioridad)
    val etiquetaPrioridadVisible = priorityLabel(prioridad)
    val separador = stringResource(R.string.task_meta_separator)
    val descripcion = stringResource(
        R.string.a11y_featured_task,
        titulo,
        textoCurso,
        textoEntrega,
        etiquetaPrioridad,
    )
    val etiquetaAbrir = stringResource(R.string.a11y_openTask)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(escala)
            .heightIn(min = 104.dp)
            .onFocusChanged { enfocada = it.isFocused }
            .anilloFoco(visible = enfocada, shape = FormaNotally.xl)
            .clip(FormaNotally.xl)
            .background(
                Brush.linearGradient(
                    colors = listOf(unamadColors.focalStart, unamadColors.focalEnd),
                ),
            )
            .border(
                width = 1.dp,
                color = unamadColors.goldAccent.copy(alpha = 0.50f),
                shape = FormaNotally.xl,
            )
            .clickable(
                interactionSource = fuenteInteraccion,
                indication = ripple(color = colores.inverseOnSurface),
                role = Role.Button,
                onClickLabel = etiquetaAbrir,
                onClick = alPulsar,
            )
            .semantics { contentDescription = descripcion }
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(unamadColors.goldAccent.copy(alpha = 0.22f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_flag),
                    contentDescription = null,
                    tint = unamadColors.goldAccent,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "${antetitulo.uppercase(DateFormat.LocaleEspanol)} · ${etiquetaPrioridadVisible.uppercase(DateFormat.LocaleEspanol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = unamadColors.heroBadgeContent,
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colores.inverseOnSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            color = colores.inverseOnSurface,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = null,
                tint = unamadColors.goldAccent,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = textoCurso + separador + textoEntrega,
                style = MaterialTheme.typography.bodySmall,
                color = colores.inverseOnSurface.copy(alpha = 0.85f),
            )
        }
    }
}
