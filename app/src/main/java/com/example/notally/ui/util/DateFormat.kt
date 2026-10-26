package com.example.notally.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Etiqueta de fecha resuelta por la lógica pura.
 *
 * El texto visible no vive en Kotlin: [Hoy], [Manana] y [Ayer] se traducen con
 * `stringResource` en la capa de Compose, y [Corta] / [Larga] llevan ya el
 * formato español (día y mes, sin copy de producto).
 */
sealed interface EtiquetaFecha {
    data object Hoy : EtiquetaFecha
    data object Manana : EtiquetaFecha
    data object Ayer : EtiquetaFecha

    /** "Vie 31 oct" para el mismo año. */
    data class Corta(val texto: String) : EtiquetaFecha

    /** "31 oct 2026" para otro año. */
    data class Larga(val texto: String) : EtiquetaFecha
}

/**
 * Formateo de fechas y horas en español para la UI.
 *
 * Es puro (java.time, sin Android) para poder probarlo en la JVM. El ajuste
 * 12/24 h se recibe como parámetro; la capa de Compose lo obtiene del sistema.
 * Las palabras de fecha ("Hoy", "Mañana", "Ayer") y los plurales de atraso se
 * resuelven con recursos en `DateText.kt`, nunca aquí.
 */
object DateFormat {

    val LocaleEspanol: Locale = Locale.forLanguageTag("es")

    private val formatoDiaCorto: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM", LocaleEspanol)
    private val formatoDiaAbrev: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE", LocaleEspanol)
    private val formatoDiaLargo: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE", LocaleEspanol)
    private val formatoMesLargo: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM", LocaleEspanol)
    private val formatoDiaMesAnio: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", LocaleEspanol)
    private val formatoAntetitulo: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", LocaleEspanol)
    private val formatoHora12: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", LocaleEspanol)
    private val formatoHora24: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", LocaleEspanol)

    fun fechaLocal(millis: Long, zona: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()

    /**
     * "Hoy", "Mañana", "Ayer", "Vie 31 oct" o "31 oct 2026".
     *
     * Se capitaliza el día abreviado ("vie" → "Vie") y se omite el punto, tal
     * como exige el documento.
     */
    fun etiquetaFecha(
        entregaEn: Long,
        ahora: Long = System.currentTimeMillis(),
        zona: ZoneId = ZoneId.systemDefault(),
    ): EtiquetaFecha {
        val hoy = fechaLocal(ahora, zona)
        return etiquetaFecha(fechaLocal(entregaEn, zona), hoy)
    }

    fun etiquetaFecha(fecha: LocalDate, hoy: LocalDate): EtiquetaFecha = when {
        fecha == hoy -> EtiquetaFecha.Hoy
        fecha == hoy.plusDays(1) -> EtiquetaFecha.Manana
        fecha == hoy.minusDays(1) -> EtiquetaFecha.Ayer
        fecha.year == hoy.year -> EtiquetaFecha.Corta(
            capitalizar(formatoDiaCorto.format(fecha)),
        )

        else -> EtiquetaFecha.Larga(formatoDiaMesAnio.format(fecha))
    }

    /**
     * Días naturales transcurridos desde [entregaEn] hasta hoy. 0 significa que
     * vence hoy (aunque la hora ya haya pasado); solo ≥ 1 se considera
     * "Atrasada" en la UI.
     */
    fun diasAtraso(
        entregaEn: Long,
        ahora: Long = System.currentTimeMillis(),
        zona: ZoneId = ZoneId.systemDefault(),
    ): Int = ChronoUnit.DAYS
        .between(fechaLocal(entregaEn, zona), fechaLocal(ahora, zona))
        .toInt()
        .coerceAtLeast(0)

    /**
     * Hora del sistema: "18:05" en 24 h; "6:05 p. m." en 12 h con espacio
     * fino antes de la marca a. m. / p. m.
     */
    fun formatearHora(
        millis: Long,
        es24Horas: Boolean,
        zona: ZoneId = ZoneId.systemDefault(),
    ): String {
        val instante = Instant.ofEpochMilli(millis).atZone(zona)
        return if (es24Horas) {
            formatoHora24.format(instante)
        } else {
            formatoHora12.format(instante).replaceFirst(" ", "\u2009")
        }
    }

    /** "Domingo, 25 de octubre" en mayúsculas: "DOMINGO, 25 DE OCTUBRE". */
    fun antetitulo(
        millis: Long,
        zona: ZoneId = ZoneId.systemDefault(),
    ): String = capitalizar(formatoAntetitulo.format(fechaLocal(millis, zona)))
        .uppercase(LocaleEspanol)

    /** "LUN": tres letras del día, sin punto y en mayúsculas (§7.1.1). */
    fun abreviaturaDia(fecha: LocalDate): String =
        formatoDiaAbrev.format(fecha)
            .removeSuffix(".")
            .take(3)
            .uppercase(LocaleEspanol)

    /** "Domingo" con inicial mayúscula, para la etiqueta de accesibilidad. */
    fun nombreDia(fecha: LocalDate): String =
        capitalizar(formatoDiaLargo.format(fecha))

    /** "octubre" en minúscula, tal como va dentro de una frase. */
    fun nombreMes(fecha: LocalDate): String =
        formatoMesLargo.format(fecha).lowercase(LocaleEspanol)

    /** `true` si el instante no cae exactamente en la medianoche local. */
    fun tieneHora(
        millis: Long,
        zona: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val instante = Instant.ofEpochMilli(millis).atZone(zona)
        return instante.hour != 0 || instante.minute != 0
    }

    private fun capitalizar(valor: String): String =
        valor.replaceFirstChar { caracter ->
            if (caracter.isLowerCase()) caracter.titlecase(LocaleEspanol) else caracter.toString()
        }
}
