package service;

import enums.EstadoPedido;
import entity.Pedido;
import exceptions.MesaNotFoundException;
import exceptions.MesaOcupadaException;
import exceptions.PedidoNoEditableException;
import repository.PedidoRepository;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

public class PedidoService {

    private static final Set<EstadoPedido> ESTADOS_YA_CERRADOS = Set.of(
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
        return pedidoRepository.save(pedido);
    }
    public Pedido cerrarPedido(long pedidoId, String codigoEstadoDestinoMesa) throws SQLException {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(() -> new MesaNotFoundException("Pedido no encontrado con id " + pedidoId));
        if (ESTADOS_YA_CERRADOS.contains(pedido.getEstado())) {
            throw new PedidoNoEditableException("El pedido "
                            + pedido.getNumeroPedido()
                            + " ya está en estado "
                            + pedido.getEstado());
        }
        pedido.setEstado(EstadoPedido.COMPLETADO);
        Pedido cerrado = pedidoRepository.save(pedido);
        String destino = codigoEstadoDestinoMesa != null ? codigoEstadoDestinoMesa : "LIBRE";
        mesaService.cambiarEstado(pedido.getMesaId(), destino, "CIERRE_PEDIDO");
        return cerrado;
    }
    /**
     * Anula una comanda.
     *
     * Si el inventario ya había sido descontado,
     * se devuelve el stock de los ingredientes y
     * se registra el movimiento correspondiente
     * como ENTRADA.
     */
    public Pedido cancelarPedido(long pedidoId, String codigoEstadoDestinoMesa) throws SQLException {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(() -> new MesaNotFoundException("Pedido no encontrado con id " + pedidoId));
        /*
         * No se puede cancelar nuevamente
         * una comanda que ya terminó.
         */
        if (ESTADOS_YA_CERRADOS.contains(pedido.getEstado())) {
            throw new PedidoNoEditableException("El pedido "
                            + pedido.getNumeroPedido()
                            + " ya está en estado "
                            + pedido.getEstado()
            );
        }
        /*
         * Si ya se descontó inventario, primero
         * devolvemos los ingredientes al stock.
         *
         * InventarioService se encarga de verificar
         * que la reversión no se haga dos veces.
         */
        if (pedido.isInventarioDescontado()) {
            inventarioService.revertirDescuento(pedidoId, pedido.getUsuarioId());
        }
        /*
         * Finalmente cambiamos el estado de la comanda.
         */
        pedido.setEstado(EstadoPedido.CANCELADO);
        Pedido cancelado = pedidoRepository.save(pedido);
        /*
         * Liberamos la mesa.
         */
        String destino = codigoEstadoDestinoMesa != null ? codigoEstadoDestinoMesa : "DISPONIBLE";
        mesaService.cambiarEstado(pedido.getMesaId(), destino, "CANCELACION_PEDIDO");
        return cancelado;
    }
}