package com.restaurante.repository;

import com.restaurante.entity.EstadoMesa;
import com.restaurante.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    List<Mesa> findByEstado(EstadoMesa estado);

    boolean existsByNumero(Integer numero);
}