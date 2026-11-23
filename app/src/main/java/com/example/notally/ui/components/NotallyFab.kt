package com.example.notally.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.unamad

/**
 * FAB principal de NotallyU UNAMAD: 54 dp circular con gradiente verde
 * amazónico y micro-animación de escala táctil.
 */
@Composable
fun NotallyFab(
    alPulsar: () -> Unit,
    descripcion: String,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val unamadColors = MaterialTheme.unamad
    var enfocado by remember { mutableStateOf(false) }
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val pulsado by fuenteInteraccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pulsado) 0.92f else 1f,
        animationSpec = MovimientoNotally.espacialRapida(),
        label = "fabScale",
    )

    Box(
        modifier = modifier
            .size(54.dp)
            .scale(escala)
            .onFocusChanged { enfocado = it.isFocused }
            .anilloFoco(visible = enfocado, shape = CircleShape)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(colores.primary, unamadColors.heroEnd),
                ),
            )
            .clickable(
                interactionSource = fuenteInteraccion,
                indication = null,
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
