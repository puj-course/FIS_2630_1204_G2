package com.restaurante.service;

import com.restaurante.entity.DetallePedido;
import com.restaurante.entity.EstadoPedido;
import com.restaurante.entity.Pedido;
import com.restaurante.entity.Plato;
import com.restaurante.repository.DetallePedidoRepository;
import com.restaurante.repository.PedidoRepository;
import com.restaurante.repository.PlatoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final PlatoRepository platoRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            DetallePedidoRepository detallePedidoRepository,
            PlatoRepository platoRepository
    ) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.platoRepository = platoRepository;
    }

    // Crear un pedido vacío en estado PENDIENTE
    @Transactional
    public Pedido crearPedido() {

        Pedido pedido = new Pedido();

        return pedidoRepository.save(pedido);
    }

    // Agregar un plato al pedido
    @Transactional
    public DetallePedido agregarPlato(
            Long pedidoId,
            Long platoId,
            BigDecimal cantidad
    ) {

        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "La cantidad debe ser mayor que cero"
            );
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new RuntimeException("Pedido no encontrado")
                );

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new RuntimeException(
                    "Solo se pueden modificar pedidos PENDIENTES"
            );
        }

        Plato plato = platoRepository.findById(platoId)
                .orElseThrow(() ->
                        new RuntimeException("Plato no encontrado")
                );

        if (!Boolean.TRUE.equals(plato.getIsActive())) {
            throw new RuntimeException(
                    "El plato no está activo"
            );
        }

        List<DetallePedido> detalles =
                detallePedidoRepository.findByPedidoId(pedidoId);

        // Si el plato ya está en el pedido, aumenta la cantidad
        for (DetallePedido detalle : detalles) {

            if (detalle.getPlato().getId().equals(platoId)) {

                detalle.setCantidad(
                        detalle.getCantidad().add(cantidad)
                );

                return detallePedidoRepository.save(detalle);
            }
        }

        // Si no existe, crea un nuevo detalle
        DetallePedido detalle = new DetallePedido();

        detalle.setPedido(pedido);
        detalle.setPlato(plato);
        detalle.setCantidad(cantidad);

        return detallePedidoRepository.save(detalle);
    }

    // Cambiar directamente la cantidad de un plato
    @Transactional
    public DetallePedido cambiarCantidad(
            Long pedidoId,
            Long platoId,
            BigDecimal nuevaCantidad
    ) {

        if (nuevaCantidad == null ||
                nuevaCantidad.compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "La cantidad debe ser mayor que cero"
            );
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new RuntimeException("Pedido no encontrado")
                );

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new RuntimeException(
                    "Solo se pueden modificar pedidos PENDIENTES"
            );
        }

        List<DetallePedido> detalles =
                detallePedidoRepository.findByPedidoId(pedidoId);

        for (DetallePedido detalle : detalles) {

            if (detalle.getPlato().getId().equals(platoId)) {

                detalle.setCantidad(nuevaCantidad);

                return detallePedidoRepository.save(detalle);
            }
        }

        throw new RuntimeException(
                "El plato no pertenece a este pedido"
        );
    }

    // Eliminar un plato del pedido
    @Transactional
    public void eliminarPlato(
            Long pedidoId,
            Long platoId
    ) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new RuntimeException("Pedido no encontrado")
                );

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new RuntimeException(
                    "Solo se pueden modificar pedidos PENDIENTES"
            );
        }

        List<DetallePedido> detalles =
                detallePedidoRepository.findByPedidoId(pedidoId);

        for (DetallePedido detalle : detalles) {

            if (detalle.getPlato().getId().equals(platoId)) {

                detallePedidoRepository.delete(detalle);
                return;
            }
        }

        throw new RuntimeException(
                "El plato no pertenece a este pedido"
        );
    }

    // Consultar los detalles actuales del pedido
    @Transactional(readOnly = true)
    public List<DetallePedido> consultarDetalles(Long pedidoId) {

        if (!pedidoRepository.existsById(pedidoId)) {
            throw new RuntimeException(
                    "Pedido no encontrado"
            );
        }

        return detallePedidoRepository.findByPedidoId(pedidoId);
    }
}