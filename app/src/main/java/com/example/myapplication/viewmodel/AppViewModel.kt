package com.example.myapplication.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.DatabaseProvider
import com.example.myapplication.model.ArchivoItem
import com.example.myapplication.model.Espacio
import com.example.myapplication.model.Reserva
import com.example.myapplication.model.Usuario
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase by lazy {
        DatabaseProvider.getDatabase(application)
    }

    var currentScreen by mutableStateOf("login")
    var espacioSeleccionado: Espacio? by mutableStateOf(null)

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
    }

    suspend fun login(email: String, password: String): Boolean {
        val usuario = database.usuarioDao().getByEmail(email)
        if (usuario != null && usuario.password == password) {
            _currentUser = usuario
            return true
        }
        return false
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
        val existentes = database.usuarioDao().getAll()
        if (existentes.isNotEmpty()) return

        val admin = Usuario(0, "Inspector Administrador", "admin@colegio.edu", "123456", "admin")
        val profesor = Usuario(0, "Profesor Juan Pérez", "profesor@colegio.edu", "123456", "profesor")
        database.usuarioDao().insert(admin)
        database.usuarioDao().insert(profesor)
    }
}
