package com.example.notally.ui.subjects

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasFormularioMateriaTest {

    @Test
    fun `guardar se reactiva tras dos fallos con el mismo resultado`() {
        val nombre = "Cálculo diferencial"

        repeat(2) {
            assertTrue(ReglasFormularioMateria.puedeEnviar(nombre, guardando = false))
            assertFalse(ReglasFormularioMateria.puedeEnviar(nombre, guardando = true))
            // `finally` vuelve a guardando = false aunque el error se repita.
            assertTrue(ReglasFormularioMateria.puedeEnviar(nombre, guardando = false))
        }
        assertFalse(ReglasFormularioMateria.termino(ResultadoGuardarMateria.DUPLICADA))
        assertFalse(ReglasFormularioMateria.termino(ResultadoGuardarMateria.GENERICA))
    }

    @Test
    fun `solo el guardado confirmado cierra la hoja`() {
        assertTrue(ReglasFormularioMateria.termino(ResultadoGuardarMateria.GUARDADA))
        assertFalse(ReglasFormularioMateria.puedeEnviar("   ", guardando = false))
    }
}
