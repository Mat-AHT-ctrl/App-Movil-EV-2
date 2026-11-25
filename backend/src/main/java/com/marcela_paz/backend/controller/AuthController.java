package com.marcela_paz.backend.controller;

import com.marcela_paz.backend.dto.LoginRequest;
import com.marcela_paz.backend.model.Usuario;
import com.marcela_paz.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("CREDENCIALES_INVALIDAS"));

        if (!usuario.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("CREDENCIALES_INVALIDAS");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", usuario.getId());
        response.put("nombre", usuario.getNombre());
        response.put("email", usuario.getEmail());
        response.put("rol", usuario.getRol());
        response.put("perfil", usuario.getPerfil());
        return ResponseEntity.ok(response);
    }
}