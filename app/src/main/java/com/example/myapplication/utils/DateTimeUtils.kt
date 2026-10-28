package com.example.myapplication.utils

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

    fun generarHorariosBloques(): List<String> {
        val horarios = mutableListOf<String>()
        var inicio = LocalTime.of(8, 15)
        val finMaximo = LocalTime.of(17, 30)

        while (inicio.plusMinutes(45).isBefore(finMaximo) || inicio.plusMinutes(45).equals(finMaximo)) {
            val termino = inicio.plusMinutes(45)
            horarios.add("${horaFormatter.format(inicio)} - ${horaFormatter.format(termino)}")
            inicio = termino
        }

        return horarios
    }

    fun calcularHoraTermino(horaInicio: String): String {
        val inicio = LocalTime.parse(horaInicio.split(" - ")[0], horaFormatter)
        return horaFormatter.format(inicio.plusMinutes(45))
    }
}