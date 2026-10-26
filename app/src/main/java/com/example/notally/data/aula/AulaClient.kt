package com.example.notally.data.aula

import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/** Motivos de fallo del Aula Virtual, tipados para la UI. */
enum class ErrorAula {
    CREDENCIALES,
    BLOQUEO,
    RED,
    SERVIDOR,
    SESION_CADUCADA,
    SIN_CURSOS,
}

/** Fallo con motivo tipado y, opcionalmente, el texto devuelto por el servidor. */
class AulaException(
    val tipo: ErrorAula,
    val mensajeServidor: String? = null,
    causa: Throwable? = null,
) : Exception(mensajeServidor ?: tipo.name, causa)

sealed interface ResultadoLogin {
    data object Exito : ResultadoLogin
    data class Fallo(
        val tipo: ErrorAula,
        val mensajeServidor: String? = null,
    ) : ResultadoLogin
}

/** Contrato mínimo del Aula, para poder sustituirlo en pruebas sin red. */
interface AulaApi {
    suspend fun iniciarSesion(usuario: String, clave: String): ResultadoLogin

    /** @throws AulaException con [ErrorAula.SESION_CADUCADA], [ErrorAula.RED]... */
    suspend fun cursos(): List<AulaCurso>

    /** @throws AulaException */
    suspend fun tareas(sectionId: String): List<AulaTarea>

    fun disconnect()
}

/**
 * Cliente HTTP del Aula Virtual sin dependencias de red: [HttpURLConnection] y
 * `org.json`, ambos en Android.
 *
 * La sesión vive en un [CookieManager] privado de esta instancia; nunca se
 * persiste ni se registra. Los timeouts son de 15 s.
 */
class AulaClient(
    private val baseUrl: String = BASE_URL,
) : AulaApi {

    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)
    private val conexiones = mutableListOf<HttpURLConnection>()

    override suspend fun iniciarSesion(usuario: String, clave: String): ResultadoLogin =
        withContext(Dispatchers.IO) {
            try {
                val cuerpo = "username=" + codificar(usuario) +
                    "&password=" + codificar(clave)
                val respuesta = solicitar(
                    ruta = RUTA_LOGIN,
                    metodo = "POST",
                    cuerpo = cuerpo,
                    tipoContenido = "application/x-www-form-urlencoded",
                )
                when (respuesta.codigo) {
                    200 -> interpretarLogin(respuesta.cuerpo)
                    429 -> ResultadoLogin.Fallo(
                        ErrorAula.BLOQUEO,
                        "El servidor bloqueó temporalmente la cuenta. " +
                            "Espera unos minutos antes de intentar otra vez.",
                    )

                    401, 403 -> ResultadoLogin.Fallo(
                        ErrorAula.CREDENCIALES,
                        "Usuario o contraseña incorrectos.",
                    )

                    in 500..599 -> ResultadoLogin.Fallo(
                        ErrorAula.SERVIDOR,
                        "El Aula Virtual no está disponible en este momento.",
                    )

                    else -> ResultadoLogin.Fallo(
                        ErrorAula.SERVIDOR,
                        "Respuesta inesperada del servidor (${respuesta.codigo}).",
                    )
                }
            } catch (error: IOException) {
                ResultadoLogin.Fallo(ErrorAula.RED, "Sin conexión con el Aula Virtual.")
            } catch (error: AulaException) {
                ResultadoLogin.Fallo(error.tipo, error.mensajeServidor)
            }
        }

    override suspend fun cursos(): List<AulaCurso> = obtener(RUTA_CURSOS) { json ->
        AulaParser.parsearCursos(json)
    }

    override suspend fun tareas(sectionId: String): List<AulaTarea> {
        val ruta = "$RUTA_TAREAS?s=" + codificar(sectionId)
        return obtener(ruta) { json -> AulaParser.parsearTareas(json) }
    }

    override fun disconnect() {
        conexiones.forEach { conexion ->
            runCatching { conexion.disconnect() }
        }
        conexiones.clear()
        cookies.cookieStore.removeAll()
    }

    private suspend fun <T> obtener(
        ruta: String,
        parsear: (String) -> T,
    ): T = withContext(Dispatchers.IO) {
        val respuesta = try {
            solicitar(ruta = ruta, metodo = "GET")
        } catch (error: IOException) {
            throw AulaException(ErrorAula.RED, "Sin conexión con el Aula Virtual.", error)
        }
        when {
            respuesta.codigo in 200..299 ->
                try {
                    parsear(respuesta.cuerpo)
                } catch (error: AulaJsonException) {
                    throw AulaException(
                        ErrorAula.SERVIDOR,
                        "El Aula Virtual devolvió datos con un formato inesperado.",
                        error,
                    )
                }

            respuesta.codigo == 401 ||
                respuesta.codigo == 403 ||
                respuesta.codigo == 419 ||
                respuesta.redirigeALogin ->
                throw AulaException(
                    ErrorAula.SESION_CADUCADA,
                    "La sesión del Aula Virtual caducó. Vuelve a conectar.",
                )

            respuesta.codigo == 429 ->
                throw AulaException(
                    ErrorAula.BLOQUEO,
                    "El servidor bloqueó temporalmente la cuenta.",
                )

            else -> throw AulaException(
                ErrorAula.SERVIDOR,
                "El Aula Virtual falló (${respuesta.codigo}).",
            )
        }
    }

    private fun interpretarLogin(cuerpo: String): ResultadoLogin {
        val objeto = try {
            JSONObject(cuerpo)
        } catch (error: JSONException) {
            return ResultadoLogin.Fallo(
                ErrorAula.SERVIDOR,
                "El Aula Virtual devolvió una respuesta inesperada.",
            )
        }
        val exito = objeto.optBoolean("status", false)
        val mensaje = objeto.optString("data", "").takeUnless { it.isBlank() }
        if (exito) return ResultadoLogin.Exito
        val bloqueo = mensaje?.let { texto ->
            Regex("(?i)bloque|intentos|demasiad|espera|intenta m[aá]s tarde")
                .containsMatchIn(texto)
        } == true
        return ResultadoLogin.Fallo(
            if (bloqueo) ErrorAula.BLOQUEO else ErrorAula.CREDENCIALES,
            mensaje,
        )
    }

    private data class Respuesta(
        val codigo: Int,
        val cuerpo: String,
        val redirigeALogin: Boolean,
    )

    private fun solicitar(
        ruta: String,
        metodo: String,
        cuerpo: String? = null,
        tipoContenido: String? = null,
    ): Respuesta {
        val url = URL(if (ruta.startsWith("http")) ruta else baseUrl + ruta)
        val conexion = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = metodo
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("Accept", "application/json, text/plain, */*")
            setRequestProperty("Accept-Language", "es-PE,es;q=0.9")
            cookies.cookieStore.get(url.toURI())
                ?.takeIf { it.isNotEmpty() }
                ?.let { galletas ->
                    setRequestProperty(
                        "Cookie",
                        galletas.joinToString("; ") { "${it.name}=${it.value}" },
                    )
                }
            if (tipoContenido != null) setRequestProperty("Content-Type", tipoContenido)
            if (cuerpo != null) {
                doOutput = true
                setFixedLengthStreamingMode(cuerpo.toByteArray(Charsets.UTF_8).size)
            }
        }
        synchronized(conexiones) { conexiones.add(conexion) }
        try {
            if (cuerpo != null) {
                conexion.outputStream.use { salida ->
                    salida.write(cuerpo.toByteArray(Charsets.UTF_8))
                }
            }
            val codigo = conexion.responseCode
            guardarCookies(url.toURI(), conexion.headerFields)
            val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val texto = flujo?.use { entrada ->
                entrada.readBytes().toString(Charsets.UTF_8)
            }.orEmpty()
            val ubicacion = conexion.getHeaderField("Location").orEmpty()
            return Respuesta(
                codigo = codigo,
                cuerpo = texto,
                redirigeALogin = ubicacion.contains("login", ignoreCase = true),
            )
        } finally {
            synchronized(conexiones) { conexiones.remove(conexion) }
            conexion.disconnect()
        }
    }

    private fun guardarCookies(uri: URI, campos: Map<String, List<String>>) {
        val lineas = campos.entries
            .firstOrNull { (nombre, _) -> nombre.equals("Set-Cookie", ignoreCase = true) }
            ?.value
            .orEmpty()
        lineas.forEach { linea ->
            runCatching {
                HttpCookie.parse(linea).forEach { galleta ->
                    if (galleta.domain.isNullOrBlank()) {
                        galleta.domain = uri.host
                    }
                    if (galleta.path.isNullOrBlank()) {
                        galleta.path = "/"
                    }
                    cookies.cookieStore.add(uri, galleta)
                }
            }
        }
    }

    private fun codificar(valor: String): String =
        URLEncoder.encode(valor, Charsets.UTF_8.name())

    private companion object {
        const val BASE_URL = "https://aula-virtual.unamad.edu.pe"
        const val RUTA_LOGIN = "/account/login"
        const val RUTA_CURSOS = "/web/user/info/courseinrole"
        const val RUTA_TAREAS = "/web/homework/list"
        const val TIMEOUT_MS = 15_000
    }
}
