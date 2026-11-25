package com.marcela_paz.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BloqueoRequest {

    @NotBlank(message = "espacioId es obligatorio")
    private String espacioId;

    @NotBlank(message = "fecha es obligatoria")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "fecha debe ser YYYY-MM-DD")
    private String fecha;

    @NotBlank(message = "horaInicio es obligatorio")
    @Pattern(regexp = "\\d{2}:\\d{2}", message = "horaInicio debe ser HH:mm")
    private String horaInicio;

    @NotBlank(message = "horaTermino es obligatorio")
    @Pattern(regexp = "\\d{2}:\\d{2}", message = "horaTermino debe ser HH:mm")
    private String horaTermino;

    @NotBlank(message = "motivo es obligatorio")
    private String motivo;

    @NotBlank(message = "creadoPor es obligatorio")
    private String creadoPor;
}