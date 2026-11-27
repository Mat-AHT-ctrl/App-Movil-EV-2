package com.example.myapplication.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.DatabaseProvider
import com.example.myapplication.data.remote.ApiResult
import com.example.myapplication.data.remote.BackendRepository
import com.example.myapplication.data.remote.BloqueoDto
import com.example.myapplication.data.remote.BloqueoRequestDto
import com.example.myapplication.data.remote.EspacioDto
import com.example.myapplication.data.remote.EstadoMapper
import com.example.myapplication.data.remote.LoginResponseDto
import com.example.myapplication.data.remote.ReservaDto
import com.example.myapplication.data.remote.ReservaRequestDto
import com.example.myapplication.data.remote.toEspacio
import com.example.myapplication.data.remote.toReservaLocal
import com.example.myapplication.model.ArchivoItem
import com.example.myapplication.model.Espacio
import com.example.myapplication.model.Reserva
import com.example.myapplication.model.Usuario
import kotlinx.coroutines.launch

/** Resultado del login, para que la UI muestre el mensaje correcto. */
sealed interface LoginResult {
    data object Ok : LoginResult
    data object CredencialesInvalidas : LoginResult
    data class Error(val mensaje: String) : LoginResult
}

/** Reserva lista para Mis Reservas / Inspectoría (local u remota). */
data class ReservaVista(
    val idLocal: Int = 0,
    val idRemota: String? = null,
    val espacioId: String = "",
    val espacioNombre: String,
    val usuarioId: String = "",
    val solicitanteNombre: String = "",
    val perfilSolicitante: String = "",
    val fecha: String,
    val horaInicio: String,
    val horaTermino: String,
    val actividad: String = "",
    val cursoGrupo: String = "",
    val asistentes: Int = 0,
    val estado: String,
    val justificacion: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase by lazy {
        DatabaseProvider.getDatabase(application)
    }

    private val backend = BackendRepository()

    var currentScreen by mutableStateOf("login")
    var espacioSeleccionado: Espacio? by mutableStateOf(null)

    /** Feriados legales de Chile (API externa). Vacío = solo fin de semana. */
    var feriadosChile by mutableStateOf(emptySet<String>())
        private set

    /** Selección real del detalle (fecha dd/MM/yyyy y bloques contiguos). */
    var reservaFecha by mutableStateOf("")
    var reservaHoraInicio by mutableStateOf("08:15")
    var reservaHoraTermino by mutableStateOf("09:00")

    /** Usuario tal como lo devuelve el backend (id UUID, rol y perfil reales). */
    var usuarioRemoto: LoginResponseDto? by mutableStateOf(null)
        private set

    /** true cuando se entro sin conexion, usando la copia local de Room. */
    var modoOffline by mutableStateOf(false)
        private set

    /** Tema oscuro activado o desactivado. Se persiste en SharedPreferences. */
    var darkTheme by mutableStateOf(false)
        private set

    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences("app_colegio_prefs", android.content.Context.MODE_PRIVATE)
    }

    private var _currentUser: Usuario? = null
    val currentUser: Usuario?
        get() = _currentUser

    val esAdmin: Boolean
        get() = _currentUser?.rol.equals("admin", ignoreCase = true)

    init {
        viewModelScope.launch {
            try {
                crearUsuariosPredeterminados()
                crearEspaciosPredeterminados()
                cargarFeriados()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Cargar preferencia de tema guardada
        darkTheme = prefs.getBoolean("dark_theme", false)
        // Cargar sesión persistente (no resetea al girar el teléfono)
        cargarSesion()
    }

    /** Cambia el tema claro/oscuro y lo persiste. */
    fun cambiarTema(activado: Boolean) {
        darkTheme = activado
        prefs.edit().putBoolean("dark_theme", activado).apply()
    }

    /** API externa: si falla, seguimos considerando solo sábado/domingo. */
    private suspend fun cargarFeriados() {
        when (val resultado = backend.listarFeriados(java.time.LocalDate.now().year)) {
            is ApiResult.Ok -> feriadosChile = resultado.data.map { it.date }.toSet()
            is ApiResult.Error -> Unit
        }
    }

    // ===== SESIÓN PERSISTENTE =====

    /** Guarda la sesión activa en SharedPreferences. */
    private fun guardarSesion() {
        val remoto = usuarioRemoto ?: return
        val editor = prefs.edit()
        editor.putString("sesion_email", remoto.email)
        editor.putString("sesion_nombre", remoto.nombre)
        editor.putString("sesion_rol", remoto.rol)
        editor.putString("sesion_perfil", remoto.perfil)
        editor.putString("sesion_id", remoto.id)
        editor.putBoolean("sesion_modoffline", modoOffline)
        editor.apply()
    }

    /** Carga la sesión guardada si existe (evita reset al girar el teléfono). */
    private fun cargarSesion() {
        val email = prefs.getString("sesion_email", null) ?: return
        val nombre = prefs.getString("sesion_nombre", null) ?: return
        val rol = prefs.getString("sesion_rol", null) ?: return
        val perfil = prefs.getString("sesion_perfil", null) ?: "DOCENTE"
        val id = prefs.getString("sesion_id", null) ?: return
        val modOffline = prefs.getBoolean("sesion_modoffline", false)

        usuarioRemoto = LoginResponseDto(id = id, nombre = nombre, email = email, rol = rol, perfil = perfil)
        // Backend guarda "ADMIN"; la app usa "admin" en minusculas.
        val rolLocal = if (rol.equals("ADMIN", ignoreCase = true)) "admin" else "profesor"
        // El usuario local debe existir en Room (FK de reservas). Si la sesión
        // se restauró de SharedPreferences, creamos su espejo con id real.
        viewModelScope.launch {
            val dao = database.usuarioDao()
            val existente = dao.getByEmail(email)
            val local = if (existente == null) {
                val nuevo = Usuario(0, nombre, email, "", rolLocal)
                nuevo.copy(id = dao.insert(nuevo).toInt())
            } else {
                val actualizado = existente.copy(nombre = nombre, rol = rolLocal)
                dao.update(actualizado)
                actualizado
            }
            _currentUser = local
        }
        modoOffline = modOffline
        if (!modoOffline) {
            currentScreen = "catalogo"
        }
    }

    /** Asegura un usuario local con id válido antes de insertar en Room. */
    private suspend fun usuarioLocalId(): Int {
        _currentUser?.id?.takeIf { it > 0 }?.let { return it }
        val remoto = usuarioRemoto ?: return 0
        val dao = database.usuarioDao()
        val existente = dao.getByEmail(remoto.email)
        if (existente != null) {
            _currentUser = existente
            return existente.id
        }
        val rolLocal = if (remoto.rol.equals("ADMIN", ignoreCase = true)) "admin" else "profesor"
        val nuevo = Usuario(0, remoto.nombre, remoto.email, "", rolLocal)
        val id = dao.insert(nuevo).toInt()
        _currentUser = nuevo.copy(id = id)
        return id
    }

    /** Limpia la sesión persistente. */
    private fun limpiarSesion() {
        val editor = prefs.edit()
        editor.remove("sesion_email")
        editor.remove("sesion_nombre")
        editor.remove("sesion_rol")
        editor.remove("sesion_perfil")
        editor.remove("sesion_id")
        editor.remove("sesion_modoffline")
        editor.apply()
    }

    /**
     * Login contra el backend Spring Boot.
     *
     * Si no hay red (o Render esta dormido), cae a la copia local de Room:
     * la app sigue siendo usable sin conexion (offline-first).
     * Si el backend responde "credenciales invalidas", NO se cae a local,
     * para que el error sea el real.
     */
    suspend fun login(email: String, password: String): LoginResult {
        val correo = email.trim()

        return when (val respuesta = backend.login(correo, password)) {
            is ApiResult.Ok -> {
                val remoto = respuesta.data
                usuarioRemoto = remoto
                modoOffline = false

                // Espejo local: el resto de la app sigue leyendo de Room
                val rolLocal = if (remoto.rol == "ADMIN") "admin" else "profesor"
                val existente = database.usuarioDao().getByEmail(remoto.email)
                val local = if (existente == null) {
                    val nuevo = Usuario(0, remoto.nombre, remoto.email, password, rolLocal)
                    val id = database.usuarioDao().insert(nuevo)
                    nuevo.copy(id = id.toInt())
                } else {
                    val actualizado = existente.copy(
                        nombre = remoto.nombre,
                        password = password,
                        rol = rolLocal
                    )
                    database.usuarioDao().update(actualizado)
                    actualizado
                }
                _currentUser = local
                guardarSesion()
                LoginResult.Ok
            }

            is ApiResult.Error -> {
                if (respuesta.codigo == "CREDENCIALES_INVALIDAS") {
                    LoginResult.CredencialesInvalidas
                } else {
                    val usuario = database.usuarioDao().getByEmail(correo)
                    if (usuario != null && usuario.password == password) {
                        _currentUser = usuario
                        usuarioRemoto = null
                        modoOffline = true
                        LoginResult.Ok
                    } else {
                        LoginResult.Error(respuesta.mensaje)
                    }
                }
            }
        }
    }

    /**
     * Registro de personal vía backend. El rol SIEMPRE es SOLICITANTE;
     * el único ADMIN es el superusuario del backend (colegio.marcela.paz14@gmail.com).
     * Nadie puede autoasignarse ADMIN desde la app.
     */
    suspend fun register(
        nombre: String,
        email: String,
        password: String,
        perfil: String
    ): ApiResult<LoginResponseDto> {
        return backend.registrar(
            nombre = nombre.trim(),
            email = email.trim().lowercase(),
            password = password,
            perfil = perfil
        )
    }

    fun logout() {
        limpiarSesion()
        _currentUser = null
        usuarioRemoto = null
        modoOffline = false
        currentScreen = "login"
        espacioSeleccionado = null
    }

    suspend fun insertEspacio(espacio: Espacio): Long {
        return database.espacioDao().insert(espacio)
    }

    suspend fun getAllEspacios(): List<Espacio> {
        val lista = database.espacioDao().getAll()
        if (lista.isEmpty()) {
            crearEspaciosPredeterminados()
            return database.espacioDao().getAll()
        }
        return lista
    }

    /**
     * Trae los espacios reales del backend (Render + Neon).
     * Si falla la red, devuelve Error para que la UI pueda decidir qué mostrar.
     */
    suspend fun listarEspaciosBackend(): ApiResult<List<Espacio>> =
        try {
            val resultado = backend.listarEspacios()
            when (resultado) {
                is ApiResult.Ok -> ApiResult.Ok(resultado.data.map { dto -> dto.toEspacio() })
                is ApiResult.Error -> resultado
            }
        } catch (e: Exception) {
            ApiResult.Error("ERROR_DESCONOCIDO", e.message ?: "Error al cargar espacios")
        }

    /**
     * Crea una reserva en el backend (Render + Neon).
     */
    suspend fun crearReservaBackend(
        espacioId: String,
        espacioNombre: String,
        usuarioId: String,
        solicitanteNombre: String,
        perfilSolicitante: String,
        fecha: String,
        horaInicio: String,
        horaTermino: String,
        actividad: String,
        cursoGrupo: String,
        asistentes: Int,
        observaciones: String? = null
    ): ApiResult<ReservaDto> {
        val resultado = backend.crearReserva(
            ReservaRequestDto(
                espacioId = espacioId,
                espacioNombre = espacioNombre,
                usuarioId = usuarioId,
                solicitanteNombre = solicitanteNombre,
                perfilSolicitante = perfilSolicitante,
                fecha = fecha,
                horaInicio = horaInicio,
                horaTermino = horaTermino,
                actividad = actividad,
                cursoGrupo = cursoGrupo,
                asistentes = asistentes,
                observaciones = observaciones
            )
        )
        if (resultado is ApiResult.Ok) guardarReservaLocal(resultado.data)
        return resultado
    }

    /** Cambia el estado de un espacio en el backend y en el espejo local. */
    suspend fun cambiarEstadoEspacio(espacio: Espacio, estadoBackend: String): ApiResult<EspacioDto> {
        val resultado = backend.cambiarEstadoEspacio(espacio.id, estadoBackend)
        if (resultado is ApiResult.Ok) {
            database.espacioDao().update(
                espacio.copy(estado = EstadoMapper.espacioDesdeBackend(estadoBackend))
            )
        }
        return resultado
    }

    // ===== SINCRONIZACION API <-> ROOM =====

    private fun ReservaDto.aVista() = ReservaVista(
        idRemota = id,
        espacioId = espacioId,
        espacioNombre = espacioNombre,
        usuarioId = usuarioId,
        solicitanteNombre = solicitanteNombre,
        perfilSolicitante = perfilSolicitante,
        fecha = fecha,
        horaInicio = horaInicio,
        horaTermino = horaTermino,
        actividad = actividad,
        cursoGrupo = cursoGrupo,
        asistentes = asistentes,
        estado = EstadoMapper.reservaDesdeBackend(estado),
        justificacion = justificacionRechazo ?: justificacionCancelacion
    )

    private fun Reserva.aVista() = ReservaVista(
        idLocal = id,
        espacioId = espacioId,
        espacioNombre = espacioNombre,
        fecha = fecha,
        horaInicio = horaInicio,
        horaTermino = horaTermino,
        estado = estado,
        justificacion = justificacionRechazo
    )

    private suspend fun guardarReservaLocal(dto: ReservaDto) {
        val usuarioIdLocal = usuarioLocalId()
        if (usuarioIdLocal == 0) return
        val dao = database.reservaDao()
        val existente = dao.findEquivalente(
            espacioId = dto.espacioId,
            fecha = dto.fecha,
            horaInicio = dto.horaInicio,
            horaTermino = dto.horaTermino,
            usuarioId = usuarioIdLocal
        )
        val copia = dto.toReservaLocal(usuarioIdLocal)
        if (existente == null) dao.insert(copia) else dao.update(copia.copy(id = existente.id))
    }

    /** Mis Reservas: API primero, Room como cache/offline. */
    suspend fun sincronizarMisReservas(): List<ReservaVista> {
        val remotas = when (val r = backend.listarReservas()) {
            is ApiResult.Ok -> r.data
            is ApiResult.Error -> emptyList()
        }
        val mias = if (usuarioRemoto != null) {
            remotas.filter { it.usuarioId == usuarioRemoto?.id }
        } else remotas
        mias.forEach { guardarReservaLocal(it) }

        val usuarioIdLocal = usuarioLocalId()
        val locales = if (usuarioIdLocal > 0) {
            database.reservaDao().getByUsuario(usuarioIdLocal)
        } else emptyList()
        return if (locales.isNotEmpty()) locales.map { it.aVista() } else mias.map { it.aVista() }
    }

    /** Panel Inspectoría: pendientes del backend con fallback local. */
    suspend fun listarPendientesParaAdmin(): ApiResult<List<ReservaVista>> =
        when (val r = backend.listarPendientes()) {
            is ApiResult.Ok -> ApiResult.Ok(r.data.map { it.aVista() })
            is ApiResult.Error -> ApiResult.Ok(database.reservaDao().getPendientes().map { it.aVista() })
        }

    suspend fun aprobarReservaRemota(idRemota: String): ApiResult<ReservaDto> =
        backend.aprobar(idRemota, usuarioRemoto?.id ?: _currentUser?.id.toString())

    suspend fun rechazarReservaRemota(idRemota: String, justificacion: String): ApiResult<ReservaDto> =
        backend.rechazar(idRemota, justificacion, usuarioRemoto?.id ?: _currentUser?.id.toString())

    suspend fun cancelarReservaRemota(idRemota: String): ApiResult<ReservaDto> {
        val resultado = backend.cancelar(
            idRemota,
            "Cancelada por el solicitante",
            usuarioRemoto?.id ?: _currentUser?.id.toString()
        )
        if (resultado is ApiResult.Ok) guardarReservaLocal(resultado.data)
        return resultado
    }

    // ===== BLOQUEOS (Inspectoría) =====

    suspend fun listarBloqueos(): ApiResult<List<BloqueoDto>> = backend.listarBloqueos()

    suspend fun crearBloqueo(request: BloqueoRequestDto): ApiResult<BloqueoDto> =
        backend.crearBloqueo(request)

    suspend fun eliminarBloqueo(id: String): ApiResult<Unit> = backend.eliminarBloqueo(id)

    /**
     * Devuelve los tipos de espacio únicos disponibles para los filtros del catálogo.
     */
    suspend fun obtenerTiposEspacio(): List<String> {
        val espacios = getAllEspacios()
        return listOf("Todos") + espacios.map { it.tipo }.distinct().sorted()
    }

    suspend fun crearEspaciosPredeterminados() {
        val existentes = database.espacioDao().getAll()
        if (existentes.isNotEmpty()) return

        val espacios = listOf(
            Espacio("1", "Laboratorio de Computación", "Laboratorio", "Equipado con 30 PCs, proyector y red de alta velocidad.", "Pabellón A, Piso 2", 30, "08:00 - 18:00", "Disponible"),
            Espacio("2", "Gimnasio Techado", "Deporte", "Cancha multideporte para básquetbol, vóleibol y eventos escolares.", "Pabellón B, Piso 1", 100, "08:00 - 18:00", "Disponible"),
            Espacio("3", "Sala Audiovisual", "Multimedia", "Butacas cómodas, pantalla gigante y sistema de sonido surround.", "Pabellón C, Piso 2", 50, "08:00 - 17:00", "Disponible"),
            Espacio("4", "Biblioteca Central", "Estudio", "Espacio silencioso para estudio y lectura grupal o individual.", "Pabellón A, Piso 1", 60, "08:30 - 17:00", "Disponible")
        )
        for (espacio in espacios) {
            insertEspacio(espacio)
        }
    }

    suspend fun insertReserva(reserva: Reserva): Long {
        return database.reservaDao().insert(reserva)
    }

    suspend fun getReservasByUsuario(usuarioId: Int): List<Reserva> {
        return database.reservaDao().getByUsuario(usuarioId)
    }

    suspend fun getReservasByEspacioYFecha(espacioId: String, fecha: String): List<Reserva> {
        return database.reservaDao().getByEspacioYFecha(espacioId, fecha)
    }

    suspend fun getTodasLasReservas(): List<Reserva> {
        return database.reservaDao().getAll()
    }

    suspend fun getReservasPendientes(): List<Reserva> {
        return database.reservaDao().getPendientes()
    }

    suspend fun aprobarReserva(id: Int) {
        database.reservaDao().actualizarEstado(id, "Aprobada", null)
    }

    suspend fun rechazarReserva(id: Int, justificacion: String) {
        database.reservaDao().actualizarEstado(id, "Rechazada", justificacion)
    }

    suspend fun cancelarReserva(id: Int) {
        database.reservaDao().cancelar(id)
    }

    val archivosFlow = database.archivoDao().getAllFlow()

    fun insertArchivo(archivo: ArchivoItem) {
        viewModelScope.launch {
            database.archivoDao().insert(archivo)
        }
    }

    suspend fun getAllArchivos(): List<ArchivoItem> {
        return database.archivoDao().getAll()
    }

    suspend fun deleteArchivo(id: String) {
        database.archivoDao().delete(id)
    }

    suspend fun crearUsuariosPredeterminados() {
        // Se revisan una por una: si la app ya tenia las cuentas .edu,
        // igual se agregan las .cl que existen en el backend.
        val cuentas = listOf(
            Usuario(0, "Inspectoría General", "colegio.marcela.paz14@gmail.com", "Marcelapaz1", "admin"),
            Usuario(0, "Profesor de Aula", "docente@colegio.cl", "123456", "profesor"),
            Usuario(0, "Inspector Administrador", "admin@colegio.edu", "123456", "admin"),
            Usuario(0, "Profesor Juan Pérez", "profesor@colegio.edu", "123456", "profesor")
        )
        for (cuenta in cuentas) {
            if (database.usuarioDao().getByEmail(cuenta.email) == null) {
                database.usuarioDao().insert(cuenta)
            }
        }
    }
}
