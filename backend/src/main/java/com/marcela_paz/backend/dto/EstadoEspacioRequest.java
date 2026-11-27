package com.marcela_paz.backend.dto;

import lombok.Data;

/** Cambio de estado de un espacio (DISPONIBLE, EN_MANTENIMIENTO, FUERA_DE_SERVICIO). */
@Data
public class EstadoEspacioRequest {
    private String estado;
}
