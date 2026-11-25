package com.marcela_paz.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check usado por Render.
 *
 * Siempre responde 200 para que el servicio no se reinicie si Neon
 * esta dormido; el estado de la base se informa en el cuerpo.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("estado", "ok");
        resp.put("servicio", "marcela-paz-backend");
        resp.put("hora", OffsetDateTime.now().toString());

        String db = "ok";
        try (Connection c = dataSource.getConnection()) {
            if (!c.isValid(3)) {
                db = "sin_respuesta";
            }
        } catch (Exception e) {
            db = "error: " + e.getClass().getSimpleName();
        }
        resp.put("baseDeDatos", db);

        return ResponseEntity.ok(resp);
    }
}
