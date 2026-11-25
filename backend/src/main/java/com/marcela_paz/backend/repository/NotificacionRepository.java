package com.marcela_paz.backend.repository;

import com.marcela_paz.backend.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, String> {

    List<Notificacion> findByUsuarioIdOrderByFechaEnvioDesc(String usuarioId);

    // @Modifying + @Transactional son obligatorios en un UPDATE con @Query:
    // sin ellos Hibernate lanza InvalidDataAccessApiUsageException en runtime.
    @Modifying
    @Transactional
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.id = :id")
    int marcarComoLeida(@Param("id") String id);

    Long countByUsuarioIdAndLeidaFalse(String usuarioId);
}
