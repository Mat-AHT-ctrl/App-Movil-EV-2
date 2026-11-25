package com.marcela_paz.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EstadoReservaRequest {

    @NotBlank(message = "estado es obligatorio")
    private String estado; // APROBADA o RECHAZADA

    private String justificacion; // obligatoria si estado es RECHAZADA

    @NotBlank(message = "resueltoPor es obligatorio")
    private String resueltoPor;
}