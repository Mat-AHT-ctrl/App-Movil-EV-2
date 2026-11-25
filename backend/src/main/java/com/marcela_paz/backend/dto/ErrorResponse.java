package com.marcela_paz.backend.dto;

import lombok.Data;

@Data
public class ErrorResponse {
    private String codigo;
    private String mensaje;

    public ErrorResponse(String codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }
}