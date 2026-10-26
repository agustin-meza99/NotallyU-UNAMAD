package com.example.notally.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectColorsTest {

    @Test
    fun `hay seis acentos opacos y distintos`() {
        val colors = SubjectColors.all

        assertEquals(6, colors.size)
        assertEquals(6, colors.distinct().size)
        assertTrue(colors.all { (it ushr 24) == 0xFFL })
    }

    @Test
    fun `los valores respetan la tabla del documento`() {
        assertEquals(
            listOf(0xFF3A5A9EL, 0xFF2F6A4EL, 0xFF99482EL, 0xFF5F7300L, 0xFF7A5F87L, 0xFF1D5F6AL),
            SubjectColors.all,
        )
    }
}
