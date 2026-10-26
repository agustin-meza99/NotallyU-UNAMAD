package com.example.notally.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {

    @Test
    fun `la semana empieza en lunes por defecto`() {
        val settings = Settings()

        assertEquals(true, settings.semanaEmpiezaLunes)
    }
}
