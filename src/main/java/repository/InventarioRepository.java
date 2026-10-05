package repository;

import database.ConexionBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventarioRepository {

    /**
     * Representa una receta asociada a un producto.
     */
    public static class IngredienteReceta {

        private final long ingredienteId;
        private final BigDecimal cantidad;

        public IngredienteReceta(
                long ingredienteId,
                BigDecimal cantidad
        ) {
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
     * Representa una salida de inventario asociada
     * a un pedido.
     */
    public static class MovimientoPedido {

        private final long ingredienteId;
        private final BigDecimal cantidad;

        public MovimientoPedido(
                long ingredienteId,
                BigDecimal cantidad
        ) {
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
     * Obtiene la receta almacenada en productos.ingredientes.
     */
    public List<IngredienteReceta> obtenerReceta(
            Connection conn,
            long productoId
    ) throws SQLException {

        String sql =
                "SELECT ingredientes " +
                        "FROM productos " +
                        "WHERE producto_id = ?";

        List<IngredienteReceta> receta =
                new ArrayList<>();

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setLong(1, productoId);

            try (ResultSet rs =
                         stmt.executeQuery()) {

                if (!rs.next()) {

                    throw new SQLException(
                            "No existe el producto con ID "
                                    + productoId
                    );
                }

                String json =
                        rs.getString("ingredientes");

                if (json == null
                        || json.isBlank()
                        || json.equals("[]")) {

                    return receta;
                }

                String contenido =
                        json.trim();

                if (contenido.startsWith("[")
                        && contenido.endsWith("]")) {

                    contenido =
                            contenido.substring(
                                    1,
                                    contenido.length() - 1
                            );
                }

                if (contenido.isBlank()) {
                    return receta;
                }

                String[] elementos =
                        contenido.split(
                                "\\},\\s*\\{"
                        );

                for (String elemento :
                        elementos) {

                    String limpio =
                            elemento
                                    .replace("{", "")
                                    .replace("}", "")
                                    .replace("\"", "")
                                    .trim();

                    Long ingredienteId = null;
                    BigDecimal cantidad = null;

                    String[] propiedades =
                            limpio.split(",");

                    for (String propiedad :
                            propiedades) {

                        String[] partes =
                                propiedad.split(
                                        ":",
                                        2
                                );

                        if (partes.length != 2) {
                            continue;
                        }

                        String clave =
                                partes[0].trim();

                        String valor =
                                partes[1].trim();

                        if ("ingrediente_id".equals(clave)) {

                            ingredienteId =
                                    Long.parseLong(
                                            valor
                                    );
                        }

                        if ("cantidad".equals(clave)) {

                            cantidad =
                                    new BigDecimal(
                                            valor
                                    );
                        }
                    }

                    if (ingredienteId == null
                            || cantidad == null) {

                        throw new SQLException(
                                "La receta del producto "
                                        + productoId
                                        + " contiene un ingrediente inválido."
                        );
                    }

                    if (cantidad.compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

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
                }
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

        String sql =
                "UPDATE ingredientes " +
                        "SET stock_actual = stock_actual - ?, " +
                        "fecha_actualizacion = CURRENT_TIMESTAMP " +
                        "WHERE ingrediente_id = ?";

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
                        "No se pudo actualizar el stock "
                                + "del ingrediente "
                                + ingredienteId
                );
            }
        }
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

        String sql =
                "UPDATE ingredientes " +
                        "SET stock_actual = stock_actual + ?, " +
                        "fecha_actualizacion = CURRENT_TIMESTAMP " +
                        "WHERE ingrediente_id = ?";

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
                        "No se pudo devolver el stock "
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

        String sql =
                "INSERT INTO movimientos_inventario " +
                        "(producto_id, ingrediente_id, tipo_movimiento, " +
                        "cantidad, motivo, usuario_id, fecha_movimiento) " +
                        "VALUES (NULL, ?, 'SALIDA', ?, ?, ?, CURRENT_TIMESTAMP)";

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

        String sql =
                "INSERT INTO movimientos_inventario " +
                        "(producto_id, ingrediente_id, tipo_movimiento, " +
                        "cantidad, motivo, usuario_id, fecha_movimiento) " +
                        "VALUES (NULL, ?, 'ENTRADA', ?, ?, ?, CURRENT_TIMESTAMP)";

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
