package com.marcela_paz.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.UUID;

@Entity
@Table(name = "espacios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Espacio {

    @Id
    @Column(name = "id", length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private String ubicacion;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(nullable = false)
    private String horarioFuncionamiento; // "08:00 - 18:00"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoEstado estado;

    public enum EstadoEstado {
        DISPONIBLE, EN_MANTENIMIENTO, FUERA_DE_SERVICIO
    }
}