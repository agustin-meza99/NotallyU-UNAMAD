package com.example.notally.ui.subjects

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectFormRulesTest {

    @Test
    fun `guardar se reactiva tras dos fallos con el mismo resultado`() {
        val name = "Cálculo diferencial"

        repeat(2) {
            assertTrue(SubjectFormRules.canSubmit(name, saving = false))
            assertFalse(SubjectFormRules.canSubmit(name, saving = true))
            // `finally` vuelve a saving = false aunque el error se repita.
            assertTrue(SubjectFormRules.canSubmit(name, saving = false))
        }
        assertFalse(SubjectFormRules.isFinished(SubjectSaveResult.DUPLICATE))
        assertFalse(SubjectFormRules.isFinished(SubjectSaveResult.GENERIC))
    }

    @Test
    fun `solo el guardado confirmado cierra la hoja`() {
        assertTrue(SubjectFormRules.isFinished(SubjectSaveResult.SAVED))
        assertFalse(SubjectFormRules.canSubmit("   ", saving = false))
    }
}
