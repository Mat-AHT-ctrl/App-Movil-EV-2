package com.example.myapplication

import com.example.myapplication.data.remote.EstadoMapper
import com.example.myapplication.logic.ValidadorReserva
import com.example.myapplication.utils.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** Pruebas unitarias de la lógica del caso (sin Android). */
class ReglasNegocioTest {

    private val feriados = setOf("2026-09-18", "2026-12-25")

    // ---------- DateTimeUtils ----------

    @Test
    fun formatearFecha_dbAUsuario() {
        assertEquals("27/11/2026", DateTimeUtils.formatearFecha("2026-11-27"))
    }

    @Test
    fun formatearFechaParaDb_usuarioADb() {
        assertEquals("2026-11-27", DateTimeUtils.formatearFechaParaDb("27/11/2026"))
    }

    @Test
    fun formatearHora() {
        assertEquals("08:15", DateTimeUtils.formatearHora("08:15"))
    }

    @Test
    fun generarHorariosBloques_cuentaBloques() {
        val bloques = DateTimeUtils.generarHorariosBloques("08:15", "12:00")
        assertEquals(5, bloques.size)
    }

    @Test
    fun generarHorariosBloques_primerBloque() {
        assertEquals("08:15 - 09:00", DateTimeUtils.generarHorariosBloques("08:15", "12:00").first())
    }

    @Test
    fun generarHorariosBloques_ultimoBloque() {
        assertEquals("11:15 - 12:00", DateTimeUtils.generarHorariosBloques("08:15", "12:00").last())
    }

    @Test
    fun calcularHoraTermino() {
        assertEquals("09:00", DateTimeUtils.calcularHoraTermino("08:15"))
    }

    @Test
    fun esDiaHabil_laborable() {
        assertTrue(DateTimeUtils.esDiaHabil(LocalDate.of(2026, 11, 27)))
    }

    @Test
    fun esDiaHabil_sabado() {
        assertFalse(DateTimeUtils.esDiaHabil(LocalDate.of(2026, 11, 28)))
    }

    @Test
    fun esDiaHabil_domingo() {
        assertFalse(DateTimeUtils.esDiaHabil(LocalDate.of(2026, 11, 29)))
    }

    @Test
    fun esDiaHabil_feriadoLegal() {
        assertFalse(DateTimeUtils.esDiaHabil(LocalDate.of(2026, 9, 18), feriados))
    }

    @Test
    fun esDiaHabil_fechaNoFeriadoConFeriadosCargados() {
        assertTrue(DateTimeUtils.esDiaHabil(LocalDate.of(2026, 11, 27), feriados))
    }

    @Test
    fun fechaMinimaHabil_esLaborable() {
        val minima = DateTimeUtils.fechaMinimaHabil(setOf("2999-01-01"))
        assertTrue(minima.dayOfWeek != DayOfWeek.SATURDAY && minima.dayOfWeek != DayOfWeek.SUNDAY)
        assertFalse(minima.isBefore(LocalDate.now().plusDays(1)))
    }

    @Test
    fun esFechaValidaParaReserva_fechaPasada() {
        assertFalse(DateTimeUtils.esFechaValidaParaReserva("2020-01-01", feriados))
    }

    @Test
    fun esFechaValidaParaReserva_feriado() {
        assertFalse(DateTimeUtils.esFechaValidaParaReserva("2999-01-01", setOf("2999-01-01")))
    }

    // ---------- EstadoMapper ----------

    @Test
    fun estadoReservaDesdeBackend_pendiente() {
        assertEquals("Pendiente", EstadoMapper.reservaDesdeBackend("PENDIENTE"))
    }

    @Test
    fun estadoReservaDesdeBackend_aprobada() {
        assertEquals("Aprobada", EstadoMapper.reservaDesdeBackend("APROBADA"))
    }

    @Test
    fun estadoReservaIdaYVuelta() {
        assertEquals("RECHAZADA", EstadoMapper.reservaHaciaBackend(EstadoMapper.reservaDesdeBackend("RECHAZADA")))
    }

    @Test
    fun estadoEspacioDesdeBackend_mantenimiento() {
        assertEquals("Mantenimiento", EstadoMapper.espacioDesdeBackend("EN_MANTENIMIENTO"))
    }

    // ---------- ValidadorReserva ----------

    @Test
    fun validarNombre_menorA3() {
        assertNotNull(ValidadorReserva.validarNombre("Jo"))
    }

    @Test
    fun validarNombre_valido() {
        assertNull(ValidadorReserva.validarNombre("Prof. Carlos Soto"))
    }

    @Test
    fun validarMotivo_menorA5() {
        assertNotNull(ValidadorReserva.validarMotivo("Tall"))
    }

    @Test
    fun validarAsistentes_cero() {
        assertNotNull(ValidadorReserva.validarAsistentes("0"))
    }

    @Test
    fun validarAsistentes_superaCapacidad() {
        assertNotNull(ValidadorReserva.validarAsistentes("40", 30))
    }

    @Test
    fun validarAsistentes_valido() {
        assertNull(ValidadorReserva.validarAsistentes("25", 30))
    }

    @Test
    fun validarJustificacion_menorA10() {
        assertNotNull(ValidadorReserva.validarJustificacion("corto"))
    }

    @Test
    fun validarFormulario_todoValido() {
        assertNull(ValidadorReserva.validarFormulario("Taller de lectura", "3A", "10", 30))
    }

    @Test
    fun hayChoqueHorario_mismoBloque() {
        val reservas = listOf(Triple("1", "2026-11-27", "08:15"))
        assertTrue(ValidadorReserva.hayChoqueHorario("1", "2026-11-27", "08:15", reservas))
    }

    @Test
    fun hayChoqueHorario_otroEspacio() {
        val reservas = listOf(Triple("1", "2026-11-27", "08:15"))
        assertFalse(ValidadorReserva.hayChoqueHorario("2", "2026-11-27", "08:15", reservas))
    }
}
