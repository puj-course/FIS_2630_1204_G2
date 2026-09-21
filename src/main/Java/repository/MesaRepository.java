package com.restaurante.repository;

import com.restaurante.entity.EstadoMesa;
import com.restaurante.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    List<Mesa> findByZonaId(Long zonaId);

    List<Mesa> findByEstado(EstadoMesa estado);

    List<Mesa> findByZonaIdAndEstado(Long zonaId, EstadoMesa estado);
}
