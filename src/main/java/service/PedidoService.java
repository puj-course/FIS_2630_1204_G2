package service;

import entity.PedidoHistorial;
import enums.EstadoPedido;
import entity.Pedido;
import entity.DetallePedido;
import exceptions.MesaNotFoundException;
import exceptions.MesaOcupadaException;
import exceptions.PedidoNoEditableException;
import repository.PedidoHistorialRepository;
import repository.PedidoRepository;
import repository.DetallePedidoRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class PedidoService {

    private static final Set<EstadoPedido> ESTADOS_YA_CERRADOS = Set.of(
            EstadoPedido.COMPLETADO,
            EstadoPedido.ENTREGADO,
            EstadoPedido.CANCELADO
    );

    private final PedidoRepository pedidoRepository = new PedidoRepository();
    private final DetallePedidoRepository detallePedidoRepository = new DetallePedidoRepository();
    private final PedidoHistorialRepository eventoRepository = new PedidoHistorialRepository();
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
        registrarEvento(guardado.getId(), "APERTURA_PEDIDO", "Se creó el pedido con número " + guardado.getNumeroPedido(), usuarioId);
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
        if (ESTADOS_YA_CERRADOS.contains(pedido.getEstado())) {
            throw new PedidoNoEditableException("El pedido " + pedido.getNumeroPedido() + " ya no es editable.");
        }
        return pedidoRepository.save(pedido);
    }

    /**
     * Guarda el pedido, registra sus ítems en la tabla detalle_pedido
     * y ejecuta el descuento de ingredientes en el inventario.
     */
    public Pedido actualizarPedidoConDetalles(Pedido pedido, List<DetallePedido> detalles) throws SQLException {
        if (ESTADOS_YA_CERRADOS.contains(pedido.getEstado())) {
            throw new PedidoNoEditableException("El pedido " + pedido.getNumeroPedido() + " ya no es editable.");
        }

        Pedido pedidoActualizado = pedidoRepository.save(pedido);
        if (detalles != null && !detalles.isEmpty()) {
            for (DetallePedido detalle : detalles) {
                detalle.setPedidoId(pedidoActualizado.getId());
            }
            detallePedidoRepository.guardarDetalles(detalles);

            Map<Long, Integer> mapaProductos = new HashMap<>();
            for (DetallePedido detalle : detalles) {
                mapaProductos.put(detalle.getProductoId(), detalle.getCantidad());
            }

            inventarioService.descontarIngredientes(
                    pedidoActualizado.getId(),
                    mapaProductos,
                    pedidoActualizado.getUsuarioId()
            );

            pedidoActualizado.setInventarioDescontado(true);
            pedidoRepository.save(pedidoActualizado);
            registrarEvento(pedidoActualizado.getId(), "ACTUALIZACION_DETALLES", "Se agregaron ítems y se descontó inventario al pedido.", pedidoActualizado.getUsuarioId());
        }
        return pedidoActualizado;
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
        registrarEvento(cerrado.getId(), "CIERRE_PEDIDO", "El pedido fue completado y cerrado formalmente.", cerrado.getUsuarioId());
        return cerrado;
    }
    /**
     * Anula una comanda.
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
         * InventarioService se encarga de verificar
         * que la reversión no se haga dos veces.
         */
        if (pedido.isInventarioDescontado()) {
            inventarioService.revertirDescuento(pedidoId, pedido.getUsuarioId());
        }
        pedido.setEstado(EstadoPedido.CANCELADO);
        Pedido cancelado = pedidoRepository.save(pedido);

        String destino = codigoEstadoDestinoMesa != null ? codigoEstadoDestinoMesa : "DISPONIBLE";
        mesaService.cambiarEstado(pedido.getMesaId(), destino, "CANCELACION_PEDIDO");
        registrarEvento(cancelado.getId(), "CANCELACION_PEDIDO", "El pedido fue anulado y se devolvió el stock al inventario.", cancelado.getUsuarioId());
        return cancelado;
    }

    // Método auxiliar privado para encapsular el guardado de eventos
    private void registrarEvento(long pedidoId, String tipoEvento, String descripcion, Long usuarioId) {
        try {
            PedidoHistorial evento = new PedidoHistorial();
            evento.setPedidoId(pedidoId);
            evento.setTipoEvento(tipoEvento);
            evento.setDescripcion(descripcion);
            evento.setUsuarioId(usuarioId);
            evento.setFechaEvento(LocalDateTime.now());

            eventoRepository.registrarEvento(evento);
        } catch (SQLException e) {
            // Imprime o maneja el error sin interrumpir el flujo principal de negocio si así se prefiere
            e.printStackTrace();
        }
    }
}
