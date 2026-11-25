package com.marcela_paz.backend.repository;

import com.marcela_paz.backend.model.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EspacioRepository extends JpaRepository<Espacio, String> {
    List<Espacio> findByEstado(Espacio.EstadoEstado estado);
}