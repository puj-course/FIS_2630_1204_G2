package com.restaurante.repository;

import com.restaurante.entity.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInventarioRepository
        extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByPedidoId(Long pedidoId);

    List<MovimientoInventario> findByIngredienteId(Long ingredienteId);
}