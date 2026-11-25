package com.marcela_paz.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservas",
    indexes = {
        @Index(name = "idx_reserva_espacio_fecha", columnList = "espacio_id, fecha"),
        @Index(name = "idx_reserva_usuario", columnList = "usuario_id")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @Column(name = "id", length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(name = "espacio_id", nullable = false)
    private String espacioId;

    @Column(name = "espacio_nombre", nullable = false)
    private String espacioNombre;

    @Column(name = "usuario_id", nullable = false)
    private String usuarioId;

    @Column(name = "solicitante_nombre", nullable = false)
    private String solicitanteNombre;

    @Column(name = "perfil_solicitante", nullable = false)
    @Enumerated(EnumType.STRING)
    private Usuario.Perfil perfilSolicitante;

    @Column(name = "fecha", nullable = false) // YYYY-MM-DD
    private String fecha;

    @Column(name = "hora_inicio", nullable = false) // HH:mm
    private String horaInicio;

    @Column(name = "hora_termino", nullable = false) // HH:mm
    private String horaTermino;

    @Column(nullable = false)
    private String actividad;

    @Column(nullable = false)
    private String cursoGrupo;

    @Column(nullable = false)
    private Integer asistentes;

    private String observaciones;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoEstado estado;

    private String justificacionRechazo;
    private String justificacionCancelacion;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "resuelto_por")
    private String resueltoPor;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "sync_estado")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SyncEstado syncEstado = SyncEstado.SINCRONIZADO;

    public enum EstadoEstado {
        PENDIENTE, APROBADA, RECHAZADA, CANCELADA, FINALIZADA
    }

    public enum SyncEstado {
        SINCRONIZADO, PENDIENTE, CONFLICTO
    }
}