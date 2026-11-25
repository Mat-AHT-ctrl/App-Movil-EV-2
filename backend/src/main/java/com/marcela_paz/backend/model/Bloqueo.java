package com.marcela_paz.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bloqueos",
    indexes = @Index(name = "idx_bloqueo_espacio_fecha", columnList = "espacio_id, fecha"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bloqueo {

    @Id
    @Column(name = "id", length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(name = "espacio_id", nullable = false)
    private String espacioId;

    @Column(nullable = false)
    private String fecha; // YYYY-MM-DD

    @Column(nullable = false)
    private String horaInicio; // HH:mm

    @Column(nullable = false)
    private String horaTermino; // HH:mm

    @Column(nullable = false)
    private String motivo;

    @Column(name = "creado_por", nullable = false)
    private String creadoPor;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "sync_estado")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Reserva.SyncEstado syncEstado = Reserva.SyncEstado.SINCRONIZADO;
}