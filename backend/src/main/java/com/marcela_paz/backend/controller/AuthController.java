package com.marcela_paz.backend.controller;

import com.marcela_paz.backend.dto.LoginRequest;
import com.marcela_paz.backend.dto.RegisterRequest;
import com.marcela_paz.backend.model.Usuario;
import com.marcela_paz.backend.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Locale;
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

        return ResponseEntity.ok(datosUsuario(usuario));
    }

    /**
     * Registro de personal. El rol SIEMPRE es SOLICITANTE: nadie puede
     * autoasignarse ADMIN desde la app. El perfil no cambia el rol.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("EMAIL_YA_EXISTE");
        }

        Usuario.Perfil perfil;
        try {
            perfil = Usuario.Perfil.valueOf(request.getPerfil().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("PERFIL_NO_VALIDO");
        }

        Usuario usuario = usuarioRepository.save(Usuario.builder()
            .nombre(request.getNombre().trim())
            .email(email)
            .password(request.getPassword())
            .rol(Usuario.Rol.SOLICITANTE)
            .perfil(perfil)
            .build());

        return ResponseEntity.ok(datosUsuario(usuario));
    }

    /** Misma respuesta para login y registro (LoginResponseDto). */
    private Map<String, Object> datosUsuario(Usuario usuario) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", usuario.getId());
        response.put("nombre", usuario.getNombre());
        response.put("email", usuario.getEmail());
        response.put("rol", usuario.getRol());
        response.put("perfil", usuario.getPerfil());
        return response;
    }
}
