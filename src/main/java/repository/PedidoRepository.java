package com.restaurante.repository;

import com.restaurante.entity.Pedido;
import com.restaurante.entity.Mesa;
import com.restaurante.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByMesaAndEstado(
            Mesa mesa,
            EstadoPedido estado
    );
}