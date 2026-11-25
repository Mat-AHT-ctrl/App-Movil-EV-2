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
import com.example.myapplication.data.remote.LoginResponseDto
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

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase by lazy {
        DatabaseProvider.getDatabase(application)
    }

    private val backend = BackendRepository()

    var currentScreen by mutableStateOf("login")
    var espacioSeleccionado: Espacio? by mutableStateOf(null)

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
        get() = _currentUser?.rol == "admin"

    init {
        viewModelScope.launch {
            try {
                crearUsuariosPredeterminados()
                crearEspaciosPredeterminados()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Cargar preferencia de tema guardada
        darkTheme = prefs.getBoolean("dark_theme", false)
    }

    /** Cambia el tema claro/oscuro y lo persiste. */
    fun cambiarTema(activado: Boolean) {
        darkTheme = activado
        prefs.edit().putBoolean("dark_theme", activado).apply()
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

    suspend fun register(nombre: String, email: String, password: String, rol: String): Boolean {
        if (database.usuarioDao().getByEmail(email) != null) {
            return false
        }
        val usuario = Usuario(0, nombre, email, password, rol)
        val id = database.usuarioDao().insert(usuario)
        _currentUser = usuario.copy(id = id.toInt())
        return true
    }

    fun logout() {
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
            Usuario(0, "Inspectoría General", "admin@colegio.cl", "123456", "admin"),
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
