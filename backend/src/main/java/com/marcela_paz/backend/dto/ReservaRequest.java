package com.marcela_paz.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReservaRequest {

    @NotBlank(message = "espacioId es obligatorio")
    private String espacioId;

    @NotBlank(message = "espacioNombre es obligatorio")
    private String espacioNombre;

    @NotBlank(message = "usuarioId es obligatorio")
    private String usuarioId;

    @NotBlank(message = "solicitanteNombre es obligatorio")
    private String solicitanteNombre;

    @NotNull(message = "perfilSolicitante es obligatorio")
    private String perfilSolicitante;

    @NotBlank(message = "fecha es obligatoria")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "fecha debe ser YYYY-MM-DD")
    private String fecha;

    @NotBlank(message = "horaInicio es obligatorio")
    @Pattern(regexp = "\\d{2}:\\d{2}", message = "horaInicio debe ser HH:mm")
    private String horaInicio;

    @NotBlank(message = "horaTermino es obligatorio")
    @Pattern(regexp = "\\d{2}:\\d{2}", message = "horaTermino debe ser HH:mm")
    private String horaTermino;

    @NotBlank(message = "actividad es obligatoria")
    private String actividad;

    @NotBlank(message = "cursoGrupo es obligatorio")
    private String cursoGrupo;

    @NotNull(message = "asistentes es obligatorio")
    @Min(value = 1, message = "asistentes debe ser mayor a 0")
    private Integer asistentes;

    private String observaciones;
}