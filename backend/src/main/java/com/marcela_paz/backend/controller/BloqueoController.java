package com.marcela_paz.backend.controller;

import com.marcela_paz.backend.dto.BloqueoRequest;
import com.marcela_paz.backend.model.Bloqueo;
import com.marcela_paz.backend.repository.BloqueoRepository;
import com.marcela_paz.backend.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bloqueos")
public class BloqueoController {

    @Autowired
    private BloqueoRepository bloqueoRepository;

    @Autowired
    private NotificacionService notificacionService;

    @GetMapping
    public List<Bloqueo> listar() {
        return bloqueoRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Bloqueo> crear(@RequestBody BloqueoRequest request) {
        Bloqueo bloqueo = Bloqueo.builder()
            .espacioId(request.getEspacioId())
            .fecha(request.getFecha())
            .horaInicio(request.getHoraInicio())
            .horaTermino(request.getHoraTermino())
            .motivo(request.getMotivo())
            .creadoPor(request.getCreadoPor())
            .fechaCreacion(java.time.LocalDateTime.now())
            .build();

        Bloqueo creado = bloqueoRepository.save(bloqueo);
        notificacionService.notificarDocentesDeEspacio(
            creado.getEspacioId(),
            "Nuevo bloqueo de espacio",
            "El espacio " + creado.getEspacioId() + " fue bloqueado el " + creado.getFecha() +
                " (" + creado.getHoraInicio() + " - " + creado.getHoraTermino() + "). Motivo: " +
                creado.getMotivo(),
            "SISTEMA"
        );
        return ResponseEntity.ok(creado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        if (!bloqueoRepository.existsById(id)) {
            throw new IllegalArgumentException("BLOQUEO_NO_ENCONTRADO");
        }
        Bloqueo bloqueo = bloqueoRepository.findById(id).orElse(null);
        bloqueoRepository.deleteById(id);
        if (bloqueo != null) {
            notificacionService.notificarDocentesDeEspacio(
                bloqueo.getEspacioId(),
                "Bloqueo eliminado",
                "Se levantó el bloqueo del espacio " + bloqueo.getEspacioId() +
                    " para el " + bloqueo.getFecha() + ".",
                "SISTEMA"
            );
        }
        return ResponseEntity.noContent().build();
    }
}