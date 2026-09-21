package com.restaurante.repository;

import com.restaurante.entity.DetallePedidoAdicional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetallePedidoAdicionalRepository
        extends JpaRepository<DetallePedidoAdicional, Long> {

    List<DetallePedidoAdicional> findByDetallePedidoId(Long detallePedidoId);
}
