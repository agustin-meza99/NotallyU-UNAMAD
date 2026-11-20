package com.example.notally.data.aula

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Error de formato del JSON del Aula Virtual. No contiene datos sensibles. */
class AulaJsonException(mensaje: String, causa: Throwable? = null) :
    IllegalArgumentException(mensaje, causa)

/**
 * Parser puro del Aula Virtual.
 *
 * No usa Android ni scraping: recibe el JSON de los endpoints y devuelve
 * modelos normalizados. La limpieza de HTML y el parseo de fechas viven aquí
 * para poder probarlos en la JVM.
 */
object AulaParser {

    val ZonaAula: ZoneId = ZoneId.of("America/Lima")

    private val formatoConHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val formatoConHoraCorta = DateTimeFormatter.ofPattern("dd/MM/yyyy H:mm")
    private val formatoSinHora = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val etiquetasBloque = Regex(
        "(?i)<\\s*/?\\s*(br|p|div|li|ul|ol|h[1-6]|tr|td|th|blockquote)\\b[^>]*>",
    )
    private val etiquetas = Regex("<[^>]*>")
    private val espacios = Regex("\\s+")

    private val entidades = mapOf(
        "nbsp" to " ",
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "ndash" to "–",
        "mdash" to "—",
        "hellip" to "…",
        "laquo" to "«",
        "raquo" to "»",
        "ordm" to "º",
        "ordf" to "ª",
        "deg" to "°",
        "aacute" to "á",
        "eacute" to "é",
        "iacute" to "í",
        "oacute" to "ó",
        "uacute" to "ú",
        "ntilde" to "ñ",
        "uuml" to "ü",
        "Aacute" to "Á",
        "Eacute" to "É",
        "Iacute" to "Í",
        "Oacute" to "Ó",
        "Uacute" to "Ú",
        "Ntilde" to "Ñ",
        "iexcl" to "¡",
        "iquest" to "¿",
    )

    /**
     * Cursos de `/web/user/info/courseinrole`.
     *
     * Acepta el array directo o un objeto `{status, data}` envolvente.
     */
    fun parsearCursos(json: String): List<AulaCurso> {
        val arreglo = arregloRaiz(json)
        return buildList(arreglo.length()) {
            for (indice in 0 until arreglo.length()) {
                val objeto = arreglo.optJSONObject(indice)
                    ?: throw AulaJsonException("El curso $indice no es un objeto")
                val sectionId = objeto.texto("sectionId")
                val nombre = objeto.texto("name")
                if (sectionId.isNullOrBlank() || nombre.isNullOrBlank()) {
                    throw AulaJsonException("El curso $indice no tiene sección o nombre")
                }
                add(
                    AulaCurso(
                        id = objeto.texto("id").orEmpty(),
                        codigoCurso = objeto.texto("codeCourse").orEmpty(),
                        sectionId = sectionId,
                        codigoSeccion = objeto.texto("codeSection").orEmpty(),
                        nombre = nombre.trim(),
                        tipo = objeto.texto("type"),
                        area = objeto.texto("area"),
                        departamento = objeto.texto("deparment")
                            ?: objeto.texto("department"),
                        facultad = objeto.texto("faculty"),
                    ),
                )
            }
        }
    }

    /** Tareas de `/web/homework/list?s=<sectionId>`. */
    fun parsearTareas(json: String): List<AulaTarea> {
        val arreglo = arregloRaiz(json)
        return buildList(arreglo.length()) {
            for (indice in 0 until arreglo.length()) {
                val objeto = arreglo.optJSONObject(indice)
                    ?: throw AulaJsonException("La tarea $indice no es un objeto")
                val id = objeto.texto("id")
                val titulo = objeto.texto("title")
                if (id.isNullOrBlank() || titulo.isNullOrBlank()) {
                    throw AulaJsonException("La tarea $indice no tiene id o título")
                }
                add(
                    AulaTarea(
                        id = id,
                        titulo = titulo.trim(),
                        descripcion = limpiarHtml(objeto.texto("description")),
                        unidad = objeto.texto("unidad")?.trim()?.takeUnless { it.isEmpty() },
                        inicioEn = parsearFecha(objeto.texto("dateBegin")),
                        entregaEn = parsearFecha(objeto.texto("dateEnd")),
                        finalizada = objeto.optBoolean("isEnded", false),
                        intentos = objeto.optInt("attempts", 0),
                        grupal = objeto.optBoolean("isGroup", false),
                        entregada = objeto.entero("homeworkstds") > 0,
                    ),
                )
            }
        }
    }

    /**
     * Calificaciones de `/web/Qualification/ListHomeworkQualification?s=<sectionId>`.
     * Devuelve un mapa `taskId -> (score, feedback)` tomando la última entrega
     * registrada en `homeworkstds`.
     */
    fun parsearCalificaciones(json: String): Map<String, Pair<Double?, String?>> {
        val arreglo = arregloRaiz(json)
        return buildMap(arreglo.length()) {
            for (indice in 0 until arreglo.length()) {
                val objeto = arreglo.optJSONObject(indice) ?: continue
                val id = objeto.texto("id") ?: continue
                val entregas = objeto.optJSONArray("homeworkstds") ?: continue
                if (entregas.length() == 0) continue
                val entrega = entregas.optJSONObject(entregas.length() - 1) ?: continue
                val nota = if (entrega.has("score") && !entrega.isNull("score")) {
                    entrega.optDouble("score").takeUnless { it.isNaN() }
                } else null
                val feedback = entrega.texto("feedback")?.trim()?.takeUnless { it.isEmpty() }
                put(id, nota to feedback)
            }
        }
    }

    /**
     * Periodo académico seleccionado de `/periodos-academicos/disponibles/get`.
     *
     * Devuelve el `text` del primer elemento con `selected: true`, o `null` si
     * no hay ninguno.
     */
    fun parsearPeriodo(json: String): String? {
        val arreglo = arregloRaiz(json)
        for (indice in 0 until arreglo.length()) {
            val objeto = arreglo.optJSONObject(indice) ?: continue
            if (!objeto.optBoolean("selected", false)) continue
            return objeto.texto("text")?.trim()?.takeUnless { it.isEmpty() }
        }
        return null
    }

    /**
     * Convierte HTML del Aula en texto plano: quita etiquetas, separa bloques,
     * decodifica entidades y colapsa espacios y saltos de línea.
     */
    fun limpiarHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""
        val conSaltos = etiquetasBloque.replace(html, " ")
        val sinEtiquetas = etiquetas.replace(conSaltos, " ")
        return decodificarEntidades(sinEtiquetas)
            .replace('\u00A0', ' ')
            .replace(espacios, " ")
            .trim()
    }

    /**
     * `dd/MM/yyyy HH:mm` en America/Lima. Tolera `dd/MM/yyyy` sin hora, en cuyo
     * caso usa las 23:59 de ese día. Devuelve `null` si la fecha falta o no se
     * reconoce.
     */
    fun parsearFecha(texto: String?, zona: ZoneId = ZonaAula): Long? {
        val valor = texto?.trim().orEmpty()
        if (valor.isEmpty()) return null
        val fechaHora = runCatching { LocalDateTime.parse(valor, formatoConHora) }
            .recoverCatching { LocalDateTime.parse(valor, formatoConHoraCorta) }
            .getOrNull()
        if (fechaHora != null) {
            return fechaHora.atZone(zona).toInstant().toEpochMilli()
        }
        val fecha = runCatching { LocalDate.parse(valor, formatoSinHora) }.getOrNull()
            ?: return null
        return fecha.atTime(LocalTime.of(23, 59)).atZone(zona).toInstant().toEpochMilli()
    }

    private fun arregloRaiz(json: String): JSONArray {
        val limpio = json.trim().removePrefix("\uFEFF")
        if (limpio.isEmpty()) throw AulaJsonException("Respuesta vacía")
        val directo = runCatching { JSONArray(limpio) }.getOrNull()
        if (directo != null) return directo
        val objeto = try {
            JSONObject(limpio)
        } catch (error: JSONException) {
            throw AulaJsonException("JSON inválido", error)
        }
        val datos = objeto.opt("data")
        return when (datos) {
            is JSONArray -> datos
            is String -> try {
                JSONArray(datos)
            } catch (error: JSONException) {
                throw AulaJsonException("El campo data no es un array", error)
            }

            null -> JSONArray()
            else -> throw AulaJsonException("El campo data no es un array")
        }
    }

    private fun decodificarEntidades(texto: String): String {
        val patron = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);")
        return patron.replace(texto) { coincidencia ->
            val cuerpo = coincidencia.groupValues[1]
            if (cuerpo.startsWith("#")) {
                val codigo = if (cuerpo.startsWith("#x") || cuerpo.startsWith("#X")) {
                    cuerpo.drop(2).toIntOrNull(16)
                } else {
                    cuerpo.drop(1).toIntOrNull()
                }
                codigo?.takeIf { it in 0..0x10FFFF }
                    ?.let { String(Character.toChars(it)) }
                    ?: coincidencia.value
            } else {
                entidades[cuerpo] ?: coincidencia.value
            }
        }
    }

    private fun JSONObject.texto(clave: String): String? {
        if (!has(clave) || isNull(clave)) return null
        return optString(clave, "").takeUnless { it.isBlank() }
    }

    /**
     * Entero que puede llegar como número o como string numérico; `0` si el
     * campo falta o no se puede interpretar.
     */
    private fun JSONObject.entero(clave: String): Int {
        if (!has(clave) || isNull(clave)) return 0
        val valor = opt(clave)
        return when (valor) {
            is Number -> valor.toInt()
            is String -> valor.trim().toIntOrNull() ?: 0
            else -> 0
        }
    }
}
