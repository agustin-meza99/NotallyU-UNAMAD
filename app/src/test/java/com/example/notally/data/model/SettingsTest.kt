package com.example.notally.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    @Test
    fun `los valores por defecto son agenda y pestana validas`() {
        val settings = Settings()

        assertEquals(Settings.PESTANA_HOY, settings.pestanaPorDefecto)
        assertTrue(settings.pestanaPorDefecto in Settings.PESTANAS_VALIDAS)
        assertEquals(true, settings.semanaEmpiezaLunes)
        assertEquals(true, settings.notificacionesActivas)
    }
}
