package com.example.notally.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorRulesTest {

    private val untitled = "Sin título"

    @Test
    fun `un borrador vacio no se guarda`() {
        assertNull(EditorRules.persistedTitle("", "", 0, untitled))
        assertFalse(EditorRules.hasContent("", "", 0))
    }

    @Test
    fun `un titulo con solo espacios tampoco se guarda`() {
        assertNull(EditorRules.persistedTitle("   ", "  ", 0, untitled))
    }

    @Test
    fun `con notas y sin titulo se guarda como Sin titulo`() {
        assertEquals(
            untitled,
            EditorRules.persistedTitle("", "Repasar la unidad 3", 0, untitled),
        )
        assertTrue(EditorRules.hasContent("", "Repasar la unidad 3", 0))
    }

    @Test
    fun `con subtareas y sin titulo se guarda como Sin titulo`() {
        assertEquals(untitled, EditorRules.persistedTitle("", "", 2, untitled))
        assertTrue(EditorRules.hasContent("", "", 2))
    }

    @Test
    fun `el titulo se recorta antes de guardar`() {
        assertEquals("Entregar informe", EditorRules.persistedTitle("  Entregar informe  ", "", 0, untitled))
    }

    @Test
    fun `displayTitle aplica el reemplazo solo cuando esta vacio`() {
        assertEquals(untitled, EditorRules.displayTitle("   ", untitled))
        assertEquals("Parcial", EditorRules.displayTitle(" Parcial ", untitled))
    }

    @Test
    fun `un titulo sin notas ni subtareas si se guarda`() {
        assertEquals("Parcial", EditorRules.persistedTitle("Parcial", "", 0, untitled))
    }
}
