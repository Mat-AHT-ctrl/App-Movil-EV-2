package com.example.myapplication.data.remote

import com.example.myapplication.model.Espacio
import com.example.myapplication.model.Reserva

/**
 * Conversion entre el JSON del backend (enums en MAYUSCULAS) y los modelos
 * que ya usa la app (texto capitalizado: "Disponible", "Pendiente", ...).
 *
 * Si se agrega un estado nuevo en el backend, solo hay que tocar aqui.
 */
object EstadoMapper {

    // ---------- Espacio ----------

    fun espacioDesdeBackend(estado: String): String = when (estado.uppercase()) {
        "DISPONIBLE" -> "Disponible"
        "EN_MANTENIMIENTO" -> "Mantenimiento"
        "FUERA_DE_SERVICIO" -> "Fuera de servicio"
        else -> estado
    }

    fun espacioHaciaBackend(estado: String): String = when (estado) {
        "Disponible" -> "DISPONIBLE"
        "Mantenimiento" -> "EN_MANTENIMIENTO"
        "Fuera de servicio" -> "FUERA_DE_SERVICIO"
        else -> estado.uppercase()
    }

    // ---------- Reserva ----------

    fun reservaDesdeBackend(estado: String): String = when (estado.uppercase()) {
        "PENDIENTE" -> "Pendiente"
        "APROBADA" -> "Aprobada"
        "RECHAZADA" -> "Rechazada"
        "CANCELADA" -> "Cancelada"
        "FINALIZADA" -> "Finalizada"
        else -> estado
    }

    fun reservaHaciaBackend(estado: String): String = when (estado) {
        "Pendiente" -> "PENDIENTE"
        "Aprobada" -> "APROBADA"
        "Rechazada" -> "RECHAZADA"
        "Cancelada" -> "CANCELADA"
        "Finalizada" -> "FINALIZADA"
        else -> estado.uppercase()
    }
}

/**
 * DTO -> entidad Room.
 *
 * Solo los espacios se mapean directo, porque su estructura coincide.
 * Las reservas del backend traen id UUID y usuarioId String, mientras que
 * la tabla local usa id Int y usuarioId Int: esa conversion se resuelve en
 * la capa de repositorio cuando se implemente la sincronizacion.
 */
fun EspacioDto.toEspacio(): Espacio = Espacio(
    id = id,
    nombre = nombre,
    tipo = tipo,
    descripcion = descripcion,
    ubicacion = ubicacion,
    capacidad = capacidad,
    horarioFuncionamiento = horarioFuncionamiento,
    estado = EstadoMapper.espacioDesdeBackend(estado)
)

/** Reserva del backend -> entidad Room (espejo local). */
fun ReservaDto.toReservaLocal(usuarioIdLocal: Int): Reserva = Reserva(
    espacioId = espacioId,
    espacioNombre = espacioNombre,
    usuarioId = usuarioIdLocal,
    fecha = fecha,
    horaInicio = horaInicio,
    horaTermino = horaTermino,
    estado = EstadoMapper.reservaDesdeBackend(estado),
    justificacionRechazo = justificacionRechazo
)
