package com.example.myapplication.data.remote

/**
 * DTOs: reflejan exactamente el JSON que devuelve el backend Spring Boot.
 *
 * No se usan directamente en la UI; se convierten con [Mappers].
 * Se mantienen separados de las entidades Room para que un cambio en la API
 * no obligue a migrar la base de datos local.
 */

// ---------- Autenticacion ----------

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class LoginResponseDto(
    val id: String,
    val nombre: String,
    val email: String,
    val rol: String,      // ADMIN | SOLICITANTE
    val perfil: String    // INSPECTORIA | DOCENTE | UTP | DIRECCION | PIE
)

/** Registro de personal vía backend. */
data class RegisterRequestDto(
    val nombre: String,
    val email: String,
    val password: String,
    val perfil: String    // INSPECTORIA | DOCENTE | UTP | DIRECCION | PIE
)

// ---------- Recuperación / cambio de contraseña ----------

data class ForgotPasswordRequestDto(val email: String)

data class ForgotPasswordResponseDto(
    val mensaje: String? = null,
    val codigoDemo: String? = null
)

data class ResetPasswordRequestDto(
    val email: String,
    val codigo: String,
    val nuevaPassword: String
)

data class ChangePasswordRequestDto(
    val email: String,
    val passwordActual: String,
    val nuevaPassword: String
)

// ---------- Espacios ----------

data class EspacioDto(
    val id: String,
    val nombre: String,
    val tipo: String,
    val descripcion: String,
    val ubicacion: String,
    val capacidad: Int,
    val horarioFuncionamiento: String,
    val estado: String    // DISPONIBLE | EN_MANTENIMIENTO | FUERA_DE_SERVICIO
)

data class EstadoEspacioRequestDto(
    val estado: String   // DISPONIBLE | EN_MANTENIMIENTO | FUERA_DE_SERVICIO
)

// ---------- Reservas ----------

data class ReservaRequestDto(
    val espacioId: String,
    val espacioNombre: String,
    val usuarioId: String,
    val solicitanteNombre: String,
    val perfilSolicitante: String,
    val fecha: String,          // YYYY-MM-DD
    val horaInicio: String,     // HH:mm
    val horaTermino: String,    // HH:mm
    val actividad: String,
    val cursoGrupo: String,
    val asistentes: Int,
    val observaciones: String? = null
)

data class ReservaDto(
    val id: String,
    val espacioId: String,
    val espacioNombre: String,
    val usuarioId: String,
    val solicitanteNombre: String,
    val perfilSolicitante: String,
    val fecha: String,
    val horaInicio: String,
    val horaTermino: String,
    val actividad: String,
    val cursoGrupo: String,
    val asistentes: Int,
    val observaciones: String? = null,
    val estado: String,                 // PENDIENTE | APROBADA | RECHAZADA | CANCELADA | FINALIZADA
    val justificacionRechazo: String? = null,
    val justificacionCancelacion: String? = null,
    val fechaCreacion: String? = null,
    val resueltoPor: String? = null,
    val fechaResolucion: String? = null,
    val syncEstado: String? = null
)

data class EstadoReservaRequestDto(
    val estado: String,                 // APROBADA | RECHAZADA | CANCELADA
    val justificacion: String? = null,
    val resueltoPor: String
)

// ---------- Bloqueos ----------

data class BloqueoRequestDto(
    val espacioId: String,
    val fecha: String,
    val horaInicio: String,
    val horaTermino: String,
    val motivo: String,
    val creadoPor: String
)

data class BloqueoDto(
    val id: String,
    val espacioId: String,
    val fecha: String,
    val horaInicio: String,
    val horaTermino: String,
    val motivo: String,
    val creadoPor: String,
    val fechaCreacion: String? = null,
    val syncEstado: String? = null
)

// ---------- Notificaciones ----------

data class NotificacionDto(
    val id: String,
    val usuarioId: String,
    val titulo: String,
    val mensaje: String,
    val tipo: String,           // APROBADA | RECHAZADA | CANCELADA | SISTEMA
    val fechaEnvio: String? = null,
    val leida: Boolean = false,
    val reservaId: String? = null
)

data class ConteoNoLeidasDto(
    val count: Long
)

// ---------- Errores ----------

data class ErrorDto(
    val codigo: String? = null,
    val mensaje: String? = null
)
