package com.example.notally.ui.cursos

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CursoFormRulesTest {

    @Test
    fun `guardar se reactiva tras dos fallos con el mismo resultado`() {
        val nombre = "Cálculo diferencial"

        repeat(2) {
            assertTrue(CursoFormRules.puedeEnviar(nombre, guardando = false))
            assertFalse(CursoFormRules.puedeEnviar(nombre, guardando = true))
            // `finally` vuelve a guardando = false aunque el error se repita.
            assertTrue(CursoFormRules.puedeEnviar(nombre, guardando = false))
        }
        assertFalse(CursoFormRules.termino(ResultadoGuardarCurso.DUPLICADO))
        assertFalse(CursoFormRules.termino(ResultadoGuardarCurso.GENERICO))
    }

    @Test
    fun `solo el guardado confirmado cierra la hoja`() {
        assertTrue(CursoFormRules.termino(ResultadoGuardarCurso.GUARDADO))
        assertFalse(CursoFormRules.puedeEnviar("   ", guardando = false))
    }
}
