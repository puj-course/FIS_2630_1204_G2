package service;

import ConexionBD.ConexionBD;
import exceptions.StockInsuficienteException;
import repository.InventarioRepository;
import Interface.InsumoObserver;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventarioService {

    private final InventarioRepository inventarioRepository =
            new InventarioRepository();

    private final List<InsumoObserver> observadores = new ArrayList<>(); // GoF - AlertaInventario
    /**
     * Descuenta los ingredientes correspondientes a una comanda.
     * El cálculo se realiza utilizando la receta oficial
     * almacenada en plato_insumo.
     */
    /**
     * Descuenta el inventario de una comanda usando la conexion que le presten,
     * sin abrir ni cerrar la transaccion: de eso se encarga quien la llama.
     *
     * La usa PedidoFacade al confirmar un pedido, para que el descuento viaje
     * en la misma transaccion que la comanda. Estaba con el cuerpo vacio, asi
     * que confirmar un pedido por la fachada no descontaba nada del inventario.
     */
    public void descontarIngredientesConConexion(
            Connection conn,
            long pedidoId,
            Map<Long, Integer> productos,
            long usuarioId
    ) throws SQLException {

        if (productos == null || productos.isEmpty()) {
            return;
        }

        descontar(conn, pedidoId, calcularConsumoEstimado(productos), usuarioId);
    }
    
    public void descontarIngredientes(
            long pedidoId,
            Map<Long, Integer> productos,
            long usuarioId
    ) throws SQLException {

        if (productos == null || productos.isEmpty()) {
            return;
        }

        Map<Long, BigDecimal> consumoTotal = calcularConsumoEstimado(productos);

        if (consumoTotal.isEmpty()) {
            return;
        }

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                descontar(conn, pedidoId, consumoTotal, usuarioId);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * El descuento propiamente dicho, sobre la conexion que le den.
     *
     * Primero revisa que haya stock de todo y solo despues descuenta: asi la
     * comanda no se queda a medio descontar cuando falta un ingrediente. No
     * toca la transaccion, porque las dos formas de llamarlo la manejan
     * distinto.
     */
    private void descontar(
            Connection conn,
            long pedidoId,
            Map<Long, BigDecimal> consumoTotal,
            long usuarioId
    ) throws SQLException {

        if (consumoTotal.isEmpty()) {
            return;
        }

        // Evita descontar dos veces la misma comanda.
        if (!inventarioRepository.obtenerSalidasPorPedido(conn, pedidoId).isEmpty()) {
            return;
        }

        // Primera fase: que alcance el stock de todos los ingredientes.
        for (Map.Entry<Long, BigDecimal> entrada : consumoTotal.entrySet()) {
            long ingredienteId = entrada.getKey();
            BigDecimal cantidadNecesaria = entrada.getValue();

            BigDecimal stockActual =
                    inventarioRepository.obtenerStockForUpdate(conn, ingredienteId);

            if (stockActual == null) {
                throw new StockInsuficienteException(
                        "No se encontró el ingrediente con ID " + ingredienteId);
            }

            if (stockActual.compareTo(cantidadNecesaria) < 0) {
                String nombre = inventarioRepository.obtenerNombreIngrediente(conn, ingredienteId);
                throw new StockInsuficienteException(
                        "Stock insuficiente para el ingrediente '" + nombre + "'. "
                                + "Disponible: " + stockActual
                                + ", necesario: " + cantidadNecesaria);
            }
        }

        // Segunda fase: descontar y dejar constancia de cada salida.
        for (Map.Entry<Long, BigDecimal> entrada : consumoTotal.entrySet()) {
            long ingredienteId = entrada.getKey();
            BigDecimal cantidad = entrada.getValue();
            String motivo = "CONSUMO_COMANDA - PEDIDO_ID=" + pedidoId;

            inventarioRepository.descontarStock(conn, ingredienteId, cantidad);
            inventarioRepository.registrarSalidaIngrediente(
                    conn, ingredienteId, cantidad, motivo, usuarioId);
        }
    }

    /**
     * Revierte el descuento de inventario de una comanda.
     *
     * Se utiliza cuando una comanda que ya descontó ingredientes
     * es posteriormente anulada.
     */
    public void revertirDescuento(
            long pedidoId,
            long usuarioId
    ) throws SQLException {

        try (Connection conn = ConexionBD.getConnection()) {

            conn.setAutoCommit(false);

            try {

                /*
                 * Si ya existe una reversión para este pedido,
                 * no se vuelve a devolver el stock.
                 */
                if (inventarioRepository.existeReversionPorPedido(
                        conn,
                        pedidoId
                )) {

                    conn.rollback();
                    return;
                }

                /*
                 * Recuperamos todos los movimientos SALIDA
                 * generados originalmente por la comanda.
                 */
                List<InventarioRepository.MovimientoPedido> movimientos =
                        inventarioRepository.obtenerSalidasPorPedido(
                                conn,
                                pedidoId
                        );

                /*
                 * Si no existen salidas, no hay nada que revertir.
                 */
                if (movimientos.isEmpty()) {

                    conn.rollback();
                    return;
                }

                /*
                 * Agrupamos por ingrediente por seguridad.
                 *
                 * Esto permite devolver correctamente el stock
                 * incluso si un ingrediente aparece en más de
                 * un movimiento.
                 */
                Map<Long, BigDecimal> cantidadesPorIngrediente =
                        new LinkedHashMap<>();

                for (InventarioRepository.MovimientoPedido movimiento
                        : movimientos) {

                    cantidadesPorIngrediente.merge(
                            movimiento.getIngredienteId(),
                            movimiento.getCantidad(),
                            BigDecimal::add
                    );
                }

                /*
                 * Bloqueamos las filas antes de modificarlas.
                 */
                for (Long ingredienteId :
                        cantidadesPorIngrediente.keySet()) {

                    inventarioRepository.obtenerStockForUpdate(
                            conn,
                            ingredienteId
                    );
                }

                /*
                 * Devolvemos el stock y registramos
                 * cada reversión como ENTRADA.
                 */
                for (Map.Entry<Long, BigDecimal> entry :
                        cantidadesPorIngrediente.entrySet()) {

                    long ingredienteId =
                            entry.getKey();

                    BigDecimal cantidad =
                            entry.getValue();

                    String motivo = "REVERSO_COMANDA - PEDIDO_ID=" + pedidoId;

                    inventarioRepository.devolverStock(conn, ingredienteId, cantidad);
                    inventarioRepository.registrarEntradaIngrediente(
                            conn, ingredienteId, cantidad, motivo, usuarioId);
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {

                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Calcula el consumo total de ingredientes para
     * todos los productos de una comanda.
     */
    public Map<Long, BigDecimal> calcularConsumoEstimado(
            Map<Long, Integer> productos
    ) throws SQLException {

        Map<Long, BigDecimal> consumoTotal =
                new LinkedHashMap<>();

        if (productos == null || productos.isEmpty()) {
            return consumoTotal;
        }

        try (Connection conn = ConexionBD.getConnection()) {

            for (Map.Entry<Long, Integer> producto :
                    productos.entrySet()) {

                long productoId =
                        producto.getKey();

                int cantidadProductos =
                        producto.getValue() != null
                                ? producto.getValue()
                                : 0;

                if (cantidadProductos <= 0) {
                    continue;
                }

                List<InventarioRepository.IngredienteReceta> receta =
                        inventarioRepository.obtenerReceta(
                                conn,
                                productoId
                        );

                for (InventarioRepository.IngredienteReceta ingrediente :
                        receta) {

                    BigDecimal cantidadReceta =
                            ingrediente.getCantidad();

                    BigDecimal cantidadNecesaria =
                            cantidadReceta.multiply(
                                    BigDecimal.valueOf(
                                            cantidadProductos
                                    )
                            );

                    consumoTotal.merge(
                            ingrediente.getIngredienteId(),
                            cantidadNecesaria,
                            BigDecimal::add
                    );
                }
            }
        }

        return consumoTotal;
    }

    /**
     * Calcula el consumo de ingredientes para un solo producto.
     */
    public Map<Long, BigDecimal> calcularConsumoProducto(
            long productoId,
            int cantidad
    ) throws SQLException {

        Map<Long, Integer> productos =
                new LinkedHashMap<>();

        productos.put(
                productoId,
                cantidad
        );

        return calcularConsumoEstimado(productos);
    }

    // Uso del patron observer para alerta de stock bajo - GoF
    public InventarioService() {
        // Suscribimos por defecto al administrador para que reciba las alertas.
        // Se agrega directamente a la lista y no con agregarObservador() porque
        // ese metodo se puede sobrescribir y llamarlo desde el constructor deja
        // a medio construir a cualquier clase que herede de esta.
        observadores.add(new AdministradorAlertaObserver());
    }

    public void agregarObservador(InsumoObserver observer) {
        observadores.add(observer);
    }

    public void eliminarObservador(InsumoObserver observer) {
        observadores.remove(observer);
    }

    private void notificarObservadores(String nombreInsumo, double stockActual, double stockMinimo) {
        for (InsumoObserver observer : observadores) {
            observer.actualizar(nombreInsumo, stockActual, stockMinimo);
        }
    }

    // Método donde validas el stock (ej: al descontar ingredientes por un pedido)
    public void verificarStockMinimo(String nombreInsumo, double stockActual, double stockMinimo) {
        if (stockActual <= stockMinimo) {
            notificarObservadores(nombreInsumo, stockActual, stockMinimo);
        }
    }
}
