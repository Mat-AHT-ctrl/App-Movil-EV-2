package com.example.myapplication.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Contrato de la API. Cada funcion corresponde a un endpoint real del
 * backend Spring Boot (ver backend/src/main/java/.../controller).
 */
interface ApiService {

    // ---------- Autenticacion ----------
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @POST("api/auth/register")
    suspend fun registrar(@Body request: RegisterRequestDto): LoginResponseDto

    @POST("api/auth/forgot-password")
    suspend fun solicitarResetPassword(@Body request: ForgotPasswordRequestDto): ForgotPasswordResponseDto

    @POST("api/auth/reset-password")
    suspend fun confirmarResetPassword(@Body request: ResetPasswordRequestDto)

    @POST("api/auth/change-password")
    suspend fun cambiarPassword(@Body request: ChangePasswordRequestDto)

    // ---------- Espacios ----------
    @GET("api/espacios")
    suspend fun listarEspacios(): List<EspacioDto>

    @GET("api/espacios/{id}")
    suspend fun obtenerEspacio(@Path("id") id: String): EspacioDto

    @PATCH("api/espacios/{id}/estado")
    suspend fun cambiarEstadoEspacio(
        @Path("id") id: String,
        @Body request: EstadoEspacioRequestDto
    ): EspacioDto

    // ---------- Reservas ----------
    @POST("api/reservas")
    suspend fun crearReserva(@Body request: ReservaRequestDto): ReservaDto

    @GET("api/reservas")
    suspend fun listarReservas(): List<ReservaDto>

    @GET("api/reservas/pendientes")
    suspend fun listarPendientes(): List<ReservaDto>

    @PATCH("api/reservas/{id}/estado")
    suspend fun cambiarEstado(
        @Path("id") id: String,
        @Body request: EstadoReservaRequestDto
    ): ReservaDto

    @POST("api/reservas/finalizar-vencidas")
    suspend fun finalizarVencidas()

    // ---------- Bloqueos ----------
    @GET("api/bloqueos")
    suspend fun listarBloqueos(): List<BloqueoDto>

    @POST("api/bloqueos")
    suspend fun crearBloqueo(@Body request: BloqueoRequestDto): BloqueoDto

    @DELETE("api/bloqueos/{id}")
    suspend fun eliminarBloqueo(@Path("id") id: String)

    // ---------- Notificaciones ----------
    @GET("api/notificaciones/usuario/{usuarioId}")
    suspend fun listarNotificaciones(@Path("usuarioId") usuarioId: String): List<NotificacionDto>

    @GET("api/notificaciones/usuario/{usuarioId}/no-leidas")
    suspend fun contarNoLeidas(@Path("usuarioId") usuarioId: String): ConteoNoLeidasDto

    @PATCH("api/notificaciones/{id}/leida")
    suspend fun marcarLeida(@Path("id") id: String)

    @PATCH("api/notificaciones/usuario/{usuarioId}/leer-todas")
    suspend fun marcarTodasLeidas(@Path("usuarioId") usuarioId: String)

    // ---------- Salud ----------
    @GET("api/health")
    suspend fun health(): Map<String, Any>
}
