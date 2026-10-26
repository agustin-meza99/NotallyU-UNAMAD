package com.example.notally.data.remote

import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.data.model.Subtask
import com.example.notally.data.model.Task
import com.example.notally.data.remote.firestore.RemoteSubtask
import com.example.notally.data.remote.firestore.RemoteTask
import com.example.notally.data.remote.firestore.TaskLimits
import com.example.notally.data.remote.firestore.toLocal
import com.example.notally.data.remote.firestore.toLocalSubtasks
import com.example.notally.data.remote.firestore.toRemote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TaskMapperTest {

    private fun task(
        id: Long = 7L,
        title: String = "Ensayo de historia",
        notes: String = "Fuentes primarias",
        subjectId: Long? = 3L,
        dueAt: Long? = 1_700_000_000_000L,
        priority: Priority = Priority.HIGH,
        status: Status = Status.IN_PROGRESS,
        createdAt: Long = 1_600_000_000_000L,
        updatedAt: Long = 1_610_000_000_000L,
        completedAt: Long? = null,
        isArchived: Boolean = false,
    ) = Task(
        id = id,
        title = title,
        notes = notes,
        subjectId = subjectId,
        dueAt = dueAt,
        priority = priority,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        isArchived = isArchived,
    )

    @Test
    fun `task round trips through remote preserving scalar fields`() {
        val local = task()

        val remote = local.toRemote()
        val back = remote.toLocal(localId = local.id)

        assertEquals(local, back)
        assertEquals("3", remote.subjectId)
        assertEquals("HIGH", remote.priority)
        assertEquals("IN_PROGRESS", remote.status)
        assertNull(remote.deletedAt)
    }

    @Test
    fun `completed task keeps completedAt`() {
        val completedAt = 1_620_000_000_000L
        val local = task(status = Status.DONE, completedAt = completedAt)

        val back = local.toRemote().toLocal(localId = local.id)

        assertEquals(Status.DONE, back.status)
        assertEquals(completedAt, back.completedAt)
    }

    @Test
    fun `nullable fields survive a round trip as null`() {
        val local = task(
            subjectId = null,
            dueAt = null,
            completedAt = null,
        )

        val remote = local.toRemote()
        val back = remote.toLocal(localId = local.id)

        assertNull(remote.subjectId)
        assertNull(remote.dueAt)
        assertNull(remote.completedAt)
        assertNull(remote.deletedAt)
        assertNull(back.subjectId)
        assertNull(back.dueAt)
        assertNull(back.completedAt)
    }

    @Test
    fun `every priority and status round trips by enum name`() {
        Status.entries.forEach { status ->
            Priority.entries.forEach { priority ->
                val local = task(status = status, priority = priority)
                val remote = local.toRemote()
                val back = remote.toLocal(localId = local.id)

                assertEquals(priority.name, remote.priority)
                assertEquals(status.name, remote.status)
                assertEquals(priority, back.priority)
                assertEquals(status, back.status)
            }
        }
    }

    @Test
    fun `subtasks are ordered by position and preserve their content`() {
        val subtasks = listOf(
            Subtask(id = 11L, taskId = 7L, title = "Buscar fuentes", isDone = true, position = 1),
            Subtask(id = 12L, taskId = 7L, title = "Leer capitulo", isDone = false, position = 0),
        )

        val remote = task().toRemote(subtasks = subtasks)
        val back = remote.toLocalSubtasks(localTaskId = 7L)

        assertEquals(listOf("12", "11"), remote.subtasks.map { it.id })
        assertEquals(listOf("Leer capitulo", "Buscar fuentes"), back.map { it.title })
        assertEquals(listOf(0, 1), back.map { it.position })
        assertEquals(listOf(false, true), back.map { it.isDone })
    }

    @Test
    fun `subtask lists are capped at one hundred`() {
        val exactlyOneHundred = (0 until TaskLimits.MAX_SUBTASKS).map { index ->
            Subtask(id = index + 1L, taskId = 7L, title = "Subtarea $index", position = index)
        }
        assertEquals(100, task().toRemote(subtasks = exactlyOneHundred).subtasks.size)

        val tooMany = exactlyOneHundred + Subtask(
            id = 101L,
            taskId = 7L,
            title = "Una mas",
            position = 100,
        )
        assertThrows(IllegalArgumentException::class.java) {
            task().toRemote(subtasks = tooMany)
        }
    }

    @Test
    fun `title accepts 300 characters and rejects 301`() {
        assertEquals(300, task(title = "a".repeat(300)).toRemote().title.length)
        assertThrows(IllegalArgumentException::class.java) {
            task(title = "a".repeat(301)).toRemote()
        }
    }

    @Test
    fun `blank title is rejected because rules require at least one character`() {
        assertThrows(IllegalArgumentException::class.java) {
            task(title = "").toRemote()
        }
    }

    @Test
    fun `notes accept 20000 characters and reject 20001`() {
        assertEquals(20_000, task(notes = "n".repeat(20_000)).toRemote().notes.length)
        assertThrows(IllegalArgumentException::class.java) {
            task(notes = "n".repeat(20_001)).toRemote()
        }
    }

    @Test
    fun `subtask title over 300 characters is rejected`() {
        val subtask = Subtask(id = 1L, taskId = 7L, title = "s".repeat(301), position = 0)

        assertThrows(IllegalArgumentException::class.java) {
            task().toRemote(subtasks = listOf(subtask))
        }
    }

    @Test
    fun `duplicate subtask ids are rejected`() {
        val subtasks = listOf(
            Subtask(id = 1L, taskId = 7L, title = "A", position = 0),
            Subtask(id = 1L, taskId = 7L, title = "B", position = 1),
        )

        assertThrows(IllegalArgumentException::class.java) {
            task().toRemote(subtasks = subtasks)
        }
    }

    @Test
    fun `negative subtask position is rejected`() {
        val subtask = Subtask(id = 1L, taskId = 7L, title = "A", position = -1)

        assertThrows(IllegalArgumentException::class.java) {
            task().toRemote(subtasks = listOf(subtask))
        }
    }

    @Test
    fun `archived task cannot be mapped because rules have no archived field`() {
        val archived = task(isArchived = true)

        val error = assertThrows(IllegalArgumentException::class.java) {
            archived.toRemote()
        }
        assertEquals(true, error.message!!.contains("isArchived"))
    }

    @Test
    fun `tombstone cannot be mapped as a live local task`() {
        val tombstone = RemoteTask(
            title = "Borrada",
            priority = "LOW",
            status = "PENDING",
            deletedAt = 1_630_000_000_000L,
        )

        assertThrows(IllegalArgumentException::class.java) {
            tombstone.toLocal()
        }
        assertThrows(IllegalArgumentException::class.java) {
            tombstone.toLocalSubtasks(localTaskId = 7L)
        }
    }

    @Test
    fun `remote with null timestamps falls back to the provided clock`() {
        val remote = RemoteTask(
            title = "Sin fechas",
            priority = "MEDIUM",
            status = "PENDING",
            createdAt = null,
            updatedAt = null,
        )

        val local = remote.toLocal(now = 42_000L)

        assertEquals(42_000L, local.createdAt)
        assertEquals(42_000L, local.updatedAt)
    }

    @Test
    fun `remote updatedAt is preserved while a missing createdAt uses now`() {
        val remote = RemoteTask(
            title = "Parcial",
            priority = "LOW",
            status = "PENDING",
            createdAt = null,
            updatedAt = 9_000L,
        )

        val local = remote.toLocal(now = 42_000L)

        assertEquals(42_000L, local.createdAt)
        assertEquals(9_000L, local.updatedAt)
    }

    @Test
    fun `invalid priority and status strings are rejected on the way in`() {
        val badPriority = RemoteTask(title = "X", priority = "URGENT", status = "PENDING")
        val badStatus = RemoteTask(title = "X", priority = "LOW", status = "BLOCKED")

        assertThrows(IllegalArgumentException::class.java) { badPriority.toLocal() }
        assertThrows(IllegalArgumentException::class.java) { badStatus.toLocal() }
    }

    @Test
    fun `non numeric remote subjectId fails loudly instead of losing the relation`() {
        val remote = RemoteTask(
            title = "Materia futura",
            priority = "LOW",
            status = "PENDING",
            subjectId = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        )

        assertThrows(IllegalArgumentException::class.java) {
            remote.toLocal()
        }
    }

    @Test
    fun `explicit local subject id overrides the parsed remote id`() {
        val remote = RemoteTask(
            title = "Con materia",
            priority = "LOW",
            status = "PENDING",
            subjectId = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        )

        val local = remote.toLocal(localSubjectId = 5L)

        assertEquals(5L, local.subjectId)
    }

    @Test
    fun `remote subtask with an oversized title is rejected`() {
        val remote = RemoteTask(
            title = "Tarea",
            priority = "LOW",
            status = "PENDING",
            subtasks = listOf(
                RemoteSubtask(id = "s1", title = "s".repeat(301), isDone = false, position = 0),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            remote.toLocal()
        }
    }

    @Test
    fun `remote title over 300 characters is rejected`() {
        val remote = RemoteTask(
            title = "t".repeat(301),
            priority = "LOW",
            status = "PENDING",
        )

        assertThrows(IllegalArgumentException::class.java) {
            remote.toLocal()
        }
    }
}
