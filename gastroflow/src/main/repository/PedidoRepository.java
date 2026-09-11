package com.restaurante.repository;

import com.restaurante.entity.EstadoPedido;
import com.restaurante.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    boolean existsByMesaIdAndEstadoNot(Long mesaId, EstadoPedido estado);
}
