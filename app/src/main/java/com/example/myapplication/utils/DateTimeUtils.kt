package com.example.myapplication.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object DateTimeUtils {

    val fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val horaFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val fechaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun formatearFecha(fechaDb: String): String {
        return LocalDate.parse(fechaDb).format(fechaFormatter)
    }

    fun formatearFechaParaDb(fechaUsuario: String): String {
        return try {
            LocalDate.parse(fechaUsuario.trim(), fechaFormatter).format(fechaDbFormatter)
        } catch (_: Exception) {
            LocalDate.now().format(fechaDbFormatter)
        }
    }

    fun formatearHora(hora: String): String {
        return LocalTime.parse(hora).format(horaFormatter)
    }

    /** Genera bloques de 45 min basados en el horario de apertura y cierre del espacio. */
    fun generarHorariosBloques(horaApertura: String, horaCierre: String): List<String> {
        val horarios = mutableListOf<String>()
        val inicio = LocalTime.parse(horaApertura.trim(), horaFormatter)
        val finMaximo = LocalTime.parse(horaCierre.trim(), horaFormatter)

        var cursor = inicio
        while (!cursor.plusMinutes(45).isAfter(finMaximo)) {
            val termino = cursor.plusMinutes(45)
            horarios.add("${horaFormatter.format(cursor)} - ${horaFormatter.format(termino)}")
            cursor = termino
        }

        return horarios
    }

    fun calcularHoraTermino(horaInicio: String): String {
        val inicio = LocalTime.parse(horaInicio.split(" - ")[0], horaFormatter)
        return horaFormatter.format(inicio.plusMinutes(45))
    }

    // ===== Validaciones de calendario =====

    /** Día hábil: no sábado/domingo y, si se cargaron feriados, no feriado legal. */
    fun esDiaHabil(fecha: LocalDate, feriados: Set<String> = emptySet()): Boolean =
        fecha.dayOfWeek != DayOfWeek.SATURDAY &&
            fecha.dayOfWeek != DayOfWeek.SUNDAY &&
            !feriados.contains(fecha.format(fechaDbFormatter))

    /** Fecha mínima hábil desde hoy, saltando fines de semana y feriados. */
    fun fechaMinimaHabil(feriados: Set<String> = emptySet()): LocalDate {
        var cursor = LocalDate.now().plusDays(1)
        while (!esDiaHabil(cursor, feriados)) {
            cursor = cursor.plusDays(1)
        }
        return cursor
    }

    fun esFechaValidaParaReserva(
        fecha: String,
        feriados: Set<String> = emptySet()
    ): Boolean {
        val fechaObj = LocalDate.parse(fecha, fechaDbFormatter)
        return esDiaHabil(fechaObj, feriados) && !fechaObj.isBefore(fechaMinimaHabil(feriados))
    }
}