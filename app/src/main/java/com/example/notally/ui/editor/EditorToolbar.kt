package com.example.notally.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.ui.theme.FormaNotally

@Composable
internal fun EditorToolbar(
    alPulsarSubtarea: () -> Unit,
    alPulsarEntrega: () -> Unit,
    alPulsarPrioridad: () -> Unit,
) {
    val compacto = LocalConfiguration.current.fontScale >= 1.5f
    Surface(
        shape = FormaNotally.l,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.height(56.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarAction(
                recursoIcono = R.drawable.ic_checklist,
                etiqueta = stringResource(R.string.editor_toolbar_subtask),
                compacto = compacto,
                alPulsar = alPulsarSubtarea,
            )
            ToolbarAction(
                recursoIcono = R.drawable.ic_calendar,
                etiqueta = stringResource(R.string.editor_toolbar_date),
                compacto = compacto,
                alPulsar = alPulsarEntrega,
            )
            ToolbarAction(
                recursoIcono = R.drawable.ic_flag,
                etiqueta = stringResource(R.string.editor_property_priority),
                compacto = compacto,
                alPulsar = alPulsarPrioridad,
            )
        }
    }
}

@Composable
private fun ToolbarAction(
    recursoIcono: Int,
    etiqueta: String,
    compacto: Boolean,
    alPulsar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(FormaNotally.s)
            .clickable(role = Role.Button, onClick = alPulsar)
            .then(
                if (compacto) {
                    Modifier.semantics { contentDescription = etiqueta }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(recursoIcono),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        if (!compacto) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
