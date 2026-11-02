package service;

import enums.EstadoPedido;
import entity.Pedido;
import exceptions.MesaNotFoundException;
import exceptions.MesaOcupadaException;
import exceptions.PedidoNoEditableException;
import repository.PedidoRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

public class PedidoService {

    // Utiliza la definición centralizada de estados de cierre
    private static final Set<EstadoPedido> ESTADOS_YA_CERRADOS = Set.of(
            EstadoPedido.PAGADO,
            EstadoPedido.COMPLETADO,
            EstadoPedido.ENTREGADO,
            EstadoPedido.CANCELADO
    );

    private final PedidoRepository pedidoRepository = new PedidoRepository();
    private final MesaService mesaService = new MesaService();
    private final InventarioService inventarioService = new InventarioService();

    public Pedido crearPedido(long mesaId, long usuarioId) throws SQLException {
        boolean tieneComandaActiva = pedidoRepository.existsByMesaIdAndEstadoNot(mesaId, EstadoPedido.CANCELADO);
        if (tieneComandaActiva) {
            throw new MesaOcupadaException("La mesa " + mesaId + " ya tiene una comanda activa");
        }
        Pedido pedido = new Pedido();
        pedido.setMesaId(mesaId);
        pedido.setUsuarioId(usuarioId);
        pedido.setNumeroPedido("PED-" + System.currentTimeMillis());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        Pedido guardado = pedidoRepository.save(pedido);
        mesaService.cambiarEstado(mesaId, "OCUPADA", "APERTURA_PEDIDO");
        return guardado;
    }

    public Optional<Pedido> obtenerPedidoActivo(long mesaId) throws SQLException {
        return pedidoRepository.findActivoByMesaId(mesaId);
    }

    public Pedido obtenerOCrearPedido(long mesaId, long usuarioId) throws SQLException {
        Optional<Pedido> existente = obtenerPedidoActivo(mesaId);
        if (existente.isPresent()) {
            return existente.get();
        }
        return crearPedido(mesaId, usuarioId);
    }

    public Pedido actualizarPedido(Pedido pedido) throws SQLException {
        // Blindaje: Verificar si el pedido ya está cerrado en la BD antes de permitir modificarlo
        if (pedido.getId() != null) {
            Pedido actual = pedidoRepository.findById(pedido.getId()).orElse(null);
            if (actual != null && ESTADOS_YA_CERRADOS.contains(actual.getEstado())) {
                throw new PedidoNoEditableException(
                        "El pedido " + actual.getNumeroPedido() + " ya se encuentra " + actual.getEstado() +
                                " y no admite modificaciones."
                );
            }
        }
        return pedidoRepository.save(pedido);
    }

    public void actualizarImportes(long pedidoId, String productosJson,
                                   BigDecimal subtotal, BigDecimal impuestos) throws SQLException {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new MesaNotFoundException("Pedido no encontrado con id " + pedidoId));

        if (ESTADOS_YA_CERRADOS.contains(pedido.getEstado())) {
            throw new PedidoNoEditableException("No se pueden actualizar importes: el pedido ya está " + pedido.getEstado());
        }

        pedidoRepository.actualizarImportes(pedidoId, productosJson, subtotal, impuestos);
    }

    public void marcarInventarioDescontado(long pedidoId) throws SQLException {
        pedidoRepository.marcarInventarioDescontado(pedidoId);
    }

    public Pedido cerrarPedido(long pedidoId, String codigoEstadoDestinoMesa) throws SQLException {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new MesaNotFoundException(
                        "Pedido no encontrado con id " + pedidoId
                ));

        // Si ya está cerrado, completado, entregado o pagado, no se puede volver a cerrar
        if (ESTADOS_YA_CERRADOS.contains(pedido.getEstado())) {
            throw new PedidoNoEditableException(
                    "El pedido " + pedido.getNumeroPedido()
                            + " ya está en estado " + pedido.getEstado()
            );
        }

        String destino = codigoEstadoDestinoMesa != null
                ? codigoEstadoDestinoMesa
                : "DISPONIBLE";

        mesaService.cambiarEstado(
                pedido.getMesaId(),
                destino,
                "CIERRE_PEDIDO"
        );

        // Se marca el pedido como entregado (o cerrado definitivo)
        pedidoRepository.actualizarEstado(
                pedidoId,
                EstadoPedido.ENTREGADO
        );
        pedido.setEstado(EstadoPedido.ENTREGADO);
        return pedido;
    }

    /**
     * Anula una comanda de forma irreversible.
     * Si el inventario ya había sido descontado, devuelve el stock.
     */
    public Pedido cancelarPedido(long pedidoId, String codigoEstadoDestinoMesa) throws SQLException {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new MesaNotFoundException("Pedido no encontrado con id " + pedidoId));

        // Bloqueo total: Una vez cancelado o en estado de cierre, la cancelación es irreversible y definitiva
        if (ESTADOS_YA_CERRADOS.contains(pedido.getEstado())) {
            throw new PedidoNoEditableException("El pedido "
                    + pedido.getNumeroPedido()
                    + " ya se encuentra cerrado (" + pedido.getEstado() + ") y no puede modificarse ni reabrirse."
            );
        }

        // Si ya se descontó inventario, devolvemos los ingredientes al stock
        if (pedido.isInventarioDescontado()) {
            inventarioService.revertirDescuento(pedidoId, pedido.getUsuarioId());
        }

        // Cambiamos el estado a CANCELADO (lo cual lo ingresa de inmediato en ESTADOS_YA_CERRADOS de forma permanente)
        pedido.setEstado(EstadoPedido.CANCELADO);
        pedidoRepository.actualizarEstado(pedidoId, EstadoPedido.CANCELADO);

        // Liberamos la mesa
        String destino = codigoEstadoDestinoMesa != null ? codigoEstadoDestinoMesa : "DISPONIBLE";
        mesaService.cambiarEstado(pedido.getMesaId(), destino, "CANCELACION_PEDIDO");

        return pedido;
    }
}