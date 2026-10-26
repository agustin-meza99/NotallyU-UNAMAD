package com.example.notally.ui.today

import com.example.notally.data.DateRanges
import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.data.model.Subject
import com.example.notally.data.model.Task
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
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

/** Día de la tira semanal (§7.1.1) con su número de pendientes. */
data class TodayWeekDay(
    val date: LocalDate,
    val pendingCount: Int,
)

/** Tarea destacada (§7.3.1): id estable, instantánea vigente y su bucket. */
data class TodayFocalTask(
    val taskId: Long,
    val item: TodayItem,
    val bucket: TodaySectionType,
)

/** Conteos de los chips de filtro (§7.7), calculados antes de extraer la focal. */
data class TodayFilterCounts(
    val all: Int = 0,
    val pending: Int = 0,
    val done: Int = 0,
)

/**
 * Resultado puro de agrupar las tareas de Hoy.
 *
 * - Atrasadas / Para hoy / Sin hora / Próximas: solo pendientes y ya sin la
 *   tarea focal, para no duplicar su fila.
 * - Completadas: tareas hechas de la agenda de hoy (con o sin hora).
 * - completedHistory: todo el historial, para el filtro "Completadas".
 * - summary: conteo del día (Atrasadas + Para hoy + Sin hora); Próximas fuera.
 * - filterCounts: lo que cuenta cada chip; se mide sobre las listas completas,
 *   así que la focal entra una sola vez. `all` sigue siendo el conteo de las
 *   secciones de la vista por secciones (§7.7), no el de "Completadas".
 * - isEmpty: no hay nada que mostrar en ningún filtro (ni secciones ni
 *   historial); `hasSectionContent` responde solo por las secciones.
 */
data class TodayGroups(
    val overdue: List<TodayItem> = emptyList(),
    val today: List<TodayItem> = emptyList(),
    val anytime: List<TodayItem> = emptyList(),
    val upcoming: List<TodayItem> = emptyList(),
    val completed: List<TodayItem> = emptyList(),
    val completedHistory: List<TodayItem> = emptyList(),
    val weekDays: List<TodayWeekDay> = emptyList(),
    val focal: TodayFocalTask? = null,
    val filterCounts: TodayFilterCounts = TodayFilterCounts(),
    val summaryCompleted: Int = 0,
    val summaryTotal: Int = 0,
    private val agendaPendingCount: Int = 0,
    private val upcomingPendingCount: Int = 0,
) {
    /** Nada que mostrar en ningún filtro: ni secciones ni historial. */
    val isEmpty: Boolean
        get() = filterCounts.all == 0 && completedHistory.isEmpty()

    /** Hay al menos una fila (o la focal) en las secciones de Hoy. */
    val hasSectionContent: Boolean
        get() = filterCounts.all > 0

    val hasPending: Boolean
        get() = filterCounts.pending > 0

    val pendingAgendaEmpty: Boolean
        get() = agendaPendingCount == 0

    val hasUpcomingPending: Boolean
        get() = upcomingPendingCount > 0
}

/**
 * Agrupa por fecha local y estado. Función pura y testeable, sin Android.
 *
 * @param dayTasks tareas con `dueAt` dentro de hoy (incluye completadas).
 * @param upcomingTasks todo el futuro desde mañana (sin tope superior; incluye
 *   completadas). La consulta no recorta a siete días.
 * @param activeTasks tareas no archivadas; se usan para "Sin hora", atrasadas,
 *   historial de completadas y el conteo de la tira semanal.
 * @param weekStartsMonday ajuste "La semana inicia en lunes" (§7.12).
 */
fun groupToday(
    now: Long,
    zone: ZoneId,
    dayTasks: List<Task>,
    upcomingTasks: List<Task>,
    activeTasks: List<Task>,
    subjectsById: Map<Long, Subject>,
    weekStartsMonday: Boolean = true,
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

    val overdueItems = active
        .filter { isPending(it) && it.dueAt != null && it.dueAt < todayStart }
        .sortedWith(dueComparator)
        .map(::item)

    val todayItems = dayTasks
        .filter { isPending(it) && it.dueAt != null }
        .sortedWith(dueComparator)
        .map(::item)

    val anytimeItems = noDate
        .filter(::isPending)
        .sortedWith(anytimeComparator)
        .map(::item)

    val upcomingItems = upcomingTasks
        .filter { isPending(it) && it.dueAt != null && it.dueAt >= tomorrowStart }
        .sortedWith(dueComparator)
        .map(::item)

    val noDateCompletedToday = noDate.filter { task ->
        !isPending(task) && task.completedAt?.let {
            toLocalDate(it, zone) == today
        } == true
    }

    val completedItems = (dayTasks.filter { !isPending(it) } + noDateCompletedToday)
        .distinctBy { it.id }
        .sortedWith(completedComparator)
        .map(::item)

    val completedHistoryItems = active
        .filter { !isPending(it) }
        .sortedWith(completedComparator)
        .map(::item)

    val noDateInSummary = noDate.count { task ->
        isPending(task) || task.completedAt?.let { toLocalDate(it, zone) == today } == true
    }

    // Conteos de los chips (§7.7): sobre las listas completas, sin extraer la
    // focal, de modo que cada número coincide con lo que verá el usuario.
    val pendingCount = (overdueItems + todayItems + anytimeItems + upcomingItems)
        .distinctBy { it.task.id }
        .size
    val allCount = (overdueItems + todayItems + anytimeItems + upcomingItems + completedItems)
        .distinctBy { it.task.id }
        .size
    val doneCount = completedHistoryItems.distinctBy { it.task.id }.size

    val focal = selectFocal(
        overdue = overdueItems,
        today = todayItems,
        anytime = anytimeItems,
        upcoming = upcomingItems,
    )
    val filtered = if (focal == null) {
        null
    } else {
        focal.taskId
    }

    return TodayGroups(
        overdue = overdueItems.filterNot { it.task.id == filtered },
        today = todayItems.filterNot { it.task.id == filtered },
        anytime = anytimeItems.filterNot { it.task.id == filtered },
        upcoming = upcomingItems.filterNot { it.task.id == filtered },
        completed = completedItems,
        completedHistory = completedHistoryItems,
        weekDays = buildWeekDays(today, zone, weekStartsMonday, active),
        focal = focal,
        filterCounts = TodayFilterCounts(
            all = allCount,
            pending = pendingCount,
            done = doneCount,
        ),
        summaryCompleted = completedItems.size,
        summaryTotal = overdueItems.size + dayTasks.size + noDateInSummary,
        agendaPendingCount = overdueItems.size + todayItems.size + anytimeItems.size,
        upcomingPendingCount = upcomingItems.size,
    )
}

/**
 * Elige la tarea focal de forma determinista (§7.3.1): bucket Atrasadas > Para
 * hoy > Sin hora > Próximas; dentro, prioridad, fecha, creación e id.
 */
private fun selectFocal(
    overdue: List<TodayItem>,
    today: List<TodayItem>,
    anytime: List<TodayItem>,
    upcoming: List<TodayItem>,
): TodayFocalTask? {
    val buckets = listOf(
        TodaySectionType.OVERDUE to overdue,
        TodaySectionType.TODAY to today,
        TodaySectionType.ANYTIME to anytime,
        TodaySectionType.UPCOMING to upcoming,
    )
    buckets.forEach { (type, items) ->
        val best = items.minWithOrNull(focalComparator) ?: return@forEach
        return TodayFocalTask(taskId = best.task.id, item = best, bucket = type)
    }
    return null
}

private val focalComparator: Comparator<TodayItem> =
    compareBy<TodayItem> { priorityRank(it.task) }
        .thenBy { it.task.dueAt ?: Long.MAX_VALUE }
        .thenBy { it.task.createdAt }
        .thenBy { it.task.id }

/** Siete días de la semana en curso, con sus pendientes con fecha. */
private fun buildWeekDays(
    today: LocalDate,
    zone: ZoneId,
    weekStartsMonday: Boolean,
    active: List<Task>,
): List<TodayWeekDay> {
    val firstDay = if (weekStartsMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDay))
    val pendingWithDate = active.filter { it.status != Status.DONE && it.dueAt != null }
    return (0L..6L).map { offset ->
        val date = weekStart.plusDays(offset)
        val range = DateRanges.dayRange(date, zone)
        TodayWeekDay(
            date = date,
            pendingCount = pendingWithDate.count { task ->
                task.dueAt?.let { it in range } == true
            },
        )
    }
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

    val upcomingDefault = groups.pendingAgendaEmpty && groups.hasUpcomingPending
    val completedDefault = groups.pendingAgendaEmpty && !groups.hasUpcomingPending

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
