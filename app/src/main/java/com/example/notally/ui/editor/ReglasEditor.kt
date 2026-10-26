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
object ReglasEditor {

    fun tieneContenido(titulo: String, notas: String, cantidadSubtareas: Int): Boolean =
        titulo.isNotBlank() || notas.isNotBlank() || cantidadSubtareas > 0

    /** Título que se muestra/guarda: recorta espacios y aplica el reemplazo. */
    fun tituloVisible(titulo: String, etiquetaSinTitulo: String): String =
        titulo.trim().ifEmpty { etiquetaSinTitulo }

    /**
     * Título que debe persistirse, o `null` si no hay nada que guardar.
     * [etiquetaSinTitulo] llega desde `R.string.editor_untitled` para no duplicar
     * microcopia en la capa de lógica.
     */
    fun tituloPersistido(
        titulo: String,
        notas: String,
        cantidadSubtareas: Int,
        etiquetaSinTitulo: String,
    ): String? = if (tieneContenido(titulo, notas, cantidadSubtareas)) {
        tituloVisible(titulo, etiquetaSinTitulo)
    } else {
        null
    }
}
