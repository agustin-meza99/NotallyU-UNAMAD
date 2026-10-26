package com.example.notally.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.ui.theme.NotallyMotion
import com.example.notally.ui.util.DateFormat

/**
 * Encabezado de sección (§7.3): `labelSmall` uppercase, contador opcional y
 * chevron de 20 dp con rotación de 180 ms cuando la sección es colapsable.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    collapsible: Boolean = false,
    expanded: Boolean = true,
    onToggle: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(NotallyMotion.chevronMs),
        label = "sectionChevron",
    )
    val label = buildString {
        append(title.uppercase(DateFormat.SpanishLocale))
        if (count != null) {
            append(" · ")
            append(count)
        }
    }
    val toggleable = collapsible && onToggle != null
    val expandedState = stringResource(R.string.section_expanded)
    val collapsedState = stringResource(R.string.section_collapsed)
    val expandLabel = stringResource(R.string.a11y_expand_section)
    val collapseLabel = stringResource(R.string.a11y_collapse_section)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(
                if (toggleable) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = if (expanded) collapseLabel else expandLabel,
                        onClick = onToggle,
                    )
                } else {
                    Modifier
                },
            )
            .semantics {
                heading()
                if (toggleable) {
                    stateDescription = if (expanded) expandedState else collapsedState
                }
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (collapsible) {
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation),
            )
        }
    }
}
