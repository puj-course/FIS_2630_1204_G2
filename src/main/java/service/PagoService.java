package com.restaurante.service;

import com.restaurante.entity.Pago;
import com.restaurante.entity.Pedido;
import com.restaurante.enums.MetodoPago;
import com.restaurante.enums.EstadoPedido;
import com.restaurante.repository.PagoRepository;
import com.restaurante.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;

    public PagoService(
            PagoRepository pagoRepository,
            PedidoRepository pedidoRepository
    ) {
        this.pagoRepository = pagoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public Pago registrarPago(
            Long pedidoId,
            MetodoPago metodo,
            BigDecimal monto
    ) {

        // 1. Buscar el pedido
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Pedido no encontrado con ID: "
                                        + pedidoId
                        )
                );

        // 2. Validar método
        if (metodo == null) {
            throw new RuntimeException(
                    "Debe seleccionar un metodo de pago"
            );
        }

        // 3. Validar monto
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "El monto del pago debe ser mayor que cero"
            );
        }

        // 4. No permitir pagos a pedidos cancelados
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new RuntimeException(
                    "No se puede registrar un pago para un pedido cancelado"
            );
        }

        // 5. No permitir pagos después de completar el pago
        if (Boolean.TRUE.equals(pedido.getPagado())) {
            throw new RuntimeException(
                    "El pedido ya se encuentra completamente pagado"
            );
        }

        // 6. Obtener pagos existentes
        List<Pago> pagos =
                pagoRepository.findByPedidoId(pedidoId);

        // 7. Calcular cuánto se ha pagado
        BigDecimal totalPagado = pagos.stream()
                .map(Pago::getMonto)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        // 8. Calcular cuánto quedaría después de este pago
        BigDecimal nuevoTotalPagado =
                totalPagado.add(monto);

        // 9. No permitir superar el total
        if (nuevoTotalPagado.compareTo(pedido.getTotal()) > 0) {
            throw new RuntimeException(
                    "El pago supera el total del pedido. "
                            + "Total: " + pedido.getTotal()
                            + ", Pagado: " + totalPagado
                            + ", Intento de pago: " + monto
            );
        }

        // 10. Crear pago
        Pago pago = new Pago();

        pago.setPedido(pedido);
        pago.setMetodo(metodo);
        pago.setMonto(monto);

        Pago pagoGuardado =
                pagoRepository.save(pago);

        // 11. Si ya se pagó todo, marcar pedido como pagado
        if (nuevoTotalPagado.compareTo(pedido.getTotal()) == 0) {

            pedido.setPagado(true);

            pedidoRepository.save(pedido);
        }

        return pagoGuardado;
    }

    @Transactional(readOnly = true)
    public List<Pago> consultarPagos(Long pedidoId) {

        if (!pedidoRepository.existsById(pedidoId)) {
            throw new RuntimeException(
                    "Pedido no encontrado con ID: "
                            + pedidoId
            );
        }

        return pagoRepository.findByPedidoId(pedidoId);
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerTotalPagado(Long pedidoId) {

        if (!pedidoRepository.existsById(pedidoId)) {
            throw new RuntimeException(
                    "Pedido no encontrado con ID: "
                            + pedidoId
            );
        }

        List<Pago> pagos =
                pagoRepository.findByPedidoId(pedidoId);

        return pagos.stream()
                .map(Pago::getMonto)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerSaldoPendiente(Long pedidoId) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Pedido no encontrado con ID: "
                                        + pedidoId
                        )
                );

        BigDecimal totalPagado =
                obtenerTotalPagado(pedidoId);

        return pedido.getTotal()
                .subtract(totalPagado);
    }
}