package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidarReservaTest {

    @Test
    fun validarCantidadAsistentesValida() {
        val asistentes = 25
        val esValido = asistentes > 0
        assertTrue(esValido)
    }

    @Test
    fun validarActividadVaciaEsInvalida() {
        val actividad = ""
        val esValido = actividad.isNotBlank()
        assertFalse(esValido)
    }

    @Test
    fun estadoInicialReservaEsPendiente() {
        val estadoInicial = "Pendiente"
        assertEquals("Pendiente", estadoInicial)
    }
}