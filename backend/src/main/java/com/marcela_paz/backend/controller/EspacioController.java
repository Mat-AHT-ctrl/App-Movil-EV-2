package com.marcela_paz.backend.controller;

import com.marcela_paz.backend.model.Espacio;
import com.marcela_paz.backend.repository.EspacioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
public class EspacioController {

    @Autowired
    private EspacioRepository espacioRepository;

    @GetMapping
    public List<Espacio> listar() {
        return espacioRepository.findAll();
    }

    @GetMapping("/{id}")
    public Espacio obtener(@PathVariable String id) {
        return espacioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ESPACIO_NO_ENCONTRADO"));
    }
}