package com.restaurante.repository;

import com.restaurante.entity.PlatoIngrediente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlatoIngredienteRepository
        extends JpaRepository<PlatoIngrediente, Long> {

    List<PlatoIngrediente> findByPlatoId(Long platoId);
}