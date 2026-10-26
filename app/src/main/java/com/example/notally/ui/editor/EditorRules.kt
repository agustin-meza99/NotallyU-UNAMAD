package com.example.notally.ui.editor

/**
 * Reglas puras del editor (§7.5), separadas de Compose y de Room para poder
 * probarlas en la JVM.
 *
 * Contrato de guardado:
 * - Un título vacío es válido: se guarda como "Sin título" solo si hay notas o
 *   subtareas.
 * - Una tarea nueva sin título, notas ni subtareas no se escribe en la base; al
 *   volver se descarta sin diálogo.
 */
object EditorRules {

    fun hasContent(title: String, notes: String, subtaskCount: Int): Boolean =
        title.isNotBlank() || notes.isNotBlank() || subtaskCount > 0

    /** Título que se muestra/guarda: recorta espacios y aplica el reemplazo. */
    fun displayTitle(title: String, untitledLabel: String): String =
        title.trim().ifEmpty { untitledLabel }

    /**
     * Título que debe persistirse, o `null` si no hay nada que guardar.
     * [untitledLabel] llega desde `R.string.editor_untitled` para no duplicar
     * microcopia en la capa de lógica.
     */
    fun persistedTitle(
        title: String,
        notes: String,
        subtaskCount: Int,
        untitledLabel: String,
    ): String? = if (hasContent(title, notes, subtaskCount)) {
        displayTitle(title, untitledLabel)
    } else {
        null
    }
}
