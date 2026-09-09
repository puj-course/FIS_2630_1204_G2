package com.restaurante.repository;

import com.restaurante.entity.AlertaInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertaInventarioRepository
        extends JpaRepository<AlertaInventario, Long> {

    // Obtener todas las alertas que todavía están activas
    List<AlertaInventario> findByIsResolvedFalse();

    // Buscar si un ingrediente ya tiene una alerta activa
    Optional<AlertaInventario> findByIngredienteIdAndIsResolvedFalse(
            Long ingredienteId
    );

    // Contar la cantidad de alertas activas
    long countByIsResolvedFalse();
}