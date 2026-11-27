package com.example.myapplication.logic

import com.example.myapplication.utils.DateTimeUtils
import java.time.LocalDate

/**
 * Validaciones de formularios y reglas basicas del caso DSY1105.
 * Logica pura: se prueba con tests unitarios sin Android.
 */
object ValidadorReserva {

    fun validarNombre(nombre: String): String? =
        if (nombre.trim().length < 3) "El nombre debe tener al menos 3 caracteres" else null

    fun validarMotivo(motivo: String): String? =
        if (motivo.trim().length < 5) "El motivo debe tener al menos 5 caracteres" else null

    fun validarCurso(curso: String): String? =
        if (curso.trim().length < 2) "Indica el curso o grupo" else null

    fun validarAsistentes(valor: String, capacidadMaxima: Int = Int.MAX_VALUE): String? = when {
        valor.isBlank() -> "Ingresa la cantidad de asistentes"
        valor.trim().toIntOrNull() == null -> "Ingresa un número válido de asistentes"
        valor.trim().toInt() <= 0 -> "La cantidad debe ser mayor a 0"
        valor.trim().toInt() > capacidadMaxima -> "Supera la capacidad del espacio ($capacidadMaxima)"
        else -> null
    }

    fun validarJustificacion(justificacion: String): String? =
        if (justificacion.trim().length < 10) "La justificación debe tener al menos 10 caracteres" else null

    /** Combina las validaciones del formulario de reserva. */
    fun validarFormulario(
        motivo: String,
        curso: String,
        asistentes: String,
        capacidadMaxima: Int
    ): String? = validarMotivo(motivo)
        ?: validarCurso(curso)
        ?: validarAsistentes(asistentes, capacidadMaxima)

    fun esDiaHabil(fecha: LocalDate, feriados: Set<String> = emptySet()): Boolean =
        DateTimeUtils.esDiaHabil(fecha, feriados)

    /** Choque simple de reservas aprobadas/pendientes en el mismo espacio, fecha y bloque. */
    fun hayChoqueHorario(
        espacioId: String,
        fecha: String,
        horaInicio: String,
        reservas: List<Triple<String, String, String>>
    ): Boolean = reservas.any { (espacio, dia, inicio) ->
        espacio == espacioId && dia == fecha && inicio == horaInicio
    }
}
