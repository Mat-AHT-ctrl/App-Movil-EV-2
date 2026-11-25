package com.marcela_paz.backend.repository;

import com.marcela_paz.backend.model.Bloqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BloqueoRepository extends JpaRepository<Bloqueo, String> {

    List<Bloqueo> findByEspacioIdAndFecha(String espacioId, String fecha);

    @Query("""
        SELECT b FROM Bloqueo b
        WHERE b.espacioId = :espacioId
          AND b.fecha = :fecha
          AND b.horaInicio < :horaTermino
          AND b.horaTermino > :horaInicio
    """)
    List<Bloqueo> findBloqueosPorSolapamiento(
        @Param("espacioId") String espacioId,
        @Param("fecha") String fecha,
        @Param("horaInicio") String horaInicio,
        @Param("horaTermino") String horaTermino
    );
}