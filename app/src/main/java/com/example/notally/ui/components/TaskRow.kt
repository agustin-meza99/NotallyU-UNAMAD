package com.example.notally.ui.components

import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.data.model.Task
import com.example.notally.ui.theme.NotallyMotion
import com.example.notally.ui.theme.NotallyShape
import com.example.notally.ui.theme.subjectColor
import com.example.notally.ui.util.DateFormat
import com.example.notally.ui.util.dateLabelText
import com.example.notally.ui.util.duePhraseText
import com.example.notally.ui.util.overdueText

/**
 * Fila de tarea (§7.2).
 *
 * El checkbox es un nodo de accesibilidad separado (nunca se fusiona con la
 * fila). La fila entera abre el editor y expone las acciones de accesibilidad
 * Completar / Posponer a mañana / Eliminar.
 *
 * @param now instante de referencia para las etiquetas de fecha; la pantalla lo
 *   actualiza periódicamente para que "Hoy"/"Atrasada" no se queden obsoletos.
 */
@Composable
fun TaskRow(
    task: Task,
    subjectName: String?,
    subjectColorArgb: Long?,
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis(),
    onClick: () -> Unit = {},
    onToggleDone: () -> Unit = {},
    onPostpone: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val isDone = task.status == Status.DONE
    val isOverdue = !isDone && task.dueAt != null &&
        DateFormat.overdueDays(task.dueAt, now) > 0

    val context = LocalContext.current
    val is24Hour = remember(context) { AndroidDateFormat.is24HourFormat(context) }
    val haptics = LocalHapticFeedback.current

    val subjectText = subjectName ?: stringResource(R.string.task_noSubject)
    val separator = stringResource(R.string.task_meta_separator)
    val noDate = stringResource(R.string.task_noDate)
    val dateText = when {
        task.dueAt == null -> noDate
        isDone -> dateLabelText(task.dueAt, now)
        else -> overdueText(DateFormat.overdueDays(task.dueAt, now))
            ?: dateLabelText(task.dueAt, now)
    }
    val dateColor = if (isOverdue) colors.error else colors.onSurfaceVariant
    val subjectDotColor = subjectColorArgb?.let { subjectColor(it) } ?: colors.outline

    val priorityText = when (task.priority) {
        Priority.HIGH -> stringResource(R.string.a11y_priority_high)
        Priority.MEDIUM -> stringResource(R.string.a11y_priority_medium)
        Priority.LOW -> stringResource(R.string.a11y_priority_low)
    }
    val dueDescription = task.dueAt?.let {
        stringResource(R.string.a11y_task_due, duePhraseText(it, now, is24Hour))
    }
    val rowDescription = remember(task.title, subjectText, dueDescription, priorityText, isDone) {
        buildList {
            add(task.title)
            add(subjectText)
            if (dueDescription != null) add(dueDescription)
            add(priorityText)
        }.joinToString(". ") + "."
    }

    val openLabel = stringResource(R.string.a11y_openTask)
    val completeLabel = stringResource(R.string.swipe_complete)
    val postponeLabel = stringResource(R.string.task_postpone)
    val deleteLabel = stringResource(R.string.action_delete)
    val checkboxLabel = stringResource(
        if (isDone) R.string.a11y_uncompleteTask else R.string.a11y_completeTask,
        task.title,
    )
    val checkboxState = stringResource(
        if (isDone) R.string.state_done else R.string.state_pending,
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed) colors.surfaceContainerLow else Color.Transparent,
        animationSpec = tween(NotallyMotion.colorMs),
        label = "taskRowBackground",
    )
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .focusRing(visible = focused, shape = NotallyShape.row)
            .clip(NotallyShape.row)
            .background(background)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = openLabel,
                onClick = onClick,
            )
            .semantics {
                contentDescription = rowDescription
                customActions = listOf(
                    CustomAccessibilityAction(completeLabel) {
                        if (!isDone) onToggleDone()
                        true
                    },
                    CustomAccessibilityAction(postponeLabel) {
                        onPostpone()
                        true
                    },
                    CustomAccessibilityAction(deleteLabel) {
                        onDelete()
                        true
                    },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Target 48×48; dibujo 20 dp.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    role = Role.Checkbox,
                    onClickLabel = checkboxLabel,
                    onClick = {
                        if (!isDone) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onToggleDone()
                    },
                )
                .semantics {
                    contentDescription = checkboxLabel
                    stateDescription = checkboxState
                },
            contentAlignment = Alignment.Center,
        ) {
            TaskCheckbox(isDone = isDone)
        }

        Spacer(Modifier.width(4.dp))
        PriorityBar(priority = task.priority, isDone = isDone)
        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isDone) colors.onSurfaceVariant else colors.onSurface,
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            subjectDotColor,
                        ),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = buildAnnotatedString {
                        append(subjectText)
                        append(separator)
                        withStyle(SpanStyle(color = dateColor)) { append(dateText) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Barra de prioridad de 2 dp. El hueco de 20 dp se reserva siempre para que
 * marcar/desmarcar no cambie la altura de la fila (sin jitter).
 */
@Composable
private fun PriorityBar(priority: Priority, isDone: Boolean) {
    val colors = MaterialTheme.colorScheme
    val height = when {
        isDone || priority == Priority.LOW -> 0.dp
        priority == Priority.HIGH -> 20.dp
        else -> 12.dp
    }
    val color = when {
        isDone || priority == Priority.LOW -> Color.Transparent
        priority == Priority.HIGH -> colors.error
        else -> colors.outline
    }
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(20.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        if (height > 0.dp) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(height)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** Checkbox circular de 20 dp con confirmación de estado (§5, patrón 3). */
@Composable
private fun TaskCheckbox(isDone: Boolean) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        targetValue = if (isDone) colors.primary else Color.Transparent,
        animationSpec = tween(NotallyMotion.colorMs),
        label = "checkboxFill",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDone) colors.primary else colors.outline,
        animationSpec = tween(NotallyMotion.colorMs),
        label = "checkboxBorder",
    )
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = isDone,
            enter = fadeIn(tween(NotallyMotion.colorMs)) +
                scaleIn(
                    animationSpec = NotallyMotion.spatialFast(),
                    initialScale = 0.9f,
                ),
            exit = fadeOut(tween(80)),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
