package com.example.myapplication.data.remote

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Resultado de una llamada a la API.
 * Evita que un error de red llegue como excepcion a la UI.
 */
sealed interface ApiResult<out T> {
    data class Ok<T>(val data: T) : ApiResult<T>
    data class Error(val codigo: String, val mensaje: String) : ApiResult<Nothing>
}

/**
 * Unico punto de acceso al backend.
 *
 * La UI nunca llama a [RetrofitClient.api] directamente: pasa por aca,
 * asi los errores se traducen a mensajes entendibles.
 */
class BackendRepository(
    private val api: ApiService = RetrofitClient.api
) {

    private val gson = Gson()

    // ---------- Autenticacion ----------

    suspend fun login(email: String, password: String): ApiResult<LoginResponseDto> =
        llamar { api.login(LoginRequestDto(email = email, password = password)) }

    /**
     * Registro de personal vía backend. El rol SIEMPRE es SOLICITANTE;
     * el único ADMIN es el superusuario del backend.
     */
    suspend fun registrar(
        nombre: String,
        email: String,
        password: String,
        perfil: String
    ): ApiResult<LoginResponseDto> =
        llamar {
            api.registrar(
                RegisterRequestDto(
                    nombre = nombre,
                    email = email,
                    password = password,
                    perfil = perfil
                )
            )
        }

    // ---------- Espacios ----------

    suspend fun listarEspacios(): ApiResult<List<EspacioDto>> =
        llamar { api.listarEspacios() }

    // ---------- Reservas ----------

    suspend fun crearReserva(request: ReservaRequestDto): ApiResult<ReservaDto> =
        llamar { api.crearReserva(request) }

    suspend fun listarReservas(): ApiResult<List<ReservaDto>> =
        llamar { api.listarReservas() }

    suspend fun listarPendientes(): ApiResult<List<ReservaDto>> =
        llamar { api.listarPendientes() }

    suspend fun aprobar(id: String, resueltoPor: String): ApiResult<ReservaDto> =
        llamar {
            api.cambiarEstado(id, EstadoReservaRequestDto(estado = "APROBADA", resueltoPor = resueltoPor))
        }

    suspend fun rechazar(id: String, justificacion: String, resueltoPor: String): ApiResult<ReservaDto> =
        llamar {
            api.cambiarEstado(
                id,
                EstadoReservaRequestDto(estado = "RECHAZADA", justificacion = justificacion, resueltoPor = resueltoPor)
            )
        }

    suspend fun cancelar(id: String, justificacion: String, resueltoPor: String): ApiResult<ReservaDto> =
        llamar {
            api.cambiarEstado(
                id,
                EstadoReservaRequestDto(estado = "CANCELADA", justificacion = justificacion, resueltoPor = resueltoPor)
            )
        }

    // ---------- Bloqueos ----------

    suspend fun listarBloqueos(): ApiResult<List<BloqueoDto>> = llamar { api.listarBloqueos() }

    suspend fun crearBloqueo(request: BloqueoRequestDto): ApiResult<BloqueoDto> =
        llamar { api.crearBloqueo(request) }

    suspend fun eliminarBloqueo(id: String): ApiResult<Unit> = llamar { api.eliminarBloqueo(id) }

    // ---------- Notificaciones ----------

    suspend fun listarNotificaciones(usuarioId: String): ApiResult<List<NotificacionDto>> =
        llamar { api.listarNotificaciones(usuarioId) }

    suspend fun contarNoLeidas(usuarioId: String): ApiResult<Long> =
        llamar { api.contarNoLeidas(usuarioId).count }

    suspend fun marcarLeida(id: String): ApiResult<Unit> = llamar { api.marcarLeida(id) }

    // ---------- Salud ----------

    suspend fun health(): ApiResult<Map<String, Any>> = llamar { api.health() }

    // ---------- Utilidad ----------

    private suspend fun <T> llamar(bloque: suspend () -> T): ApiResult<T> =
        withContext(Dispatchers.IO) {
            try {
                ApiResult.Ok(bloque())
            } catch (e: HttpException) {
                val cuerpo = e.response()?.errorBody()?.string()
                val error = cuerpo?.let {
                    try {
                        gson.fromJson(it, ErrorDto::class.java)
                    } catch (_: JsonSyntaxException) {
                        null
                    }
                }
                val codigo = error?.codigo ?: "HTTP_${e.code()}"
                ApiResult.Error(codigo, mensajeDe(codigo, error?.mensaje))
            } catch (e: UnknownHostException) {
                ApiResult.Error("SIN_CONEXION", "No hay conexión con el servidor. Revisa tu internet.")
            } catch (e: SocketTimeoutException) {
                ApiResult.Error(
                    "TIEMPO_AGOTADO",
                    "El servidor tardó demasiado en responder. Intenta de nuevo."
                )
            } catch (e: IOException) {
                ApiResult.Error("SIN_CONEXION", "No se pudo conectar: ${e.message ?: "error de red"}")
            } catch (e: Exception) {
                ApiResult.Error("ERROR_INESPERADO", e.message ?: "Error inesperado")
            }
        }

    /** Traduce los codigos del backend a mensajes para el usuario. */
    private fun mensajeDe(codigo: String, mensajeBackend: String?): String = when (codigo) {
        "CREDENCIALES_INVALIDAS" -> "Correo o contraseña incorrectos."
        "ANTICIPACION_INSUFICIENTE" -> "Debes reservar con al menos 1 día hábil de anticipación."
        "HORARIO_OCUPADO" -> "Ese horario ya está ocupado. Elige otro bloque."
        "CAPACIDAD_EXCEDIDA" -> "La cantidad de asistentes supera la capacidad del espacio."
        "ESPACIO_NO_DISPONIBLE" -> "Este espacio no está disponible (mantenimiento o fuera de servicio)."
        "ESPACIO_NO_ENCONTRADO" -> "El espacio seleccionado no existe."
        "JUSTIFICACION_REQUERIDA" -> "Debes escribir el motivo (mínimo 10 caracteres)."
        "BLOQUES_NO_CONTIGUOS" -> "Los bloques seleccionados deben ser consecutivos."
        "ESTADO_NO_VALIDO" -> "Ese cambio de estado no está permitido."
        "RESERVA_NO_ENCONTRADA" -> "La reserva no existe o fue eliminada."
        else -> mensajeBackend ?: "Ocurrió un error en el servidor."
    }
}
