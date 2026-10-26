package com.example.notally.ui.cursos

import com.example.notally.data.repository.ResultadoCurso
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
        assertFalse(CursoFormRules.termino(ResultadoCurso.Duplicado))
        assertFalse(CursoFormRules.termino(ResultadoCurso.Error))
    }

    @Test
    fun `solo el guardado confirmado cierra la hoja`() {
        assertTrue(CursoFormRules.termino(ResultadoCurso.Creado(1L)))
        assertTrue(CursoFormRules.termino(ResultadoCurso.Actualizado(1L)))
        assertFalse(CursoFormRules.puedeEnviar("   ", guardando = false))
    }
}
