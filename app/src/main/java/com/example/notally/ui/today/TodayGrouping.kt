package com.example.notally.ui.today

import com.example.notally.data.DateRanges
import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.data.model.Subject
import com.example.notally.data.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Filtro de Hoy (§7.7). No se persiste entre sesiones. */
enum class TodayFilter { ALL, PENDING, DONE }

/** Secciones de Hoy, en el orden del documento (§7.3). */
enum class TodaySectionType { OVERDUE, TODAY, ANYTIME, UPCOMING, COMPLETED }

/** Tarea con su materia resuelta, lista para pintar una fila. */
data class TodayItem(
    val task: Task,
    val subject: Subject?,
)

/** Sección ya resuelta para la UI. */
data class TodaySectionUi(
    val type: TodaySectionType,
    val items: List<TodayItem>,
    val expanded: Boolean,
    val collapsible: Boolean,
    val count: Int?,
)

/**
 * Resultado puro de agrupar las tareas de Hoy.
 *
 * - Atrasadas / Para hoy / Sin hora / Próximas: solo pendientes.
 * - Completadas: tareas hechas de la agenda de hoy (con o sin hora).
 * - completedHistory: todo el historial, para el filtro "Completadas".
 * - summary: conteo del día (Atrasadas + Para hoy + Sin hora); Próximas fuera.
 */
data class TodayGroups(
    val overdue: List<TodayItem> = emptyList(),
    val today: List<TodayItem> = emptyList(),
    val anytime: List<TodayItem> = emptyList(),
    val upcoming: List<TodayItem> = emptyList(),
    val completed: List<TodayItem> = emptyList(),
    val completedHistory: List<TodayItem> = emptyList(),
    val summaryCompleted: Int = 0,
    val summaryTotal: Int = 0,
) {
    val isEmpty: Boolean
        get() = overdue.isEmpty() && today.isEmpty() && anytime.isEmpty() &&
            upcoming.isEmpty() && completed.isEmpty()

    val hasPending: Boolean
        get() = overdue.isNotEmpty() || today.isNotEmpty() || anytime.isNotEmpty() ||
            upcoming.isNotEmpty()

    val pendingAgendaEmpty: Boolean
        get() = overdue.isEmpty() && today.isEmpty() && anytime.isEmpty()
}

/**
 * Agrupa por fecha local y estado. Función pura y testeable, sin Android.
 *
 * @param dayTasks tareas con `dueAt` dentro de hoy (incluye completadas).
 * @param upcomingTasks todo el futuro desde mañana (sin tope superior; incluye
 *   completadas). La consulta no recorta a siete días.
 * @param activeTasks tareas no archivadas; se usan para "Sin hora", atrasadas e
 *   historial de completadas.
 */
fun groupToday(
    now: Long,
    zone: ZoneId,
    dayTasks: List<Task>,
    upcomingTasks: List<Task>,
    activeTasks: List<Task>,
    subjectsById: Map<Long, Subject>,
): TodayGroups {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val todayStart = DateRanges.dayRange(today, zone).first
    val tomorrowStart = DateRanges.dayRange(today.plusDays(1), zone).first

    fun item(task: Task) = TodayItem(
        task = task,
        subject = task.subjectId?.let(subjectsById::get),
    )

    fun isPending(task: Task) = task.status != Status.DONE

    val active = activeTasks.filter { !it.isArchived }
    val noDate = active.filter { it.dueAt == null }

    val overdue = active
        .filter { isPending(it) && it.dueAt != null && it.dueAt < todayStart }
        .sortedWith(dueComparator)
        .map(::item)

    val todayPending = dayTasks
        .filter { isPending(it) && it.dueAt != null }
        .sortedWith(dueComparator)
        .map(::item)

    val anytime = noDate
        .filter(::isPending)
        .sortedWith(anytimeComparator)
        .map(::item)

    val upcoming = upcomingTasks
        .filter { isPending(it) && it.dueAt != null && it.dueAt >= tomorrowStart }
        .sortedWith(dueComparator)
        .map(::item)

    val noDateCompletedToday = noDate.filter { task ->
        !isPending(task) && task.completedAt?.let {
            toLocalDate(it, zone) == today
        } == true
    }

    val completed = (dayTasks.filter { !isPending(it) } + noDateCompletedToday)
        .distinctBy { it.id }
        .sortedWith(completedComparator)
        .map(::item)

    val completedHistory = active
        .filter { !isPending(it) }
        .sortedWith(completedComparator)
        .map(::item)

    val noDateInSummary = noDate.count { task ->
        isPending(task) || task.completedAt?.let { toLocalDate(it, zone) == today } == true
    }

    return TodayGroups(
        overdue = overdue,
        today = todayPending,
        anytime = anytime,
        upcoming = upcoming,
        completed = completed,
        completedHistory = completedHistory,
        summaryCompleted = completed.size,
        summaryTotal = overdue.size + dayTasks.size + noDateInSummary,
    )
}

private fun toLocalDate(epochMillis: Long, zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

/**
 * Construye las secciones visibles según el filtro y el estado de expansión.
 *
 * Por defecto "Próximas" se abre si no hay nada en la agenda de hoy; y
 * "Completadas" se abre si, además, no hay próximas (para no dejar la pantalla
 * vacía, §7.3). Las preferencias manuales del usuario mandan sobre el default.
 */
fun buildTodaySections(
    groups: TodayGroups,
    filter: TodayFilter,
    overrides: Map<TodaySectionType, Boolean> = emptyMap(),
): List<TodaySectionUi> {
    if (filter == TodayFilter.DONE) return emptyList()

    val upcomingDefault = groups.pendingAgendaEmpty && groups.upcoming.isNotEmpty()
    val completedDefault = groups.pendingAgendaEmpty && groups.upcoming.isEmpty()

    fun section(
        type: TodaySectionType,
        items: List<TodayItem>,
        collapsible: Boolean,
        expanded: Boolean,
        count: Int? = null,
    ) = TodaySectionUi(
        type = type,
        items = items,
        expanded = if (collapsible) overrides[type] ?: expanded else true,
        collapsible = collapsible,
        count = count,
    )

    return buildList {
        if (groups.overdue.isNotEmpty()) {
            add(section(TodaySectionType.OVERDUE, groups.overdue, false, true))
        }
        if (groups.today.isNotEmpty()) {
            add(section(TodaySectionType.TODAY, groups.today, false, true))
        }
        if (groups.anytime.isNotEmpty()) {
            add(section(TodaySectionType.ANYTIME, groups.anytime, false, true))
        }
        if (groups.upcoming.isNotEmpty()) {
            add(
                section(
                    type = TodaySectionType.UPCOMING,
                    items = groups.upcoming,
                    collapsible = true,
                    expanded = upcomingDefault,
                    count = groups.upcoming.size,
                ),
            )
        }
        if (filter != TodayFilter.PENDING && groups.completed.isNotEmpty()) {
            add(
                section(
                    type = TodaySectionType.COMPLETED,
                    items = groups.completed,
                    collapsible = true,
                    expanded = completedDefault,
                    count = groups.completed.size,
                ),
            )
        }
    }
}

private val priorityRank: (Task) -> Int = {
    when (it.priority) {
        Priority.HIGH -> 0
        Priority.MEDIUM -> 1
        Priority.LOW -> 2
    }
}

private fun titleKey(task: Task): String = task.title.lowercase(Locale.forLanguageTag("es"))

private val dueComparator: Comparator<Task> =
    compareBy<Task> { it.dueAt ?: Long.MAX_VALUE }
        .thenBy(priorityRank)
        .thenBy(::titleKey)

private val anytimeComparator: Comparator<Task> =
    compareBy<Task> { it.createdAt }
        .thenBy(priorityRank)
        .thenBy(::titleKey)

private val completedComparator: Comparator<Task> =
    compareByDescending<Task> { it.completedAt ?: it.updatedAt }
        .thenBy(priorityRank)
        .thenBy(::titleKey)
