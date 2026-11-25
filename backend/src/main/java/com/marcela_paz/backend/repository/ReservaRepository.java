package com.marcela_paz.backend.repository;

import com.marcela_paz.backend.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, String> {

    List<Reserva> findByUsuarioId(String usuarioId);

    List<Reserva> findByEstado(Reserva.EstadoEstado estado);

    List<Reserva> findByEspacioIdAndFecha(String espacioId, String fecha);

    Optional<Reserva> findById(String id);

    @Query("""
        SELECT r FROM Reserva r
        WHERE r.espacioId = :espacioId
          AND r.fecha = :fecha
          AND r.estado IN ('PENDIENTE', 'APROBADA')
          AND r.horaInicio < :horaTermino
          AND r.horaTermino > :horaInicio
    """)
    List<Reserva> findOcupadasPorSolapamiento(
        @Param("espacioId") String espacioId,
        @Param("fecha") String fecha,
        @Param("horaInicio") String horaInicio,
        @Param("horaTermino") String horaTermino
    );
}