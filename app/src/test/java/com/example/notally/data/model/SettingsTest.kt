package com.example.notally.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {

    @Test
    fun `la semana empieza en lunes y el tema sigue al sistema por defecto`() {
        val settings = Settings()

        assertEquals(true, settings.semanaEmpiezaLunes)
        assertEquals(TemaApp.SISTEMA, settings.tema)
    }
}
