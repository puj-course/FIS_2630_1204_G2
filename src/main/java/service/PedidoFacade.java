package service;

import dao.DisponibilidadDAO;
import dto.ItemPedido;
import entity.DetallePedido;
import entity.Pedido;
import ConexionBD.ConexionBD;
import enums.EstadoPedido;
import repository.PedidoRepository;
import repository.DetallePedidoAdicionalRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Patrón Facade: una interfaz simple para un subsistema complicado.
 *
 * Confirmar un pedido obliga a coordinar varias piezas que el controller
 * no debería conocer:
 *   - DisponibilidadDAO   (¿alcanzan los ingredientes del pedido completo?)
 *   - PedidoService       (buscar o crear el pedido y guardarlo)
 *   - InventarioService   (descontar los ingredientes)
 * y además deshacer lo creado si algo falla a mitad de camino.
 *
 * El controller solo habla con esta clase: validarDisponibilidad() y
 * confirmarPedido(). El orden de los pasos y el manejo de errores viven aquí.
 */
public class PedidoFacade {

    private static final BigDecimal TASA_IVA = new BigDecimal("0.19");
    private final PedidoService pedidoService;
    private final InventarioService inventarioService;
    private final DisponibilidadDAO disponibilidadDAO;
    private final PedidoRepository pedidoRepository = new PedidoRepository();


    public PedidoFacade() {
        this(new PedidoService(), new InventarioService(), new DisponibilidadDAO());
    }

    public PedidoFacade(PedidoService pedidoService,
                        InventarioService inventarioService,
                        DisponibilidadDAO disponibilidadDAO) {
        this.pedidoService = pedidoService;
        this.inventarioService = inventarioService;
        this.disponibilidadDAO = disponibilidadDAO;
    }

    /**
     * Valida el pedido COMPLETO contra el inventario.
     *
     * @return lo que falta; lista vacía si el pedido se puede preparar
     */
    public List<String> validarDisponibilidad(Collection<ItemPedido> items) throws SQLException {
        return disponibilidadDAO.faltantesDelPedido(aLineas(items));
    }

    /**
     * Guarda el pedido de la mesa y descuenta el inventario.
     *
     * Si el pedido se creó en esta llamada y el descuento falla, se cancela
     * para no dejar una comanda vacía ocupando la mesa.
     *
     * @throws IllegalStateException si el inventario de ese pedido ya se descontó
     */
    public Pedido confirmarPedido(long mesaId, long usuarioId, List<DetallePedido> detalles)
            throws SQLException, Exception {

        BigDecimal subtotal = calcularSubtotal(detalles);

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Obtener o crear el pedido activo usando la conexión transaccional
                Optional<Pedido> pedidoOpt = pedidoRepository.findActivoByMesaIdWithConn(conn, mesaId);
                Pedido pedido;
                if (pedidoOpt.isPresent()) {
                    pedido = pedidoOpt.get();
                } else {
                    pedido = new Pedido();
                    pedido.setMesaId(mesaId);
                    pedido.setUsuarioId(usuarioId);
                    pedido.setNumeroPedido("PED-" + System.currentTimeMillis());
                    pedido.setEstado(EstadoPedido.PENDIENTE);
                    pedido = pedidoRepository.saveWithConn(conn, pedido);
                }

                if (pedido.isInventarioDescontado()) {
                    throw new IllegalStateException(
                            "El inventario de este pedido ya fue descontado. "
                                    + "No se puede volver a confirmar para evitar un descuento duplicado.");
                }

                // 2. Guardar los detalles en la tabla relacional detalle_pedido
                for (DetallePedido detalle : detalles) {
                    detalle.setPedidoId(pedido.getId());
                }
                detallePedidoRepository.guardarDetallesConConn(conn, detalles);

                // 3. Mapear productos para el descuento de inventario
                Map<Long, Integer> lineas = new HashMap<>();
                for (DetallePedido detalle : detalles) {
                    lineas.put(detalle.getProductoId(), detalle.getCantidad());
                }

                // 4. Actualizar totales del pedido
                pedido.setSubtotal(subtotal);
                pedido.setTotal(subtotal);
                pedidoRepository.saveWithConn(conn, pedido);

                // 5. Descontar ingredientes de forma transaccional
                inventarioService.descontarIngredientesConConexion(conn, pedido.getId(), lineas, usuarioId);

                // 6. Marcar inventario como descontado
                pedido.setInventarioDescontado(true);
                pedidoRepository.saveWithConn(conn, pedido);

                // Confirmar transacción si todo finaliza correctamente
                conn.commit();
                return pedido;

            } catch (SQLException | RuntimeException e) {
                // Revertir cambios ante cualquier fallo para garantizar consistencia y retorno de stock
                conn.rollback();
                throw e;
            }
        }
    }
    /** Total del pedido, para mostrarlo en pantalla. */
    public BigDecimal calcularSubtotal(Collection<ItemPedido> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : items) {
            subtotal = subtotal.add(item.getSubtotal());
        }
        return subtotal;
    }

    // ------------------------------------------------------------------

    /** producto_id -> cantidad pedida. */
    private Map<Long, Integer> aLineas(Collection<ItemPedido> items) {
        Map<Long, Integer> lineas = new LinkedHashMap<>();
        for (ItemPedido item : items) {
            lineas.put(item.getProducto().getProductoId(), item.getCantidad());
        }
        return lineas;
    }

    private String construirJson(Collection<ItemPedido> items) {
        StringBuilder json = new StringBuilder("[");
        boolean primero = true;
        for (ItemPedido item : items) {
            if (!primero) {
                json.append(",");
            }
            json.append("{")
                    .append("\"producto_id\":").append(item.getProducto().getProductoId()).append(",")
                    .append("\"cantidad\":").append(item.getCantidad()).append(",")
                    .append("\"precio_unitario\":").append(item.getProducto().getPrecioVenta()).append(",")
                    .append("\"subtotal\":").append(item.getSubtotal())
                    .append("}");
            primero = false;
        }
        return json.append("]").toString();
    }
}
