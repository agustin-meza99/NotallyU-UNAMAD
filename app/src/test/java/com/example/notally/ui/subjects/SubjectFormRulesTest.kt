package com.example.notally.ui.subjects

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectFormRulesTest {

    @Test
    fun `guardar se reactiva tras dos fallos con el mismo resultado`() {
        val nombre = "Cálculo diferencial"

        repeat(2) {
            assertTrue(SubjectFormRules.puedeEnviar(nombre, guardando = false))
            assertFalse(SubjectFormRules.puedeEnviar(nombre, guardando = true))
            // `finally` vuelve a guardando = false aunque el error se repita.
            assertTrue(SubjectFormRules.puedeEnviar(nombre, guardando = false))
        }
        assertFalse(SubjectFormRules.termino(ResultadoGuardarMateria.DUPLICADA))
        assertFalse(SubjectFormRules.termino(ResultadoGuardarMateria.GENERICA))
    }

    @Test
    fun `solo el guardado confirmado cierra la hoja`() {
        assertTrue(SubjectFormRules.termino(ResultadoGuardarMateria.GUARDADA))
        assertFalse(SubjectFormRules.puedeEnviar("   ", guardando = false))
    }
}
