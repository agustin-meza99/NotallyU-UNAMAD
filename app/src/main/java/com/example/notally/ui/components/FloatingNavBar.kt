package com.example.notally.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
 * Cluster inferior flotante: solo la píldora de navegación (§6).
 *
 * **Contrato estable:** es la navegación activa de la app; `NotallyRoot` lo
 * compone y le pasa [items] y [modifier]. El componente no conoce `Destino` ni
 * el grafo de navegación, y no compone el FAB: la acción principal vive en el
 * overlay del root para que sea una sola instancia y pueda desplazarse entre
 * destinos sin que la píldora cambie de tamaño ni de centro.
 *
 * No lee `WindowInsets`: los márgenes laterales (20/24 dp), el margen inferior
 * (16 dp + inset de navegación) y el ancho máximo de composición (720 dp en
 * ventanas ≥ 600 dp) los aplica el root por [modifier], que es lo que mantiene
 * el componente independiente de su contenedor.
 *
 * El indicador de selección es **uno solo** y viaja entre slots con la aritmética
 * de §6, relativa a la fila de slots.
 */
@Composable
fun FloatingNavBar(
    items: List<FloatingNavItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AltoPildora),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .shadow(elevation = ElevacionCluster, shape = CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .height(AltoPildora)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
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

    val tinteIcono by animateColorAsState(
        targetValue = if (item.seleccionado) colores.onPrimary else colores.onSurfaceVariant,
        animationSpec = MovimientoNotally.espacialNormal(),
        label = "clusterIcono",
    )

    Box(
        modifier = Modifier
            .size(TargetDestino)
            .onFocusChanged { enfocado = it.isFocused }
            .selectable(
                selected = item.seleccionado,
                interactionSource = interaccion,
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

@Preview(name = "Cluster", showBackground = true)
@Composable
private fun VistaPreviaCluster() {
    VistaPreviaCluster(indiceSeleccionado = 0)
}

/**
 * Tercera pestaña activa desde el primer frame: sirve para comprobar que el
 * indicador nace en su slot y no barre desde el índice 0.
 */
@Preview(name = "Cluster con Ajustes activo", showBackground = true)
@Composable
private fun VistaPreviaClusterTercera() {
    VistaPreviaCluster(indiceSeleccionado = 2)
}

@Composable
private fun VistaPreviaCluster(indiceSeleccionado: Int) {
    TemaNotally {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            FloatingNavBar(
                items = elementosCluster(indiceSeleccionado = indiceSeleccionado),
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
