package com.marcela_paz.backend.service;

import com.marcela_paz.backend.model.Notificacion;
import com.marcela_paz.backend.model.Reserva;
import com.marcela_paz.backend.model.Usuario;
import com.marcela_paz.backend.repository.NotificacionRepository;
import com.marcela_paz.backend.repository.ReservaRepository;
import com.marcela_paz.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final ZoneId ZONA_COLEGIO = ZoneId.of("America/Santiago");

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    /** En este proyecto el id de usuario es un UUID (String), no un Long. */
    public Notificacion crearNotificacion(String usuarioId, String titulo, String mensaje,
                                           String tipo, String reservaId) {
        if (usuarioId == null || usuarioId.isBlank()) return null;
        try {
            Notificacion.Tipo tipoEnum;
            try {
                tipoEnum = Notificacion.Tipo.valueOf(tipo.trim().toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                tipoEnum = Notificacion.Tipo.SISTEMA;
            }
            return notificacionRepository.save(Notificacion.builder()
                .usuarioId(usuarioId)
                .titulo(titulo)
                .mensaje(mensaje)
                .tipo(tipoEnum)
                .fechaEnvio(LocalDateTime.now(ZONA_COLEGIO))
                .leida(false)
                .reservaId(reservaId)
                .build());
        } catch (Exception ex) {
            // Las notificaciones no deben romper el flujo de negocio.
            log.warn("No se pudo crear notificación para {}: {}", usuarioId, ex.getMessage());
            return null;
        }
    }

    /** Sobrecarga por si algún llamador usa un id numérico. */
    public Notificacion crearNotificacion(Long usuarioId, String titulo, String mensaje,
                                           String tipo, Long reservaId) {
        return crearNotificacion(
            usuarioId == null ? null : String.valueOf(usuarioId),
            titulo,
            mensaje,
            tipo,
            reservaId == null ? null : String.valueOf(reservaId)
        );
    }

    /** Avisa a ADMIN e INSPECTORÍA. */
    public void notificarAdmins(String titulo, String mensaje, String tipo, String reservaId) {
        for (Usuario usuario : usuarioRepository.findAll()) {
            boolean esAdmin = usuario.getRol() == Usuario.Rol.ADMIN;
            boolean esInspectoria = usuario.getPerfil() == Usuario.Perfil.INSPECTORIA;
            if (esAdmin || esInspectoria) {
                crearNotificacion(usuario.getId(), titulo, mensaje, tipo, reservaId);
            }
        }
    }

    /** Avisa a todo el personal solicitationte (rol SOLICITANTE). */
    public void notificarTodosDocentes(String titulo, String mensaje, String tipo) {
        for (Usuario usuario : usuarioRepository.findAll()) {
            if (usuario.getRol() == Usuario.Rol.SOLICITANTE) {
                crearNotificacion(usuario.getId(), titulo, mensaje, tipo, null);
            }
        }
    }

    /** Avisa a los docentes con reserva PENDIENTE/APROBADA en ese espacio. */
    public void notificarDocentesDeEspacio(String espacioId, String titulo, String mensaje, String tipo) {
        if (espacioId == null) return;
        Set<String> usuarios = new HashSet<>();
        for (Reserva reserva : reservaRepository.findAll()) {
            if (espacioId.equals(reserva.getEspacioId()) &&
                (reserva.getEstado() == Reserva.EstadoEstado.PENDIENTE ||
                 reserva.getEstado() == Reserva.EstadoEstado.APROBADA) &&
                reserva.getUsuarioId() != null) {
                usuarios.add(reserva.getUsuarioId());
            }
        }
        for (String usuarioId : usuarios) {
            crearNotificacion(usuarioId, titulo, mensaje, tipo, null);
        }
    }
}
