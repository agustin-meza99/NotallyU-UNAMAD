package com.example.notally.data

/**
 * Los seis acentos de materia del DESIGN.md §2.5, en ARGB opaco.
 *
 * Es la única fuente de verdad: [com.example.notally.ui.theme.NotallySubjectSwatches]
 * construye sus `argb` a partir de estas constantes y el seed elige entre ellas,
 * de modo que un color sembrado siempre coincide exactamente con un swatch
 * seleccionable (sin resolución "al más cercano" ni duplicados).
 */
object SubjectColors {
    const val INDIGO: Long = 0xFF3A5A9E
    const val FOREST: Long = 0xFF2F6A4E
    const val TERRACOTTA: Long = 0xFF99482E
    const val LIME: Long = 0xFF5F7300
    const val LAVENDER: Long = 0xFF7A5F87
    const val PETROL: Long = 0xFF1D5F6A

    /** Orden de la tabla del documento, usado por los swatches. */
    val all: List<Long> = listOf(INDIGO, FOREST, TERRACOTTA, LIME, LAVENDER, PETROL)
}
