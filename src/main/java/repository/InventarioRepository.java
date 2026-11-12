package repository;

import ConexionBD.ConexionBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventarioRepository {

    /** Resta del stock de un ingrediente. */
    private static final String SQL_DESCONTAR_STOCK =
            "UPDATE ingredientes "
                    + "SET stock_actual = stock_actual - ?, "
                    + "fecha_actualizacion = CURRENT_TIMESTAMP "
                    + "WHERE ingrediente_id = ?";

    /** Suma al stock de un ingrediente. */
    private static final String SQL_DEVOLVER_STOCK =
            "UPDATE ingredientes "
                    + "SET stock_actual = stock_actual + ?, "
                    + "fecha_actualizacion = CURRENT_TIMESTAMP "
                    + "WHERE ingrediente_id = ?";

    /** Deja constancia de una salida de inventario. */
    private static final String SQL_MOVIMIENTO_SALIDA =
            "INSERT INTO movimientos_inventario "
                    + "(producto_id, ingrediente_id, tipo_movimiento, "
                    + "cantidad, motivo, usuario_id, fecha_movimiento) "
                    + "VALUES (NULL, ?, 'SALIDA', ?, ?, ?, CURRENT_TIMESTAMP)";

    /** Deja constancia de una entrada de inventario. */
    private static final String SQL_MOVIMIENTO_ENTRADA =
            "INSERT INTO movimientos_inventario "
                    + "(producto_id, ingrediente_id, tipo_movimiento, "
                    + "cantidad, motivo, usuario_id, fecha_movimiento) "
                    + "VALUES (NULL, ?, 'ENTRADA', ?, ?, ?, CURRENT_TIMESTAMP)";

    /**
     * Un ingrediente con una cantidad. Es lo unico que comparten una linea de
     * receta y una salida de inventario, y esta aqui para no escribir dos
     * veces los mismos campos y los mismos getters.
     */
    public abstract static class CantidadDeIngrediente {

        private final long ingredienteId;
        private final BigDecimal cantidad;

        protected CantidadDeIngrediente(long ingredienteId, BigDecimal cantidad) {
            this.ingredienteId = ingredienteId;
            this.cantidad = cantidad;
        }

        public long getIngredienteId() {
            return ingredienteId;
        }

        public BigDecimal getCantidad() {
            return cantidad;
        }
    }

    /**
     * Representa una receta asociada a un producto: cuanto de un ingrediente
     * lleva el plato.
     */
    public static class IngredienteReceta extends CantidadDeIngrediente {

        public IngredienteReceta(long ingredienteId, BigDecimal cantidad) {
            super(ingredienteId, cantidad);
        }
    }

    /**
     * Representa una salida de inventario asociada a un pedido: cuanto de un
     * ingrediente se descontó.
     */
    public static class MovimientoPedido extends CantidadDeIngrediente {

        public MovimientoPedido(long ingredienteId, BigDecimal cantidad) {
            super(ingredienteId, cantidad);
        }
    }

    /**
     * Obtiene la receta almacenada en plato_insumo.
     */
    public List<IngredienteReceta> obtenerReceta(
            Connection conn,
            long productoId
    ) throws SQLException {

        String sql =
                "SELECT p.producto_id, " +
                        "pi.insumo_id AS ingrediente_id, " +
                        "pi.cantidad_necesaria AS cantidad " +
                        "FROM productos p " +
                        "LEFT JOIN plato_insumo pi " +
                        "ON pi.plato_id = p.producto_id " +
                        "WHERE p.producto_id = ? " +
                        "ORDER BY pi.insumo_id";

        List<IngredienteReceta> receta = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, productoId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    throw new SQLException(
                            "No existe el producto con ID " + productoId
                    );
                }

                do {
                    long ingredienteId = rs.getLong("ingrediente_id");

                    if (rs.wasNull()) {
                        // El producto existe, pero no tiene receta.
                        continue;
                    }

                    BigDecimal cantidad =
                            rs.getBigDecimal("cantidad");

                    if (cantidad == null
                            || cantidad.compareTo(BigDecimal.ZERO) <= 0) {

                        throw new SQLException(
                                "La cantidad de receta del ingrediente "
                                        + ingredienteId
                                        + " debe ser mayor que cero."
                        );
                    }

                    receta.add(
                            new IngredienteReceta(
                                    ingredienteId,
                                    cantidad
                            )
                    );

                } while (rs.next());
            }
        }

        return receta;
    }

    /**
     * Obtiene el stock actual bloqueando la fila.
     */
    public BigDecimal obtenerStockForUpdate(
            Connection conn,
            long ingredienteId
    ) throws SQLException {

        String sql =
                "SELECT stock_actual " +
                        "FROM ingredientes " +
                        "WHERE ingrediente_id = ? " +
                        "FOR UPDATE";

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setLong(1, ingredienteId);

            try (ResultSet rs =
                         stmt.executeQuery()) {

                if (!rs.next()) {

                    throw new SQLException(
                            "No existe el ingrediente con ID "
                                    + ingredienteId
                    );
                }

                BigDecimal stock =
                        rs.getBigDecimal(
                                "stock_actual"
                        );

                if (stock == null) {

                    throw new SQLException(
                            "El ingrediente "
                                    + ingredienteId
                                    + " tiene stock nulo."
                    );
                }

                return stock;
            }
        }
    }

    /**
     * Descuenta una cantidad del stock.
     */
    public void descontarStock(
            Connection conn,
            long ingredienteId,
            BigDecimal cantidad
    ) throws SQLException {

        moverStock(conn, ingredienteId, cantidad, SQL_DESCONTAR_STOCK, "actualizar");
    }

    /**
     * Suma nuevamente una cantidad al stock.
     *
     * Se utiliza cuando se revierte la anulación
     * de una comanda cuyo inventario ya había sido descontado.
     */
    public void devolverStock(
            Connection conn,
            long ingredienteId,
            BigDecimal cantidad
    ) throws SQLException {

        moverStock(conn, ingredienteId, cantidad, SQL_DEVOLVER_STOCK, "devolver");
    }

    /**
     * Suma o resta una cantidad al stock de un ingrediente.
     *
     * Descontar y devolver hacen exactamente lo mismo salvo el signo de la
     * sentencia, asi que el resto se escribe una sola vez.
     *
     * @param sql   SQL_DESCONTAR_STOCK o SQL_DEVOLVER_STOCK
     * @param verbo palabra que se usa en el mensaje de error
     */
    private void moverStock(
            Connection conn,
            long ingredienteId,
            BigDecimal cantidad,
            String sql,
            String verbo
    ) throws SQLException {

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setBigDecimal(
                    1,
                    cantidad
            );

            stmt.setLong(
                    2,
                    ingredienteId
            );

            int filasActualizadas =
                    stmt.executeUpdate();

            if (filasActualizadas != 1) {

                throw new SQLException(
                        "No se pudo " + verbo + " el stock "
                                + "del ingrediente "
                                + ingredienteId
                );
            }
        }
    }

    /**
     * Registra una salida de inventario.
     */
    public void registrarSalidaIngrediente(
            Connection conn,
            long ingredienteId,
            BigDecimal cantidad,
            String motivo,
            long usuarioId
    ) throws SQLException {

        registrarMovimientoIngrediente(
                conn, ingredienteId, cantidad, motivo, usuarioId, SQL_MOVIMIENTO_SALIDA);
    }

    /**
     * Registra una entrada de inventario producto
     * de la reversión de una comanda.
     */
    public void registrarEntradaIngrediente(
            Connection conn,
            long ingredienteId,
            BigDecimal cantidad,
            String motivo,
            long usuarioId
    ) throws SQLException {

        registrarMovimientoIngrediente(
                conn, ingredienteId, cantidad, motivo, usuarioId, SQL_MOVIMIENTO_ENTRADA);
    }

    /**
     * Escribe un movimiento de inventario de un ingrediente.
     *
     * La salida y la entrada insertan la misma fila con los mismos cuatro
     * parametros y solo cambian el tipo de movimiento, asi que el resto se
     * escribe una sola vez.
     *
     * @param sql SQL_MOVIMIENTO_SALIDA o SQL_MOVIMIENTO_ENTRADA
     */
    private void registrarMovimientoIngrediente(
            Connection conn,
            long ingredienteId,
            BigDecimal cantidad,
            String motivo,
            long usuarioId,
            String sql
    ) throws SQLException {

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setLong(
                    1,
                    ingredienteId
            );

            stmt.setBigDecimal(
                    2,
                    cantidad
            );

            stmt.setString(
                    3,
                    motivo
            );

            stmt.setLong(
                    4,
                    usuarioId
            );

            stmt.executeUpdate();
        }
    }

    /**
     * Obtiene las salidas de ingredientes asociadas
     * a un pedido.
     *
     * Como movimientos_inventario no tiene una columna
     * pedido_id, el ID se encuentra dentro del motivo.
     */
    public List<MovimientoPedido> obtenerSalidasPorPedido(
            Connection conn,
            long pedidoId
    ) throws SQLException {

        String sql =
                "SELECT ingrediente_id, cantidad " +
                        "FROM movimientos_inventario " +
                        "WHERE tipo_movimiento = 'SALIDA' " +
                        "AND ingrediente_id IS NOT NULL " +
                        "AND motivo = ? " +
                        "ORDER BY id_movimiento";

        List<MovimientoPedido> movimientos =
                new ArrayList<>();

        String motivo =
                "CONSUMO_COMANDA - PEDIDO_ID="
                        + pedidoId;

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setString(
                    1,
                    motivo
            );

            try (ResultSet rs =
                         stmt.executeQuery()) {

                while (rs.next()) {

                    movimientos.add(
                            new MovimientoPedido(
                                    rs.getLong(
                                            "ingrediente_id"
                                    ),
                                    rs.getBigDecimal(
                                            "cantidad"
                                    )
                            )
                    );
                }
            }
        }

        return movimientos;
    }

    /**
     * Verifica si ya existe una reversión para un pedido.
     */
    public boolean existeReversionPorPedido(
            Connection conn,
            long pedidoId
    ) throws SQLException {

        String sql =
                "SELECT 1 " +
                        "FROM movimientos_inventario " +
                        "WHERE tipo_movimiento = 'ENTRADA' " +
                        "AND motivo = ? " +
                        "LIMIT 1";

        String motivo =
                "REVERSO_COMANDA - PEDIDO_ID="
                        + pedidoId;

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setString(
                    1,
                    motivo
            );

            try (ResultSet rs =
                         stmt.executeQuery()) {

                return rs.next();
            }
        }
    }

    /**
     * Obtiene el nombre de un ingrediente.
     */
    public String obtenerNombreIngrediente(
            Connection conn,
            long ingredienteId
    ) throws SQLException {

        String sql =
                "SELECT nombre " +
                        "FROM ingredientes " +
                        "WHERE ingrediente_id = ?";

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setLong(
                    1,
                    ingredienteId
            );

            try (ResultSet rs =
                         stmt.executeQuery()) {

                if (!rs.next()) {

                    return "Ingrediente #"
                            + ingredienteId;
                }

                return rs.getString(
                        "nombre"
                );
            }
        }
    }

    /**
     * Obtiene el stock mínimo de un ingrediente.
     */
    public BigDecimal obtenerStockMinimo(
            Connection conn,
            long ingredienteId
    ) throws SQLException {

        String sql =
                "SELECT stock_minimo " +
                        "FROM ingredientes " +
                        "WHERE ingrediente_id = ?";

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setLong(
                    1,
                    ingredienteId
            );

            try (ResultSet rs =
                         stmt.executeQuery()) {

                if (!rs.next()) {

                    throw new SQLException(
                            "No existe el ingrediente con ID "
                                    + ingredienteId
                    );
                }

                return rs.getBigDecimal(
                        "stock_minimo"
                );
            }
        }
    }
}
