package com.marcela_paz.backend.controller;

import com.marcela_paz.backend.dto.ChangePasswordRequest;
import com.marcela_paz.backend.dto.ErrorResponse;
import com.marcela_paz.backend.dto.ForgotPasswordRequest;
import com.marcela_paz.backend.dto.LoginRequest;
import com.marcela_paz.backend.dto.RegisterRequest;
import com.marcela_paz.backend.dto.ResetPasswordRequest;
import com.marcela_paz.backend.model.Usuario;
import com.marcela_paz.backend.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    /** Códigos de recuperación en memoria, vigentes por 15 minutos. */
    private final ConcurrentHashMap<String, CodigoVigente> codigos = new ConcurrentHashMap<>();

    private record CodigoVigente(String codigo, Instant expira) { }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(normalizar(request.getEmail()))
            .orElseThrow(() -> new IllegalArgumentException("CREDENCIALES_INVALIDAS"));

        if (!coincidePassword(request.getPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("CREDENCIALES_INVALIDAS");
        }

        return ResponseEntity.ok(datosUsuario(usuario));
    }

    /** Compatible con contraseñas antiguas en texto plano y nuevas con BCrypt. */
    private boolean coincidePassword(String enviada, String guardada) {
        if (enviada == null || guardada == null) return false;
        if (guardada.equals(enviada)) return true;
        return guardada.startsWith("$2") && passwordEncoder.matches(enviada, guardada);
    }

    /**
     * Registro de personal. El rol SIEMPRE es SOLICITANTE: nadie puede
     * autoasignarse ADMIN desde la app. El perfil no cambia el rol.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String email = normalizar(request.getEmail());

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
            .password(passwordEncoder.encode(request.getPassword()))
            .rol(Usuario.Rol.SOLICITANTE)
            .perfil(perfil)
            .build());

        return ResponseEntity.ok(datosUsuario(usuario));
    }

    /** Paso 1: genera un código de 6 dígitos vigente 15 minutos. */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String email = normalizar(request.email());

        if (email.isEmpty() || usuarioRepository.findByEmail(email).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                    "USUARIO_NO_ENCONTRADO",
                    "No existe una cuenta con ese correo."
                ));
        }

        String codigo = String.valueOf(100000 + random.nextInt(900000));
        codigos.put(email, new CodigoVigente(codigo, Instant.now().plus(Duration.ofMinutes(15))));

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Código generado");
        respuesta.put("codigoDemo", codigo);
        return ResponseEntity.ok(respuesta);
    }

    /** Paso 2: valida el código y guarda la nueva contraseña con BCrypt. */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        String email = normalizar(request.email());

        if (request.nuevaPassword() == null || request.nuevaPassword().length() < 6) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                    "PASSWORD_NO_VALIDA",
                    "La nueva contraseña debe tener al menos 6 caracteres."
                ));
        }

        CodigoVigente vigente = codigos.get(email);
        if (vigente == null || request.codigo() == null ||
            !request.codigo().equals(vigente.codigo()) ||
            Instant.now().isAfter(vigente.expira())) {
            codigos.remove(email);
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                    "CODIGO_INVALIDO",
                    "El código es incorrecto o ha expirado."
                ));
        }

        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                    "USUARIO_NO_ENCONTRADO",
                    "No existe una cuenta con ese correo."
                ));
        }

        usuario.setPassword(passwordEncoder.encode(request.nuevaPassword()));
        usuarioRepository.save(usuario);
        codigos.remove(email);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Contraseña restablecida");
        return ResponseEntity.ok(respuesta);
    }

    /** Cambio directo: exige la contraseña actual. */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        String email = normalizar(request.email());
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        if (usuario == null || !coincidePassword(request.passwordActual(), usuario.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                    "CREDENCIALES_INVALIDAS",
                    "El correo o la contraseña actual son incorrectos."
                ));
        }

        if (request.nuevaPassword() == null || request.nuevaPassword().length() < 6 ||
            request.nuevaPassword().equals(request.passwordActual())) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                    "PASSWORD_NO_VALIDA",
                    "La nueva contraseña debe tener al menos 6 caracteres y ser distinta de la actual."
                ));
        }

        usuario.setPassword(passwordEncoder.encode(request.nuevaPassword()));
        usuarioRepository.save(usuario);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Contraseña actualizada");
        return ResponseEntity.ok(respuesta);
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**Mis misma respuesta para login y registro (LoginResponseDto). */
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
