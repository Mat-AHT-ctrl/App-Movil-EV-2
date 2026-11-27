package com.marcela_paz.backend.controller;

import com.marcela_paz.backend.dto.EstadoEspacioRequest;
import com.marcela_paz.backend.model.Espacio;
import com.marcela_paz.backend.repository.EspacioRepository;
import com.marcela_paz.backend.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
public class EspacioController {

    @Autowired
    private EspacioRepository espacioRepository;

    @Autowired
    private NotificacionService notificacionService;

    @GetMapping
    public List<Espacio> listar() {
        return espacioRepository.findAll();
    }

    @GetMapping("/{id}")
    public Espacio obtener(@PathVariable String id) {
        return espacioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ESPACIO_NO_ENCONTRADO"));
    }

    @PatchMapping("/{id}/estado")
    public Espacio cambiarEstado(@PathVariable String id, @RequestBody EstadoEspacioRequest request) {
        Espacio espacio = espacioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ESPACIO_NO_ENCONTRADO"));
        try {
            espacio.setEstado(Espacio.EstadoEstado.valueOf(request.getEstado().trim().toUpperCase()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("ESTADO_NO_VALIDO");
        }
        Espacio guardado = espacioRepository.save(espacio);
        notificacionService.notificarDocentesDeEspacio(
            id,
            "Estado de espacio actualizado",
            "El espacio " + espacio.getNombre() + " ahora está " + guardado.getEstado() + ".",
            "SISTEMA"
        );
        return guardado;
    }
}