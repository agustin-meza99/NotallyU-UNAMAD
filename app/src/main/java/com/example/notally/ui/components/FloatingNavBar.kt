package com.example.notally.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.TemaNotally

/** Alto de la píldora; el FAB de 52 dp queda centrado dentro de ella (§6). */
private val AltoPildora = 56.dp

/** Target de cada destino: 48 × 48 dp (§9.1). */
private val TargetDestino = 48.dp

/** Cápsula de selección: 40 dp de alto, deja 4 dp de aire dentro del target. */
private val AltoIndicador = 40.dp

/** Ancho de la cápsula: 24 dp de icono + 10 dp de aire a cada lado. */
private val AnchoIndicador = 44.dp

/** Radio de la cápsula = mitad del alto: extremos completamente curvos. */
private val RadioIndicador = 20.dp

/** Hueco entre destinos; el paso entre slots consecutivos es target + hueco. */
private val SeparacionDestinos = 8.dp

/** Paso horizontal entre slots: 48 dp de target + 8 dp de hueco (§6). */
private val PasoDestino = TargetDestino + SeparacionDestinos

/** Aire a cada lado de la cápsula dentro del target: (48 − 44) / 2 = 2 dp. */
private val AireIndicador = (TargetDestino - AnchoIndicador) / 2

/** Nivel del cluster en §4.3. */
private val ElevacionCluster = 3.dp

/**
 * Un destino del cluster de navegación (§6).
 *
 * [etiqueta] **no se dibuja**: es el `contentDescription` del destino, porque el
 * patrón aprobado es de solo iconos (§9.2). El componente no conoce `Destino` ni
 * el grafo de navegación.
 */
data class FloatingNavItem(
    val etiqueta: String,
    val icono: Painter,
    val seleccionado: Boolean,
    val alPulsar: () -> Unit,
)

/**
 * Cluster inferior flotante: píldora de navegación + FAB opcional (§6, §7.10).
 *
 * **Contrato estable:** es la navegación activa de la app; `NotallyRoot` lo
 * compone y le pasa [items], [accionPrincipal], [modifier] y
 * [descripcionAccionPrincipal]. El componente no conoce `Destino` ni el grafo
 * de navegación.
 *
 * No lee `WindowInsets`: los márgenes laterales (20/24 dp), el margen inferior
 * (16 dp + inset de navegación) y el ancho máximo de composición (720 dp en
 * ventanas ≥ 600 dp) los aplica el root por [modifier], que es lo que mantiene
 * el componente independiente de su contenedor.
 *
 * Si [accionPrincipal] es `null` no se compone el FAB; la píldora queda centrada
 * y el cluster conserva sus 56 dp de alto, así que aparecer o desaparecer el FAB
 * **no mueve la píldora ni cambia la altura**.
 *
 * El indicador de selección es **uno solo** y viaja entre slots con la aritmética
 * de §6, relativa a la fila de slots: no depende de dónde esté la píldora ni de
 * si hay FAB.
 */
@Composable
fun FloatingNavBar(
    items: List<FloatingNavItem>,
    accionPrincipal: (() -> Unit)?,
    modifier: Modifier = Modifier,
    descripcionAccionPrincipal: String = stringResource(R.string.a11y_newTask),
) {
    if (items.isEmpty()) return

    // Una sola capa de layout: la píldora está en el flujo (centrada) y el FAB se
    // ancla al borde final sin compartir fila con ella. Si compartieran `Row`, el
    // FAB empujaría el centro de la píldora en cada cambio de pantalla.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AltoPildora),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .shadow(elevation = ElevacionCluster, shape = CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .height(AltoPildora)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            // Un solo indicador compartido, dibujado debajo de la fila para que los
            // iconos queden por encima durante el recorrido (§6).
            val indiceSeleccionado = items.indexOfFirst { it.seleccionado }
            if (indiceSeleccionado >= 0) {
                IndicadorSeleccion(indice = indiceSeleccionado)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(SeparacionDestinos),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    DestinoCluster(item = item)
                }
            }
        }

        // Durante los 150 ms de salida `accionPrincipal` ya es null; se conserva
        // la última acción para que el botón no se vacíe mientras se desvanece.
        val accionRecordada = remember { mutableStateOf<(() -> Unit)?>(null) }
        accionPrincipal?.let { accionRecordada.value = it }

        AnimatedVisibility(
            visible = accionPrincipal != null,
            enter = fadeIn(tween(MovimientoNotally.fabMs)) +
                scaleIn(
                    animationSpec = tween(MovimientoNotally.fabMs),
                    initialScale = 0.9f,
                ),
            exit = fadeOut(tween(MovimientoNotally.fabMs)) +
                scaleOut(
                    animationSpec = tween(MovimientoNotally.fabMs),
                    targetScale = 0.9f,
                ),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            accionRecordada.value?.let { accion ->
                BotonAccionPrincipal(
                    alPulsar = accion,
                    descripcion = descripcionAccionPrincipal,
                )
            }
        }
    }
}

/**
 * Indicador único de selección (§6, §13.6).
 *
 * La x se calcula con la aritmética de los slots fijos: `indice * (48 + 8) + 2`,
 * relativa a la fila de slots. No se mide con `onGloballyPositioned`: los tres
 * slots miden lo mismo y la píldora está fija en `Alignment.Center`, así que
 * medir solo añadiría un frame de retraso y saltos al recomponer. En la primera
 * composición `animateDpAsState`
 * arranca en el valor destino, de modo que no hay barrido de entrada desde el
 * índice 0. Con la escala de animación del sistema en 0, Compose salta directo
 * al destino (§9).
 */
@Composable
private fun IndicadorSeleccion(indice: Int) {
    val desplazamiento by animateDpAsState(
        targetValue = PasoDestino * indice + AireIndicador,
        animationSpec = MovimientoNotally.espacialNormal(),
        label = "clusterIndicador",
    )

    Box(
        modifier = Modifier
            .offset(x = desplazamiento)
            .size(width = AnchoIndicador, height = AltoIndicador)
            .clip(RoundedCornerShape(RadioIndicador))
            .background(MaterialTheme.colorScheme.primary),
    )
}

@Composable
private fun DestinoCluster(item: FloatingNavItem) {
    val colores = MaterialTheme.colorScheme
    val seleccionada = stringResource(R.string.state_selected)
    val noSeleccionada = stringResource(R.string.state_unselected)
    var enfocado by remember { mutableStateOf(false) }
    val interaccion = remember { MutableInteractionSource() }
    val formaIndicador = RoundedCornerShape(RadioIndicador)
    val indication = remember(colores.onSurfaceVariant) {
        ripple(color = colores.onSurfaceVariant)
    }

    // El relleno vive en el indicador compartido; aquí solo cambia el tinte. Se
    // usa el mismo spring espacial que el desplazamiento para que el color del
    // icono no llegue antes que la cápsula (§5, §6).
    val tinteIcono by animateColorAsState(
        targetValue = if (item.seleccionado) colores.onPrimary else colores.onSurfaceVariant,
        animationSpec = MovimientoNotally.espacialNormal(),
        label = "clusterIcono",
    )

    // El gesto y el target viven en el contenedor de 48 dp; el dibujo no cambia de
    // tamaño al pulsar ni al cambiar de selección.
    Box(
        modifier = Modifier
            .size(TargetDestino)
            .onFocusChanged { enfocado = it.isFocused }
            .selectable(
                selected = item.seleccionado,
                interactionSource = interaccion,
                // Sin indication en el slot de 48 dp: si el ripple se dibuja aquí
                // sale un cuadrado de esquinas rectas sobre la píldora.
                indication = null,
                role = Role.Tab,
                onClick = item.alPulsar,
            )
            .semantics {
                contentDescription = item.etiqueta
                stateDescription = if (item.seleccionado) {
                    seleccionada
                } else {
                    noSeleccionada
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Única indication del destino: la cápsula de 44 × 40 recortada a radio 20.
        // El target de 48 dp conserva el gesto; solo cambia dónde se pinta el ripple.
        Box(
            modifier = Modifier
                .size(width = AnchoIndicador, height = AltoIndicador)
                .clip(formaIndicador)
                .indication(interaccion, indication),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = item.icono,
                contentDescription = null,
                tint = tinteIcono,
                modifier = Modifier.size(24.dp),
            )
        }

        // Anillo pegado a la cápsula: con el offset de 2 dp del token, el trazo
        // exterior se recorta contra el clip de la píldora en los destinos extremos.
        Box(
            modifier = Modifier
                .size(width = AnchoIndicador, height = AltoIndicador)
                .anilloFoco(
                    visible = enfocado,
                    shape = formaIndicador,
                    offset = 0.dp,
                ),
        )
    }
}

@Composable
private fun BotonAccionPrincipal(
    alPulsar: () -> Unit,
    descripcion: String,
) {
    val colores = MaterialTheme.colorScheme
    var enfocado by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(52.dp)
            .onFocusChanged { enfocado = it.isFocused }
            .anilloFoco(visible = enfocado, shape = CircleShape)
            .shadow(elevation = ElevacionCluster, shape = CircleShape)
            .clip(CircleShape)
            .background(colores.primary)
            .clickable(
                role = Role.Button,
                onClick = alPulsar,
            )
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = colores.onPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview(name = "Cluster con FAB", showBackground = true)
@Composable
private fun VistaPreviaClusterConFab() {
    VistaPreviaCluster(conAccionPrincipal = true)
}

@Preview(name = "Cluster sin FAB", showBackground = true)
@Composable
private fun VistaPreviaClusterSinFab() {
    VistaPreviaCluster(conAccionPrincipal = false)
}

/**
 * Tercera pestaña activa desde el primer frame: sirve para comprobar que el
 * indicador nace en su slot y no barre desde el índice 0.
 */
@Preview(name = "Cluster con Ajustes activo", showBackground = true)
@Composable
private fun VistaPreviaClusterTercera() {
    VistaPreviaCluster(conAccionPrincipal = true, indiceSeleccionado = 2)
}

/**
 * Los dos estados lado a lado a 320 dp, el ancho más estrecho de V1: la píldora
 * debe caer en la misma caja en ambos paneles y el cluster medir 56 dp de alto,
 * aunque el FAB desaparezca y el hueco hasta él se cierre a 0 dp.
 */
@Preview(
    name = "Cluster 320 dp: con y sin FAB",
    widthDp = 656,
    heightDp = 200,
    showBackground = true,
)
@Composable
private fun VistaPreviaClusterComparativa() {
    TemaNotally {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PanelCluster320(conAccionPrincipal = true)
            PanelCluster320(conAccionPrincipal = false)
        }
    }
}

@Composable
private fun PanelCluster320(conAccionPrincipal: Boolean) {
    Box(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        FloatingNavBar(
            items = elementosCluster(indiceSeleccionado = 0),
            accionPrincipal = if (conAccionPrincipal) ({}) else null,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun VistaPreviaCluster(
    conAccionPrincipal: Boolean,
    indiceSeleccionado: Int = 0,
) {
    TemaNotally {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            FloatingNavBar(
                items = elementosCluster(indiceSeleccionado = indiceSeleccionado),
                accionPrincipal = if (conAccionPrincipal) ({}) else null,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun elementosCluster(indiceSeleccionado: Int): List<FloatingNavItem> = listOf(
    FloatingNavItem(
        etiqueta = stringResource(R.string.nav_hoy),
        icono = painterResource(R.drawable.ic_checklist),
        seleccionado = indiceSeleccionado == 0,
        alPulsar = {},
    ),
    FloatingNavItem(
        etiqueta = stringResource(R.string.nav_cursos),
        icono = painterResource(R.drawable.ic_book),
        seleccionado = indiceSeleccionado == 1,
        alPulsar = {},
    ),
    FloatingNavItem(
        etiqueta = stringResource(R.string.nav_ajustes),
        icono = painterResource(R.drawable.ic_settings),
        seleccionado = indiceSeleccionado == 2,
        alPulsar = {},
    ),
)
