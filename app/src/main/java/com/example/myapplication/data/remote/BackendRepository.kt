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


class BackendRepository(
    private val api: ApiService = RetrofitClient.api
) {

    private val gson = Gson()

    // ---------- Autenticacion ----------

    suspend fun login(email: String, password: String): ApiResult<LoginResponseDto> =
        llamar { api.login(LoginRequestDto(email = email, password = password)) }


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

    // ---------- Recuperación / cambio de contraseña ----------

    suspend fun solicitarResetPassword(email: String): ApiResult<ForgotPasswordResponseDto> =
        llamar { api.solicitarResetPassword(ForgotPasswordRequestDto(email)) }

    suspend fun confirmarResetPassword(
        email: String,
        codigo: String,
        nuevaPassword: String
    ): ApiResult<Unit> =
        llamar { api.confirmarResetPassword(ResetPasswordRequestDto(email, codigo, nuevaPassword)) }

    suspend fun cambiarPassword(
        email: String,
        passwordActual: String,
        nuevaPassword: String
    ): ApiResult<Unit> =
        llamar { api.cambiarPassword(ChangePasswordRequestDto(email, passwordActual, nuevaPassword)) }

    // ---------- Espacios ----------

    suspend fun listarEspacios(): ApiResult<List<EspacioDto>> =
        llamar { api.listarEspacios() }

    suspend fun cambiarEstadoEspacio(id: String, estado: String): ApiResult<EspacioDto> =
        llamar { api.cambiarEstadoEspacio(id, EstadoEspacioRequestDto(estado)) }

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

    suspend fun marcarTodasLeidas(usuarioId: String): ApiResult<Unit> =
        llamar { api.marcarTodasLeidas(usuarioId) }

    // ---------- API externa: feriados de Chile ----------

    suspend fun listarFeriados(anio: Int): ApiResult<List<FeriadoDto>> =
        llamar { RetrofitClient.feriadosApi.obtenerFeriadosChile(anio) }

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
        "USUARIO_NO_ENCONTRADO" -> "No existe una cuenta con ese correo."
        "CODIGO_INVALIDO" -> "El código es incorrecto o ha expirado."
        "PASSWORD_NO_VALIDA" -> "La contraseña debe tener al menos 6 caracteres."
        "EMAIL_YA_EXISTE" -> "Ya existe una cuenta con ese correo."
        "PERFIL_NO_VALIDO" -> "El cargo seleccionado no es válido."
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
