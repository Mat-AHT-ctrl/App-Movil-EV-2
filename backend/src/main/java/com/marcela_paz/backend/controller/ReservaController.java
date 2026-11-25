package com.marcela_paz.backend.controller;

import com.marcela_paz.backend.dto.EstadoReservaRequest;
import com.marcela_paz.backend.dto.ReservaRequest;
import com.marcela_paz.backend.model.Reserva;
import com.marcela_paz.backend.model.Usuario;
import com.marcela_paz.backend.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @PostMapping
    public ResponseEntity<Reserva> crear(@RequestBody ReservaRequest request) {
        Reserva reserva = Reserva.builder()
            .espacioId(request.getEspacioId())
            .espacioNombre(request.getEspacioNombre())
            .usuarioId(request.getUsuarioId())
            .solicitanteNombre(request.getSolicitanteNombre())
            .perfilSolicitante(Usuario.Perfil.valueOf(request.getPerfilSolicitante()))
            .fecha(request.getFecha())
            .horaInicio(request.getHoraInicio())
            .horaTermino(request.getHoraTermino())
            .actividad(request.getActividad())
            .cursoGrupo(request.getCursoGrupo())
            .asistentes(request.getAsistentes())
            .observaciones(request.getObservaciones())
            .build();

        Reserva creada = reservaService.crearReserva(reserva);
        return ResponseEntity.ok(creada);
    }

    @GetMapping("/pendientes")
    public List<Reserva> pendientes() {
        return reservaService.listarPendientes();
    }

    @GetMapping
    public List<Reserva> todas() {
        return reservaService.listarTodas();
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Reserva> cambiarEstado(
        @PathVariable String id,
        @RequestBody EstadoReservaRequest request) {

        Reserva reserva;
        switch (request.getEstado()) {
            case "APROBADA":
                reserva = reservaService.aprobarReserva(id, request.getResueltoPor());
                break;
            case "RECHAZADA":
                reserva = reservaService.rechazarReserva(id, request.getJustificacion(), request.getResueltoPor());
                break;
            case "CANCELADA":
                reserva = reservaService.cancelarReserva(id, request.getJustificacion());
                break;
            default:
                throw new IllegalArgumentException("ESTADO_NO_VALIDO");
        }
        return ResponseEntity.ok(reserva);
    }

    @PostMapping("/finalizar-vencidas")
    public ResponseEntity<Map<String, String>> finalizarVencidas() {
        reservaService.finalizarReservasVencidas();
        Map<String, String> resp = new java.util.HashMap<>();
        resp.put("mensaje", "Reservas vencidas finalizadas");
        return ResponseEntity.ok(resp);
    }
}