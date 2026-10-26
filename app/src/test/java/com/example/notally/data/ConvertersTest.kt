package com.example.notally.data

import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.data.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `priority round trips through its persisted name`() {
        Priority.entries.forEach { priority ->
            val stored = converters.priorityToString(priority)
            assertEquals(priority.name, stored)
            assertEquals(priority, converters.stringToPriority(stored))
        }
    }

    @Test
    fun `status round trips through its persisted name`() {
        Status.entries.forEach { status ->
            val stored = converters.statusToString(status)
            assertEquals(status.name, stored)
            assertEquals(status, converters.stringToStatus(stored))
        }
    }

    @Test
    fun `theme mode round trips through its persisted name`() {
        ThemeMode.entries.forEach { mode ->
            val stored = converters.themeModeToString(mode)
            assertEquals(mode.name, stored)
            assertEquals(mode, converters.stringToThemeMode(stored))
        }
    }

    @Test
    fun `persisted values match the names used in SQL queries`() {
        assertEquals("DONE", converters.statusToString(Status.DONE))
        assertEquals("PENDING", converters.statusToString(Status.PENDING))
        assertEquals("HIGH", converters.priorityToString(Priority.HIGH))
    }
}
