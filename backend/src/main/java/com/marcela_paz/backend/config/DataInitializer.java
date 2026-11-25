package com.marcela_paz.backend.config;

import com.marcela_paz.backend.model.Espacio;
import com.marcela_paz.backend.model.Usuario;
import com.marcela_paz.backend.repository.EspacioRepository;
import com.marcela_paz.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Carga inicial de datos.
 *
 * Es idempotente: si la base ya tiene espacios, no hace nada.
 * Asi funciona igual con H2 local y con Neon en produccion,
 * sin borrar informacion en cada reinicio.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final EspacioRepository espacioRepository;
    private final UsuarioRepository usuarioRepository;

    public DataInitializer(EspacioRepository espacioRepository, UsuarioRepository usuarioRepository) {
        this.espacioRepository = espacioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) {
        cargarUsuarios();
        cargarEspacios();
    }

    private void cargarUsuarios() {
        if (usuarioRepository.count() > 0) {
            log.info("Usuarios ya existen ({}), se omite la carga inicial", usuarioRepository.count());
            return;
        }

        usuarioRepository.saveAll(List.of(
            Usuario.builder()
                .nombre("Inspectoria General")
                .email("admin@colegio.cl")
                .password("123456")
                .rol(Usuario.Rol.ADMIN)
                .perfil(Usuario.Perfil.INSPECTORIA)
                .build(),
            Usuario.builder()
                .nombre("Profesor de Aula")
                .email("docente@colegio.cl")
                .password("123456")
                .rol(Usuario.Rol.SOLICITANTE)
                .perfil(Usuario.Perfil.DOCENTE)
                .build()
        ));
        log.info("Carga inicial: 2 usuarios creados");
    }

    private void cargarEspacios() {
        if (espacioRepository.count() > 0) {
            log.info("Espacios ya existen ({}), se omite la carga inicial", espacioRepository.count());
            return;
        }

        espacioRepository.saveAll(List.of(
            espacio("Sala de Capacitacion", "Formacion",
                "Sala equipada para talleres y capacitaciones docentes.",
                "Piso 2 - Edificio Central", 40, "08:15 - 17:30",
                Espacio.EstadoEstado.EN_MANTENIMIENTO),

            espacio("Laboratorio de Computacion", "Laboratorio",
                "30 equipos con internet y proyector.",
                "Piso 2 - Pabellon A", 30, "08:15 - 17:30",
                Espacio.EstadoEstado.DISPONIBLE),

            espacio("Biblioteca CRA", "Biblioteca",
                "Zona de lectura y trabajo grupal.",
                "Piso 1 - Edificio Central", 35, "08:15 - 16:45",
                Espacio.EstadoEstado.DISPONIBLE),

            espacio("Sala de Apoyo PIE", "Apoyo PIE",
                "Espacio de trabajo con estudiantes del programa PIE.",
                "Piso 1 - Pabellon B", 12, "08:15 - 15:30",
                Espacio.EstadoEstado.DISPONIBLE),

            espacio("Laboratorio de Innovacion", "Innovacion",
                "Impresoras 3D, sensores y materiales de robotica.",
                "Piso 3 - Pabellon B", 20, "09:00 - 16:45",
                Espacio.EstadoEstado.DISPONIBLE),

            espacio("Sala de Reuniones", "Reuniones",
                "Sala para reuniones de UTP, direccion y apoderados.",
                "Piso 1 - Direccion", 15, "08:15 - 17:30",
                Espacio.EstadoEstado.DISPONIBLE),

            espacio("Aula de Formacion", "Formacion",
                "Aula flexible para talleres y charlas.",
                "Piso 2 - Pabellon A", 28, "08:15 - 16:45",
                Espacio.EstadoEstado.DISPONIBLE),

            espacio("Patio Recreativo", "Recreativo",
                "Patio techado para actividades deportivas y recreativas.",
                "Patio Central", 60, "08:15 - 17:30",
                Espacio.EstadoEstado.DISPONIBLE)
        ));
        log.info("Carga inicial: 8 espacios creados");
    }

    private Espacio espacio(String nombre, String tipo, String descripcion, String ubicacion,
                            int capacidad, String horario, Espacio.EstadoEstado estado) {
        return Espacio.builder()
            .nombre(nombre)
            .tipo(tipo)
            .descripcion(descripcion)
            .ubicacion(ubicacion)
            .capacidad(capacidad)
            .horarioFuncionamiento(horario)
            .estado(estado)
            .build();
    }
}
