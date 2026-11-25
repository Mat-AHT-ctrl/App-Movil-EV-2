package com.marcela_paz.backend.controller;

import com.marcela_paz.backend.model.Notificacion;
import com.marcela_paz.backend.repository.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @GetMapping("/usuario/{usuarioId}")
    public List<Notificacion> listarPorUsuario(@PathVariable String usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByFechaEnvioDesc(usuarioId);
    }

    @GetMapping("/usuario/{usuarioId}/no-leidas")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(@PathVariable String usuarioId) {
        Long count = notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
        Map<String, Long> resp = new java.util.HashMap<>();
        resp.put("count", count);
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable String id) {
        notificacionRepository.marcarComoLeida(id);
        return ResponseEntity.noContent().build();
    }
}