package com.example.notally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.notally.R

/**
 * FAB principal de la app (§6, §7.10): 52 dp circular, `primary`/`onPrimary`,
 * sombra de 3 dp e icono "+" de 24 dp, con anillo de foco.
 *
 * Es el mismo botón que Hoy incrusta en el cluster inferior y el que Cursos
 * coloca flotando sobre la barra. El llamador decide posición y descripción;
 * el estilo no se duplica.
 */
@Composable
fun NotallyFab(
    alPulsar: () -> Unit,
    descripcion: String,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    var enfocado by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(52.dp)
            .onFocusChanged { enfocado = it.isFocused }
            .anilloFoco(visible = enfocado, shape = CircleShape)
            .shadow(elevation = 3.dp, shape = CircleShape)
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
