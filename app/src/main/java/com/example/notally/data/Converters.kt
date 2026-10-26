package com.example.notally.data

import androidx.room.TypeConverter
import com.example.notally.data.model.Estado
import com.example.notally.data.model.ModoTema
import com.example.notally.data.model.Prioridad

/**
 * Conversores de Room para los enums del dominio.
 *
 * Los identificadores Kotlin están en español, pero los valores persistidos
 * conservan el contrato original en inglés (`"HIGH"`, `"IN_PROGRESS"`,
 * `"DARK"`, ...). El mapeo es explícito y puro para que el esquema y las
 * consultas SQL no cambien.
 */
class Converters {

    @TypeConverter
    fun prioridadATexto(valor: Prioridad): String = valor.aTexto()

    @TypeConverter
    fun textoAPrioridad(valor: String): Prioridad = prioridadDesdeTexto(valor)

    @TypeConverter
    fun estadoATexto(valor: Estado): String = valor.aTexto()

    @TypeConverter
    fun textoAEstado(valor: String): Estado = estadoDesdeTexto(valor)

    @TypeConverter
    fun modoTemaATexto(valor: ModoTema): String = valor.aTexto()

    @TypeConverter
    fun textoAModoTema(valor: String): ModoTema = modoTemaDesdeTexto(valor)
}

/** Texto persistido de una prioridad, con los nombres originales en inglés. */
fun Prioridad.aTexto(): String = when (this) {
    Prioridad.BAJA -> "LOW"
    Prioridad.MEDIA -> "MEDIUM"
    Prioridad.ALTA -> "HIGH"
}

/** Reconstruye una prioridad desde su texto persistido. */
fun prioridadDesdeTexto(texto: String): Prioridad = when (texto) {
    "LOW" -> Prioridad.BAJA
    "MEDIUM" -> Prioridad.MEDIA
    "HIGH" -> Prioridad.ALTA
    else -> throw IllegalArgumentException("Prioridad desconocida: $texto")
}

/** Texto persistido de un estado, con los nombres originales en inglés. */
fun Estado.aTexto(): String = when (this) {
    Estado.PENDIENTE -> "PENDING"
    Estado.EN_CURSO -> "IN_PROGRESS"
    Estado.COMPLETADA -> "DONE"
}

/** Reconstruye un estado desde su texto persistido. */
fun estadoDesdeTexto(texto: String): Estado = when (texto) {
    "PENDING" -> Estado.PENDIENTE
    "IN_PROGRESS" -> Estado.EN_CURSO
    "DONE" -> Estado.COMPLETADA
    else -> throw IllegalArgumentException("Estado desconocido: $texto")
}

/** Texto persistido de un modo de tema, con los nombres originales en inglés. */
fun ModoTema.aTexto(): String = when (this) {
    ModoTema.SISTEMA -> "SYSTEM"
    ModoTema.CLARO -> "LIGHT"
    ModoTema.OSCURO -> "DARK"
}

/** Reconstruye un modo de tema desde su texto persistido. */
fun modoTemaDesdeTexto(texto: String): ModoTema = when (texto) {
    "SYSTEM" -> ModoTema.SISTEMA
    "LIGHT" -> ModoTema.CLARO
    "DARK" -> ModoTema.OSCURO
    else -> throw IllegalArgumentException("Modo de tema desconocido: $texto")
}
