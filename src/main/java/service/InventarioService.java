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
    public void descontarIngredientes(
            long pedidoId,
            Map<Long, Integer> productos,
            long usuarioId
    ) throws SQLException {

        if (productos == null || productos.isEmpty()) {
            return;
        }

        Map<Long, BigDecimal> consumoTotal =
                calcularConsumoEstimado(productos);

        if (consumoTotal.isEmpty()) {
            return;
        }

        try (Connection conn = ConexionBD.getConnection()) {

            conn.setAutoCommit(false);

            try {

                /*
                 * Evita descontar nuevamente el inventario
                 * si la misma comanda ya fue procesada.
                 */
                List<InventarioRepository.MovimientoPedido> salidasExistentes =
                        inventarioRepository.obtenerSalidasPorPedido(
                                conn,
                                pedidoId
                        );

                if (!salidasExistentes.isEmpty()) {
                    conn.rollback();
                    return;
                }

                /*
                 * Primera fase:
                 * validar que todos los ingredientes tengan
                 * suficiente stock.
                 */
                for (Map.Entry<Long, BigDecimal> entry
                        : consumoTotal.entrySet()) {

                    long ingredienteId =
                            entry.getKey();

                    BigDecimal cantidadNecesaria =
                            entry.getValue();

                    BigDecimal stockActual =
                            inventarioRepository.obtenerStockForUpdate(
                                    conn,
                                    ingredienteId
                            );

                    if (stockActual == null) {

                        throw new StockInsuficienteException(
                                "No se encontró el ingrediente con ID "
                                        + ingredienteId
                        );
                    }

                    if (stockActual.compareTo(
                            cantidadNecesaria
                    ) < 0) {

                        String nombre =
                                inventarioRepository.obtenerNombreIngrediente(
                                        conn,
                                        ingredienteId
                                );

                        throw new StockInsuficienteException(
                                "Stock insuficiente para el ingrediente '"
                                        + nombre
                                        + "'. "
                                        + "Disponible: "
                                        + stockActual
                                        + ", necesario: "
                                        + cantidadNecesaria
                        );
                    }
                }

                /*
                 * Segunda fase:
                 * realizar los descuentos y registrar
                 * los movimientos de salida.
                 */
                for (Map.Entry<Long, BigDecimal> entry
                        : consumoTotal.entrySet()) {

                    long ingredienteId =
                            entry.getKey();

                    BigDecimal cantidad =
                            entry.getValue();

                    inventarioRepository.descontarStock(
                            conn,
                            ingredienteId,
                            cantidad
                    );

                    String motivo =
                            "CONSUMO_COMANDA - PEDIDO_ID="
                                    + pedidoId;

                    inventarioRepository.registrarSalidaIngrediente(
                            conn,
                            ingredienteId,
                            cantidad,
                            motivo,
                            usuarioId
                    );
                }

                conn.commit();

            } catch (StockInsuficienteException e) {

                conn.rollback();
                throw e;

            } catch (SQLException e) {

                conn.rollback();
                throw e;

            } catch (RuntimeException e) {

                conn.rollback();
                throw e;
            }
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

                    inventarioRepository.devolverStock(
                            conn,
                            ingredienteId,
                            cantidad
                    );

                    String motivo =
                            "REVERSO_COMANDA - PEDIDO_ID="
                                    + pedidoId;

                    inventarioRepository.registrarEntradaIngrediente(
                            conn,
                            ingredienteId,
                            cantidad,
                            motivo,
                            usuarioId
                    );
                }

                conn.commit();

            } catch (SQLException e) {

                conn.rollback();
                throw e;

            } catch (RuntimeException e) {

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
        // Suscribimos por defecto al administrador para que reciba las alertas
        this.agregarObservador(new AdministradorAlertaObserver());
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