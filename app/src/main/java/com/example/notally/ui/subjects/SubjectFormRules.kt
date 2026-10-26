package com.example.notally.ui.subjects

/**
 * Reglas puras de la hoja de materia (§7.4, §7.9).
 *
 * "Guardar" se habilita con un nombre no vacío y se deshabilita mientras hay
 * una escritura en curso. Al terminar cualquier intento —éxito o error— el
 * estado vuelve a habilitado: dos fallos seguidos no dejan el botón muerto.
 */
object SubjectFormRules {

    fun canSubmit(name: String, saving: Boolean): Boolean =
        name.isNotBlank() && !saving

    /** Solo un guardado confirmado cierra la hoja. */
    fun isFinished(result: SubjectSaveResult): Boolean =
        result == SubjectSaveResult.SAVED
}
