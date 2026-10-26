package com.example.notally.ui.cursos

import com.example.notally.data.repository.ResultadoCurso

/**
 * Reglas puras de la hoja de curso (§7.4, §7.9).
 *
 * "Guardar" se habilita con un nombre no vacío y se deshabilita mientras hay
 * una escritura en curso. Al terminar cualquier intento —éxito o error— el
 * estado vuelve a habilitado: dos fallos seguidos no dejan el botón muerto.
 */
object CursoFormRules {

    fun puedeEnviar(nombre: String, guardando: Boolean): Boolean =
        nombre.isNotBlank() && !guardando

    /** Solo un guardado confirmado cierra la hoja. */
    fun termino(resultado: ResultadoCurso): Boolean = resultado.esExito
}
