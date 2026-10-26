package com.example.notally.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.ui.theme.NotallyMotion
import com.example.notally.ui.theme.NotallyShape
import com.example.notally.ui.util.DateFormat
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Un día de la tira semanal (§7.1.1).
 *
 * [sectionLabelRes] es el nombre de la sección destino ("Atrasadas", "Para
 * hoy", "Próximas"); `null` cuando el día no tiene pendientes y por tanto no es
 * clicable.
 */
data class DayStripDay(
    val date: LocalDate,
    val pendingCount: Int,
    val isToday: Boolean,
    val sectionLabelRes: Int?,
)

/**
 * Tira de siete días de la semana en curso (§7.1.1).
 *
 * Celdas de 48 × 64 dp mínimo que crecen con la fuente, gap de 8 dp y scroll
 * horizontal sin barra. Cada celda con pendientes es un botón independiente;
 * la selección nunca se comunica solo con `primaryContainer`: añade borde y
 * cambia el color del texto.
 */
@Composable
fun DayStrip(
    days: List<DayStripDay>,
    selectedDate: LocalDate?,
    onDayClick: (DayStripDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var stripX by remember { mutableStateOf(0f) }
    var stripWidth by remember { mutableStateOf(0) }
    var todayX by remember { mutableStateOf<Float?>(null) }
    var todayWidth by remember { mutableStateOf(0) }
    var centeredToday by remember { mutableStateOf(false) }

    // La celda de hoy se lleva al centro en la primera composición, sin
    // animación (§7.1.1).
    LaunchedEffect(days, stripWidth, todayX, scrollState.maxValue) {
        val cellX = todayX ?: return@LaunchedEffect
        if (centeredToday || stripWidth <= 0) return@LaunchedEffect
        val cellCenter = cellX - stripX + todayWidth / 2f
        val target = (cellCenter - stripWidth / 2f).roundToInt()
            .coerceIn(0, scrollState.maxValue)
        scrollState.scrollTo(target)
        centeredToday = true
    }

    Row(
        modifier = modifier
            .onGloballyPositioned {
                stripX = it.positionInRoot().x
                stripWidth = it.size.width
            }
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        days.forEach { day ->
            DayCell(
                day = day,
                selected = day.date == selectedDate,
                onClick = { onDayClick(day) },
                modifier = if (day.isToday) {
                    Modifier.onGloballyPositioned {
                        todayX = it.positionInRoot().x
                        todayWidth = it.size.width
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
    day: DayStripDay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }

    val container by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else Color.Transparent,
        animationSpec = tween(NotallyMotion.colorMs),
        label = "dayCellContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.onSurfaceVariant else Color.Transparent,
        animationSpec = tween(NotallyMotion.colorMs),
        label = "dayCellBorder",
    )
    val weekdayColor by animateColorAsState(
        targetValue = if (selected) {
            colors.onPrimaryContainer
        } else {
            colors.onSurfaceVariant
        },
        animationSpec = tween(NotallyMotion.colorMs),
        label = "dayCellWeekday",
    )
    val numberColor by animateColorAsState(
        targetValue = when {
            selected -> colors.onPrimaryContainer
            day.sectionLabelRes == null -> colors.onSurfaceVariant
            else -> colors.onSurface
        },
        animationSpec = tween(NotallyMotion.colorMs),
        label = "dayCellNumber",
    )
    val barColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimaryContainer else colors.outline,
        animationSpec = tween(NotallyMotion.colorMs),
        label = "dayCellBar",
    )

    val pendingText = if (day.pendingCount == 0) {
        stringResource(R.string.today_pending_none_label)
    } else {
        pluralStringResource(
            R.plurals.today_pending_count,
            day.pendingCount,
            day.pendingCount,
        )
    }
    val label = stringResource(
        R.string.a11y_week_day,
        DateFormat.weekdayName(day.date),
        day.date.dayOfMonth,
        DateFormat.monthName(day.date),
        pendingText,
    )
    val actionLabel = day.sectionLabelRes?.let { sectionRes ->
        stringResource(R.string.a11y_week_open, stringResource(sectionRes))
    }
    val selectedState = stringResource(R.string.a11y_week_selected)

    val clickModifier = if (actionLabel != null) {
        Modifier.clickable(
            role = Role.Button,
            onClickLabel = actionLabel,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .widthIn(min = 48.dp)
            .heightIn(min = 64.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusRing(visible = focused, shape = NotallyShape.m)
            .clip(NotallyShape.m)
            .background(container)
            .border(1.dp, borderColor, NotallyShape.m)
            .then(clickModifier)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                if (selected) stateDescription = selectedState
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = DateFormat.weekdayAbbreviation(day.date),
                style = MaterialTheme.typography.labelSmall,
                color = weekdayColor,
                maxLines = 1,
            )
            if (day.isToday) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(colors.tertiary),
                )
            }
        }
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = numberColor,
            maxLines = 1,
        )
        // Área inferior de 8 dp reservada siempre: mostrar u ocultar la barra
        // no cambia la altura de la celda.
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (day.pendingCount > 0) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(barColor),
                )
            }
        }
    }
}
