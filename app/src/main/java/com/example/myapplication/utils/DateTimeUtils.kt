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

    fun esDiaHabil(fecha: LocalDate): Boolean =
        fecha.dayOfWeek != DayOfWeek.SATURDAY && fecha.dayOfWeek != DayOfWeek.SUNDAY

    /**
     * Fecha mínima habil: mañana si es día hábil; si hoy es viernes/sábado/domingo → lunes siguiente.
     */
    fun fechaMinimaHabil(): LocalDate {
        var cursor = LocalDate.now().plusDays(1)
        while (!esDiaHabil(cursor)) {
            cursor = cursor.plusDays(1)
        }
        return cursor
    }

    fun esFechaValidaParaReserva(fecha: String): Boolean {
        val fechaObj = LocalDate.parse(fecha, fechaDbFormatter)
        return esDiaHabil(fechaObj) && !fechaObj.isBefore(fechaMinimaHabil())
    }
}