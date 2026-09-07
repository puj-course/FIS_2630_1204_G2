package com.restaurante.repository;

import com.restaurante.entity.Ingrediente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.Optional;

public interface IngredienteRepository
        extends JpaRepository<Ingrediente, Long> {
    Page<Ingrediente> findByIsActiveTrue(Pageable pageable);

    Page<Ingrediente> findByIsActiveTrueAndNombreContainingIgnoreCase(
            String nombre,
            Pageable pageable
    );
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Ingrediente> findById(Long id);
}