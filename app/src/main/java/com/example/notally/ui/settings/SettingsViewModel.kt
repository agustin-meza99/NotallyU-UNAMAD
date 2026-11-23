package com.example.notally.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notally.contenedorNotally
import com.example.notally.data.aula.AulaApi
import com.example.notally.data.aula.AulaClient
import com.example.notally.data.aula.AulaCredencialesStore
import com.example.notally.data.aula.AulaImporter
import com.example.notally.data.aula.ErrorAula
import com.example.notally.data.aula.ProgresoAula
import com.example.notally.data.aula.ResultadoImportacion
import com.example.notally.data.firebase.AuthRepository
import com.example.notally.data.firebase.ErrorAuth
import com.example.notally.data.firebase.ResultadoAuth
import com.example.notally.data.firebase.UsuarioFirebase
import com.example.notally.data.model.Settings
import com.example.notally.data.model.TemaApp
import com.example.notally.data.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Error visible del Aula Virtual, ya tipado para decidir si se puede reintentar. */
data class ErrorAulaUi(
    val tipo: ErrorAula,
    val mensajeServidor: String? = null,
    val reintentable: Boolean = false,
)

/** Estado del bloque Aula Virtual de Ajustes. */
data class EstadoAula(
    val cargandoInicial: Boolean = true,
    val conectado: Boolean = false,
    val usuario: String? = null,
    val ultimaImportacion: Long? = null,
    val importando: Boolean = false,
    val progreso: ProgresoAula? = null,
    val error: ErrorAulaUi? = null,
)

/** Mensajes de una sola vez para el snackbar. */
sealed interface EventoAula {
    data class Importado(
        val cursos: Int,
        val tareas: Int,
        val errores: Int,
    ) : EventoAula
}

/** Estado del bloque Cuenta de Ajustes. */
data class EstadoCuenta(
    val cargando: Boolean = false,
    val error: ErrorAuth? = null,
    val sincronizando: Boolean = false,
    val errorSync: Boolean = false,
)

/** ViewModel de Settings (§7.12). Persiste cada cambio en DataStore. */
class SettingsViewModel(
    private val repositorio: SettingsRepository,
    private val credencialesAula: AulaCredencialesStore,
    private val importadorAula: AulaImporter,
    private val authRepository: AuthRepository,
    private val crearClienteAula: () -> AulaApi = { AulaClient() },
    private val reloj: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val settings: StateFlow<Settings> = repositorio.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings(),
    )

    private val _estadoAula = MutableStateFlow(EstadoAula())
    val estadoAula: StateFlow<EstadoAula> = _estadoAula

    /** Sesión de Firebase Auth; ya es un StateFlow del repositorio. */
    val usuario: StateFlow<UsuarioFirebase?> = authRepository.usuario

    private val _estadoCuenta = MutableStateFlow(EstadoCuenta())
    val estadoCuenta: StateFlow<EstadoCuenta> = _estadoCuenta

    private val _eventosAula = MutableSharedFlow<EventoAula>(extraBufferCapacity = 4)
    val eventosAula: SharedFlow<EventoAula> = _eventosAula.asSharedFlow()

    init {
        viewModelScope.launch {
            val guardadas = try {
                credencialesAula.leer()
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                null
            }
            _estadoAula.update {
                it.copy(
                    cargandoInicial = false,
                    conectado = guardadas?.conectado == true,
                    usuario = guardadas?.usuario,
                    ultimaImportacion = guardadas?.ultimaImportacion,
                )
            }
        }
    }

    fun cambiarSemanaEmpiezaLunes(activo: Boolean) {
        viewModelScope.launch { repositorio.cambiarSemanaEmpiezaLunes(activo) }
    }

    fun cambiarTema(tema: TemaApp) {
        viewModelScope.launch { repositorio.cambiarTema(tema) }
    }

    /** Inicia sesión con email/contraseña y, si funciona, sube lo pendiente. */
    fun iniciarSesion(email: String, password: String) {
        ejecutarAuth { authRepository.iniciarSesion(email, password) }
    }

    /** Crea la cuenta y deja la sesión iniciada. */
    fun crearCuenta(email: String, password: String) {
        ejecutarAuth { authRepository.crearCuenta(email, password) }
    }

    /** Canjea en Firebase el ID token obtenido por Credential Manager. */
    fun iniciarConGoogle(idToken: String) {
        ejecutarAuth { authRepository.iniciarConGoogle(idToken) }
    }

    /** El usuario cerró el selector de cuentas: no es un error. */
    fun cancelarGoogle() {
        _estadoCuenta.update { it.copy(cargando = false, error = null) }
    }

    /** Fallo al obtener el token (sin Play Services, SHA sin registrar...). */
    fun falloGoogle() {
        _estadoCuenta.update {
            it.copy(cargando = false, error = ErrorAuth.GOOGLE, errorSync = false)
        }
    }

    fun descartarErrorCuenta() {
        _estadoCuenta.update { it.copy(error = null, errorSync = false) }
    }

    /**
     * Cierra la sesión de Firebase. No borra cursos ni tareas de Firestore: los
     * datos locales siguen disponibles sin conexión.
     */
    fun cerrarSesion() {
        authRepository.cerrarSesion()
        _estadoCuenta.value = EstadoCuenta()
    }

    /** Subida manual desde Ajustes. */
    fun sincronizarAhora() {
        if (_estadoCuenta.value.sincronizando) return
        viewModelScope.launch { ejecutarSync() }
    }

    private fun ejecutarAuth(bloque: suspend () -> ResultadoAuth) {
        if (_estadoCuenta.value.cargando) return
        viewModelScope.launch {
            _estadoCuenta.update { it.copy(cargando = true, error = null, errorSync = false) }
            when (val resultado = try {
                bloque()
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                ResultadoAuth.Fallo(ErrorAuth.DESCONOCIDO)
            }) {
                ResultadoAuth.Exito -> {
                    _estadoCuenta.update { it.copy(cargando = false, error = null) }
                    ejecutarSync()
                }

                is ResultadoAuth.Fallo -> {
                    _estadoCuenta.update {
                        it.copy(cargando = false, error = resultado.error)
                    }
                }
            }
        }
    }

    private suspend fun ejecutarSync() {
        if (authRepository.usuario.value == null) return
        _estadoCuenta.update { it.copy(sincronizando = true, errorSync = false) }
        try {
            repositorio.marcarSincronizacionFirestore(reloj())
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (_: Exception) {
        }
        _estadoCuenta.update {
            it.copy(sincronizando = false, errorSync = false)
        }
    }

    /**
     * Conecta e importa con las credenciales escritas por el usuario.
     *
     * Un solo intento: el servidor bloquea la cuenta tras varios fallos. La
     * contraseña no se guarda en el estado del ViewModel ni en el
     * `SavedStateHandle`.
     */
    fun conectarAula(usuario: String, clave: String) {
        if (usuario.isBlank() || clave.isBlank()) return
        if (_estadoAula.value.importando) return
        viewModelScope.launch {
            ejecutarImportacion(usuario.trim(), clave, guardarCredenciales = true)
        }
    }

    /** Reimporta con las credenciales cifradas ya guardadas. */
    fun importarAulaAhora() {
        if (_estadoAula.value.importando) return
        viewModelScope.launch {
            val guardadas = try {
                credencialesAula.leer()
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                null
            }
            if (guardadas == null) {
                _estadoAula.update {
                    it.copy(
                        conectado = false,
                        usuario = null,
                        ultimaImportacion = null,
                        error = ErrorAulaUi(
                            tipo = ErrorAula.CREDENCIALES,
                            reintentable = false,
                        ),
                    )
                }
                return@launch
            }
            ejecutarImportacion(
                usuario = guardadas.usuario,
                clave = guardadas.clave,
                guardarCredenciales = false,
            )
        }
    }

    /** Solo se ofrece para errores de red; nunca para credenciales. */
    fun reintentarAula() {
        if (_estadoAula.value.error?.reintentable != true) return
        importarAulaAhora()
    }

    fun descartarErrorAula() {
        _estadoAula.update { it.copy(error = null) }
    }

    fun desconectarAula() {
        viewModelScope.launch {
            try {
                credencialesAula.limpiar()
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                // Aunque falle el borrado, la UI deja de mostrar la conexión.
            }
            _estadoAula.value = EstadoAula(cargandoInicial = false)
        }
    }

    private suspend fun ejecutarImportacion(
        usuario: String,
        clave: String,
        guardarCredenciales: Boolean,
    ) {
        _estadoAula.update {
            it.copy(importando = true, progreso = null, error = null)
        }
        val resultado = importadorAula.importar(
            cliente = crearClienteAula(),
            usuario = usuario,
            clave = clave,
        ) { progreso ->
            _estadoAula.update { it.copy(progreso = progreso) }
        }

        when (resultado) {
            is ResultadoImportacion.Exito -> {
                try {
                    if (guardarCredenciales) {
                        credencialesAula.guardar(usuario, clave)
                    }
                    credencialesAula.marcarImportacion(resultado.ahora)
                } catch (cancelada: CancellationException) {
                    throw cancelada
                } catch (_: Exception) {
                    // La importación ya ocurrió; solo falló el guardado local.
                }
                _estadoAula.update {
                    it.copy(
                        conectado = true,
                        usuario = usuario,
                        ultimaImportacion = resultado.ahora,
                        importando = false,
                        progreso = null,
                        error = null,
                    )
                }
                ejecutarSync()
                _eventosAula.emit(
                    EventoAula.Importado(
                        cursos = resultado.cursos,
                        tareas = resultado.tareasNuevas + resultado.tareasActualizadas,
                        errores = resultado.errores,
                    ),
                )
            }

            is ResultadoImportacion.Fallo -> {
                val borrarCredenciales = resultado.tipo == ErrorAula.CREDENCIALES ||
                    resultado.tipo == ErrorAula.SESION_CADUCADA
                if (borrarCredenciales) {
                    try {
                        credencialesAula.limpiar()
                    } catch (cancelada: CancellationException) {
                        throw cancelada
                    } catch (_: Exception) {
                        // Sin credenciales utilizables, la UI queda desconectada.
                    }
                }
                _estadoAula.update {
                    it.copy(
                        conectado = if (borrarCredenciales) false else it.conectado,
                        usuario = if (borrarCredenciales) null else it.usuario,
                        ultimaImportacion = if (borrarCredenciales) {
                            null
                        } else {
                            it.ultimaImportacion
                        },
                        importando = false,
                        progreso = null,
                        error = ErrorAulaUi(
                            tipo = resultado.tipo,
                            mensajeServidor = resultado.mensajeServidor,
                            reintentable = resultado.tipo == ErrorAula.RED,
                        ),
                    )
                }
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val contenedor = context.contenedorNotally
                SettingsViewModel(
                    repositorio = contenedor.repositorioSettings,
                    credencialesAula = contenedor.credencialesAula,
                    importadorAula = contenedor.importadorAula,
                    authRepository = contenedor.authRepository,
                )
            }
        }
    }
}
