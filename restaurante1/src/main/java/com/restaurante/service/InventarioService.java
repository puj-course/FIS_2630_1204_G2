package com.restaurante.service;

import com.restaurante.entity.*;
import com.restaurante.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.restaurante.dto.IngredienteStockDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;

@Service
public class InventarioService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final PlatoIngredienteRepository platoIngredienteRepository;
    private final IngredienteRepository ingredienteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final AlertaInventarioService alertaInventarioService;

    public InventarioService(
            PedidoRepository pedidoRepository,
            DetallePedidoRepository detallePedidoRepository,
            PlatoIngredienteRepository platoIngredienteRepository,
            IngredienteRepository ingredienteRepository,
            MovimientoInventarioRepository movimientoRepository,
            AlertaInventarioService alertaInventarioService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.platoIngredienteRepository = platoIngredienteRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.movimientoRepository = movimientoRepository;
        this.alertaInventarioService = alertaInventarioService;
    }

    @Transactional
    public void procesarPedido(Long pedidoId) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new RuntimeException("Pedido no encontrado")
                );
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new RuntimeException(
                    "El pedido ya fue procesado. Estado actual: "
                            + pedido.getEstado()
            );
        }

        List<DetallePedido> detalles =
                detallePedidoRepository.findByPedidoId(pedidoId);

        if (detalles.isEmpty()) {
            throw new RuntimeException(
                    "El pedido no tiene productos"
            );
        }

        for (DetallePedido detalle : detalles) {

            Plato plato = detalle.getPlato();

            BigDecimal cantidadPlatos =
                    detalle.getCantidad();

            List<PlatoIngrediente> receta =
                    platoIngredienteRepository
                            .findByPlatoId(plato.getId());

            if (receta.isEmpty()) {
                throw new RuntimeException(
                        "El plato " + plato.getNombre()
                                + " no tiene receta configurada"
                );
            }

            for (PlatoIngrediente item : receta) {

                BigDecimal cantidadNecesaria =
                        item.getCantidadReceta()
                                .multiply(cantidadPlatos);

                Ingrediente ingrediente =
                        ingredienteRepository.findById(item.getIngrediente().getId())
                                .orElseThrow(() ->
                                        new RuntimeException(
                                                "Ingrediente no encontrado"
                                        )
                                );

                BigDecimal stockActual =
                        ingrediente.getStockActual();

                if (stockActual.compareTo(cantidadNecesaria) < 0) {

                    throw new RuntimeException(
                            "Stock insuficiente para el ingrediente: "
                                    + ingrediente.getNombre()
                    );
                }

                ingrediente.setStockActual(
                        stockActual.subtract(cantidadNecesaria)
                );

                ingredienteRepository.save(ingrediente);

                MovimientoInventario movimiento =
                        new MovimientoInventario();

                movimiento.setIngrediente(ingrediente);
                movimiento.setPedido(pedido);
                movimiento.setCantidad(cantidadNecesaria);
                movimiento.setTipoMovimiento(
                        TipoMovimiento.VENTA_COMANDA
                );
                movimiento.setCreatedBy("SISTEMA");

                movimientoRepository.save(movimiento);

                alertaInventarioService
                        .evaluarInventario(ingrediente);
            }
        }

        pedido.setEstado(EstadoPedido.CONFIRMADO);

        pedidoRepository.save(pedido);
    }
    @Transactional
    public void cancelarPedido(Long pedidoId) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new RuntimeException("Pedido no encontrado")
                );

        // El pedido debe estar confirmado para poder revertir
        if (pedido.getEstado() != EstadoPedido.CONFIRMADO) {
            throw new RuntimeException(
                    "El pedido no puede cancelarse. Estado actual: "
                            + pedido.getEstado()
            );
        }

        // Buscar todos los movimientos generados por este pedido
        List<MovimientoInventario> movimientos =
                movimientoRepository.findByPedidoId(pedidoId);

        if (movimientos.isEmpty()) {
            throw new RuntimeException(
                    "El pedido no tiene movimientos de inventario"
            );
        }

        // Devolver al inventario cada cantidad descontada
        for (MovimientoInventario movimiento : movimientos) {

            Ingrediente ingrediente =
                    ingredienteRepository
                            .findById(
                                    movimiento.getIngrediente().getId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Ingrediente no encontrado"
                                    )
                            );

            BigDecimal stockActual =
                    ingrediente.getStockActual();

            BigDecimal cantidadDevuelta =
                    movimiento.getCantidad();

            ingrediente.setStockActual(
                    stockActual.add(cantidadDevuelta)
            );

            ingredienteRepository.save(ingrediente);
        }

        // Finalmente cancelar el pedido
        pedido.setEstado(EstadoPedido.CANCELADO);

        pedidoRepository.save(pedido);
    }
    @Transactional(readOnly = true)
    public Page<IngredienteStockDTO> consultarStock(String nombre, Pageable pageable) {

        Page<Ingrediente> ingredientes;

        if (nombre != null && !nombre.isBlank()) {

            ingredientes =
                    ingredienteRepository
                            .findByIsActiveTrueAndNombreContainingIgnoreCase(
                                    nombre,
                                    pageable
                            );

        } else {

            ingredientes =
                    ingredienteRepository
                            .findByIsActiveTrue(pageable);
        }

        return ingredientes.map(ingrediente -> {

            BigDecimal stockActual =
                    ingrediente.getStockActual();

            BigDecimal stockMinimo =
                    ingrediente.getStockMinimo();

            String estado;

            if (stockActual.compareTo(BigDecimal.ZERO) == 0) {

                estado = "AGOTADO";

            } else if (stockActual.compareTo(stockMinimo) <= 0) {

                estado = "STOCK_BAJO";

            } else {

                estado = "NORMAL";
            }

            return new IngredienteStockDTO(
                    ingrediente.getId(),
                    ingrediente.getNombre(),
                    stockActual,
                    stockMinimo,
                    ingrediente.getIsActive(),
                    estado
            );
        });
    }
}