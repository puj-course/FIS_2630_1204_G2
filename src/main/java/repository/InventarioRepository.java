package repository;

import entity.ProductoIngrediente;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventarioRepository {

    public static class MovimientoPedido {
        private final long ingredienteId;
        private final BigDecimal cantidad;

        public MovimientoPedido(long ingredienteId, BigDecimal cantidad) {
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
     * Obtiene la receta de un producto utilizando la entidad unificada ProductoIngrediente
     * y consultando exclusivamente la tabla producto_ingrediente.
     */
    public List<ProductoIngrediente> obtenerReceta(
            Connection conn,
            long productoId
    ) throws SQLException {
        String sql = "SELECT producto_id, ingrediente_id, cantidad_necesaria " +
                "FROM producto_ingrediente " +
                "WHERE producto_id = ? " +
                "ORDER BY ingrediente_id";

        List<ProductoIngrediente> receta = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, productoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long prodId = rs.getLong("producto_id");
                    long ingredienteId = rs.getLong("ingrediente_id");
                    BigDecimal cantidad = rs.getBigDecimal("cantidad_necesaria");

                    if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new SQLException(
                                "La cantidad necesaria del ingrediente " + ingredienteId +
                                        " para el producto " + productoId + " debe ser mayor que cero."
                        );
                    }
                    receta.add(new ProductoIngrediente(prodId, ingredienteId, cantidad));
                }
            }
        }

        if (receta.isEmpty()) {
            String checkProducto = "SELECT 1 FROM productos WHERE producto_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkProducto)) {
                stmt.setLong(1, productoId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("No existe el producto con ID " + productoId);
                    }
                }
            }
        }

        return receta;
    }

    /* Obtiene el stock actual bloqueando la fila */
    public BigDecimal obtenerStockForUpdate(
            Connection conn,
            long ingredienteId
    ) throws SQLException {

        String sql = "SELECT stock_actual " +
                "FROM ingredientes " +
                "WHERE ingrediente_id = ? " +
                "FOR UPDATE";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ingredienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No existe el ingrediente con ID " + ingredienteId);
                }
                BigDecimal stock = rs.getBigDecimal("stock_actual");
                if (stock == null) {
                    throw new SQLException("El ingrediente " + ingredienteId + " tiene stock nulo.");
                }
                return stock;
            }
        }
    }

    /* Descuenta stock de manera exclusiva en la tabla ingredientes. */
    public void descontarStock(
            Connection conn,
            long ingredienteId,
            BigDecimal cantidad
    ) throws SQLException {

        String sql = "UPDATE ingredientes " +
                "SET stock_actual = stock_actual - ?, " +
                "fecha_actualizacion = CURRENT_TIMESTAMP " +
                "WHERE ingrediente_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, cantidad);
            stmt.setLong(2, ingredienteId);
            int filasActualizadas = stmt.executeUpdate();
            if (filasActualizadas != 1) {
                throw new SQLException("No se pudo actualizar (descontar) el stock del ingrediente " + ingredienteId);
            }
        }
    }

    /* Devuelve stock a la tabla ingredientes (reversiones). */
    public void devolverStock(
            Connection conn,
            long ingredienteId,
            BigDecimal cantidad
    ) throws SQLException {

        String sql = "UPDATE ingredientes " +
                "SET stock_actual = stock_actual + ?, " +
                "fecha_actualizacion = CURRENT_TIMESTAMP " +
                "WHERE ingrediente_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, cantidad);
            stmt.setLong(2, ingredienteId);
            int filasActualizadas = stmt.executeUpdate();
            if (filasActualizadas != 1) {
                throw new SQLException("No se pudo devolver el stock del ingrediente " + ingredienteId);
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

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ingredienteId);
            stmt.setBigDecimal(2, cantidad);
            stmt.setString(3, motivo);
            stmt.setLong(4, usuarioId);
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

        String sql = "INSERT INTO movimientos_inventario " +
                "(producto_id, ingrediente_id, tipo_movimiento, cantidad, motivo, usuario_id, fecha_movimiento) " +
                "VALUES (NULL, ?, 'ENTRADA', ?, ?, ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ingredienteId);
            stmt.setBigDecimal(2, cantidad);
            stmt.setString(3, motivo);
            stmt.setLong(4, usuarioId);
            stmt.executeUpdate();
        }
    }

    /**
     * Obtiene las salidas de ingredientes asociadas a un pedido.
     */
    public List<MovimientoPedido> obtenerSalidasPorPedido(
            Connection conn,
            long pedidoId
    ) throws SQLException {

        String sql = "SELECT ingrediente_id, cantidad " +
                "FROM movimientos_inventario " +
                "WHERE tipo_movimiento = 'SALIDA' " +
                "AND ingrediente_id IS NOT NULL " +
                "AND motivo = ? " +
                "ORDER BY id_movimiento";

        List<MovimientoPedido> movimientos = new ArrayList<>();
        String motivo = "CONSUMO_COMANDA - PEDIDO_ID=" + pedidoId;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, motivo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(new MovimientoPedido(
                            rs.getLong("ingrediente_id"),
                            rs.getBigDecimal("cantidad")
                    ));
                }
            }
        }

        return movimientos;
    }

    /**
     * Verifica si ya existe una reversión de inventario para un pedido.
     */
    public boolean existeReversionPorPedido(
            Connection conn,
            long pedidoId
    ) throws SQLException {

        String sql = "SELECT 1 " +
                "FROM movimientos_inventario " +
                "WHERE tipo_movimiento = 'ENTRADA' " +
                "AND motivo = ? " +
                "LIMIT 1";

        String motivo = "REVERSO_COMANDA - PEDIDO_ID=" + pedidoId;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, motivo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Obtiene el nombre del ingrediente desde la tabla maestra.
     */
    public String obtenerNombreIngrediente(
            Connection conn,
            long ingredienteId
    ) throws SQLException {

        String sql = "SELECT nombre FROM ingredientes WHERE ingrediente_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ingredienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return "Ingrediente #" + ingredienteId;
                }
                return rs.getString("nombre");
            }
        }
    }

    /**
     * Obtiene el stock mínimo configurado para las alertas del ingrediente.
     */
    public BigDecimal obtenerStockMinimo(
            Connection conn,
            long ingredienteId
    ) throws SQLException {

        String sql = "SELECT stock_minimo FROM ingredientes WHERE ingrediente_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ingredienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No existe el ingrediente con ID " + ingredienteId);
                }
                return rs.getBigDecimal("stock_minimo");
            }
        }
    }
}