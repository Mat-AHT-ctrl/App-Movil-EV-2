package com.marcela_paz.backend.service;

import com.marcela_paz.backend.model.Reserva;
import com.marcela_paz.backend.repository.ReservaRepository;
import com.marcela_paz.backend.repository.BloqueoRepository;
import com.marcela_paz.backend.repository.EspacioRepository;
import com.marcela_paz.backend.model.Espacio;
import com.marcela_paz.backend.model.Bloqueo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private BloqueoRepository bloqueoRepository;

    @Autowired
    private EspacioRepository espacioRepository;

    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Render corre en UTC; las reglas del colegio usan hora de Chile. */
    private static final ZoneId ZONA_COLEGIO = ZoneId.of("America/Santiago");

    public Reserva crearReserva(Reserva reserva) {
        // R14: obtener el espacio
        Optional<Espacio> espacioOpt = espacioRepository.findById(reserva.getEspacioId());
        if (espacioOpt.isEmpty()) {
            throw new IllegalArgumentException("ESPACIO_NO_ENCONTRADO");
        }
        Espacio espacio = espacioOpt.get();

        // R4: estado del espacio
        if (espacio.getEstado() == Espacio.EstadoEstado.EN_MANTENIMIENTO
            || espacio.getEstado() == Espacio.EstadoEstado.FUERA_DE_SERVICIO) {
            throw new IllegalArgumentException("ESPACIO_NO_DISPONIBLE");
        }

        // R1: anticipación mínima 1 día hábil
        validarAnticipacion(reserva.getFecha());

        // R3: capacidad
        if (reserva.getAsistentes() <= 0 || reserva.getAsistentes() > espacio.getCapacidad()) {
            throw new IllegalArgumentException("CAPACIDAD_EXCEDIDA");
        }

        // R9: bloques contiguos
        if (!bloquesContiguos(reserva.getHoraInicio(), reserva.getHoraTermino())) {
            throw new IllegalArgumentException("BLOQUES_NO_CONTIGUOS");
        }

        // R2: sin solapamiento con reservas Pendiente/Aprobada
        List<Reserva> ocupadas = reservaRepository.findOcupadasPorSolapamiento(
            reserva.getEspacioId(),
            reserva.getFecha(),
            reserva.getHoraInicio(),
            reserva.getHoraTermino()
        );
        if (!ocupadas.isEmpty()) {
            throw new IllegalArgumentException("HORARIO_OCUPADO");
        }

        // R2: sin solapamiento con bloqueos
        List<Bloqueo> bloqueos = bloqueoRepository.findBloqueosPorSolapamiento(
            reserva.getEspacioId(),
            reserva.getFecha(),
            reserva.getHoraInicio(),
            reserva.getHoraTermino()
        );
        if (!bloqueos.isEmpty()) {
            throw new IllegalArgumentException("HORARIO_OCUPADO");
        }

        reserva.setEstado(Reserva.EstadoEstado.PENDIENTE);
        reserva.setFechaCreacion(LocalDateTime.now(ZONA_COLEGIO));
        reserva.setSyncEstado(Reserva.SyncEstado.SINCRONIZADO);
        return reservaRepository.save(reserva);
    }

    public Reserva aprobarReserva(String id, String resueltoPor) {
        Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("RESERVA_NO_ENCONTRADA"));
        if (reserva.getEstado() != Reserva.EstadoEstado.PENDIENTE) {
            throw new IllegalArgumentException("ESTADO_NO_VALIDO");
        }
        reserva.setEstado(Reserva.EstadoEstado.APROBADA);
        reserva.setResueltoPor(resueltoPor);
        reserva.setFechaResolucion(LocalDateTime.now(ZONA_COLEGIO));
        return reservaRepository.save(reserva);
    }

    public Reserva rechazarReserva(String id, String justificacion, String resueltoPor) {
        if (justificacion == null || justificacion.trim().length() < 10) {
            throw new IllegalArgumentException("JUSTIFICACION_REQUERIDA");
        }
        Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("RESERVA_NO_ENCONTRADA"));
        if (reserva.getEstado() != Reserva.EstadoEstado.PENDIENTE) {
            throw new IllegalArgumentException("ESTADO_NO_VALIDO");
        }
        reserva.setEstado(Reserva.EstadoEstado.RECHAZADA);
        reserva.setJustificacionRechazo(justificacion);
        reserva.setResueltoPor(resueltoPor);
        reserva.setFechaResolucion(LocalDateTime.now(ZONA_COLEGIO));
        return reservaRepository.save(reserva);
    }

    public Reserva cancelarReserva(String id, String justificacion) {
        Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("RESERVA_NO_ENCONTRADA"));
        if (reserva.getEstado() != Reserva.EstadoEstado.PENDIENTE) {
            throw new IllegalArgumentException("ESTADO_NO_VALIDO");
        }
        reserva.setEstado(Reserva.EstadoEstado.CANCELADA);
        reserva.setJustificacionCancelacion(justificacion);
        reserva.setFechaResolucion(LocalDateTime.now(ZONA_COLEGIO));
        return reservaRepository.save(reserva);
    }

    public List<Reserva> listarPendientes() {
        return reservaRepository.findByEstado(Reserva.EstadoEstado.PENDIENTE);
    }

    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    public void finalizarReservasVencidas() {
        LocalDate hoy = LocalDate.now(ZONA_COLEGIO);
        LocalTime ahora = LocalTime.now(ZONA_COLEGIO);
        List<Reserva> aprobadas = reservaRepository.findByEstado(Reserva.EstadoEstado.APROBADA);
        for (Reserva r : aprobadas) {
            LocalDate fechaReserva = LocalDate.parse(r.getFecha());
            LocalTime termino = LocalTime.parse(r.getHoraTermino());
            if (fechaReserva.isBefore(hoy)
                || (fechaReserva.isEqual(hoy) && termino.isBefore(ahora))) {
                r.setEstado(Reserva.EstadoEstado.FINALIZADA);
                reservaRepository.save(r);
            }
        }
    }

    private void validarAnticipacion(String fechaStr) {
        LocalDate fechaReserva = LocalDate.parse(fechaStr);
        LocalDate hoy = LocalDate.now(ZONA_COLEGIO);
        LocalDate minima = calcularFechaMinimaHabil(hoy);
        if (fechaReserva.isBefore(minima)) {
            throw new IllegalArgumentException("ANTICIPACION_INSUFICIENTE");
        }
    }

    private LocalDate calcularFechaMinimaHabil(LocalDate hoy) {
        LocalDate candidato = hoy.plusDays(1);
        while (!esDiaHabil(candidato)) {
            candidato = candidato.plusDays(1);
        }
        return candidato;
    }

    private boolean esDiaHabil(LocalDate fecha) {
        java.time.DayOfWeek dia = fecha.getDayOfWeek();
        return dia != java.time.DayOfWeek.SATURDAY && dia != java.time.DayOfWeek.SUNDAY;
    }

    private boolean bloquesContiguos(String inicio, String termino) {
        LocalTime hi = LocalTime.parse(inicio, HORA_FMT);
        LocalTime ht = LocalTime.parse(termino, HORA_FMT);
        return ht.isAfter(hi);
    }
}