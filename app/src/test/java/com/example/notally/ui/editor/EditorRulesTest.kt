package com.example.notally.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorRulesTest {

    private val sinTitulo = "Sin título"

    @Test
    fun `un borrador vacio no se guarda`() {
        assertNull(EditorRules.tituloPersistido("", "", 0, sinTitulo))
        assertFalse(EditorRules.tieneContenido("", "", 0))
    }

    @Test
    fun `un titulo con solo espacios tampoco se guarda`() {
        assertNull(EditorRules.tituloPersistido("   ", "  ", 0, sinTitulo))
    }

    @Test
    fun `con notas y sin titulo se guarda como Sin titulo`() {
        assertEquals(
            sinTitulo,
            EditorRules.tituloPersistido("", "Repasar la unidad 3", 0, sinTitulo),
        )
        assertTrue(EditorRules.tieneContenido("", "Repasar la unidad 3", 0))
    }

    @Test
    fun `con subtareas y sin titulo se guarda como Sin titulo`() {
        assertEquals(sinTitulo, EditorRules.tituloPersistido("", "", 2, sinTitulo))
        assertTrue(EditorRules.tieneContenido("", "", 2))
    }

    @Test
    fun `el titulo se recorta antes de guardar`() {
        assertEquals(
            "Entregar informe",
            EditorRules.tituloPersistido("  Entregar informe  ", "", 0, sinTitulo),
        )
    }

    @Test
    fun `tituloVisible aplica el reemplazo solo cuando esta vacio`() {
        assertEquals(sinTitulo, EditorRules.tituloVisible("   ", sinTitulo))
        assertEquals("Parcial", EditorRules.tituloVisible(" Parcial ", sinTitulo))
    }

    @Test
    fun `un titulo sin notas ni subtareas si se guarda`() {
        assertEquals("Parcial", EditorRules.tituloPersistido("Parcial", "", 0, sinTitulo))
    }
}
