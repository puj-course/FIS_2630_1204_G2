package service;

import dao.DisponibilidadDAO;
import dto.ItemPedido;
import entity.Pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public Pedido confirmarPedido(long mesaId, long usuarioId, Collection<ItemPedido> items)
            throws SQLException {

        Map<Long, Integer> lineas = aLineas(items);
        BigDecimal subtotal = calcularSubtotal(items);

        // Si no hay pedido activo, obtenerOCrearPedido va a crear uno nuevo.
        boolean pedidoNuevo = pedidoService.obtenerPedidoActivo(mesaId).isEmpty();

        Pedido pedido = pedidoService.obtenerOCrearPedido(mesaId, usuarioId);

        try {
            if (pedido.isInventarioDescontado()) {
                throw new IllegalStateException(
                        "El inventario de este pedido ya fue descontado. "
                                + "No se puede volver a confirmar para evitar un descuento duplicado.");
            }

            pedido.setProductos(construirJson(items));
            BigDecimal impuestos = subtotal
                    .multiply(TASA_IVA)  // ej. new BigDecimal("0.19")
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = subtotal.add(impuestos);

            pedido.setSubtotal(subtotal);
            pedido.setImpuestos(impuestos);
            pedido.setTotal(total);
            pedidoService.actualizarPedido(pedido);

            inventarioService.descontarIngredientes(pedido.getId(), lineas, usuarioId);

            pedido.setInventarioDescontado(true);
            pedidoService.actualizarPedido(pedido);
            return pedido;

        } catch (SQLException | RuntimeException e) {
            if (pedidoNuevo && pedido.getId() != null) {
                try {
                    pedidoService.cancelarPedido(pedido.getId(), "DISPONIBLE");
                } catch (Exception errorCancelacion) {
                    System.err.println("No se pudo cancelar el pedido creado: "
                            + errorCancelacion.getMessage());
                }
            }
            throw e;
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
