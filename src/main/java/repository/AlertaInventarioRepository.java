package repository;

import ConexionDB.ConexionBD;
import entity.AlertaInventario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlertaInventarioRepository {

    private static final String SELECT_BASE =
            "SELECT id, ingrediente_id, producto_id, stock_actual, stock_minimo, " +
                    "cantidad_sugerida, estado, created_at, resolved_at FROM alerta_inventario ";

    public Optional<AlertaInventario> findById(long id) throws SQLException {
        String sql = SELECT_BASE + "WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    // Alertas vigentes (todo lo que no esté RESUELTA), las más antiguas primero
    public List<AlertaInventario> findActivas() throws SQLException {
        String sql = SELECT_BASE + "WHERE estado <> ? ORDER BY created_at";
        List<AlertaInventario> resultado = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, AlertaInventario.ESTADO_RESUELTA);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resultado.add(map(rs));
                }
            }
        }
        return resultado;
    }

    // Para evitar crear una alerta duplicada si el ingrediente ya tiene una vigente
    public Optional<AlertaInventario> findActivaByIngredienteId(long ingredienteId) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            return findActivaByIngredienteId(conn, ingredienteId);
        }
    }

    public Optional<AlertaInventario> findActivaByIngredienteId(Connection conn, long ingredienteId)
            throws SQLException {
        String sql = SELECT_BASE + "WHERE ingrediente_id = ? AND estado <> ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ingredienteId);
            stmt.setString(2, AlertaInventario.ESTADO_RESUELTA);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public int countActivas() throws SQLException {
        String sql = "SELECT COUNT(*) FROM alerta_inventario WHERE estado <> ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, AlertaInventario.ESTADO_RESUELTA);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public AlertaInventario save(AlertaInventario a) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            return save(conn, a);
        }
    }

    public AlertaInventario save(Connection conn, AlertaInventario a) throws SQLException {
        if (a.getId() == null) {
            String sql = "INSERT INTO alerta_inventario (ingrediente_id, producto_id, stock_actual, " +
                    "stock_minimo, cantidad_sugerida, estado, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setLongNullable(stmt, 1, a.getIngredienteId());
                setLongNullable(stmt, 2, a.getProductoId());
                stmt.setBigDecimal(3, a.getStockActual());
                stmt.setBigDecimal(4, a.getStockMinimo());
                stmt.setBigDecimal(5, a.getCantidadSugerida());
                stmt.setString(6, a.getEstado());
                stmt.setTimestamp(7, Timestamp.valueOf(
                        a.getCreatedAt() != null ? a.getCreatedAt() : java.time.LocalDateTime.now()));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        a.setId(rs.getLong(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE alerta_inventario SET stock_actual = ?, stock_minimo = ?, " +
                    "cantidad_sugerida = ?, estado = ?, resolved_at = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBigDecimal(1, a.getStockActual());
                stmt.setBigDecimal(2, a.getStockMinimo());
                stmt.setBigDecimal(3, a.getCantidadSugerida());
                stmt.setString(4, a.getEstado());
                if (a.getResolvedAt() != null) {
                    stmt.setTimestamp(5, Timestamp.valueOf(a.getResolvedAt()));
                } else {
                    stmt.setNull(5, Types.TIMESTAMP);
                }
                stmt.setLong(6, a.getId());
                stmt.executeUpdate();
            }
        }
        return a;
    }

    public void resolver(long id) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            resolver(conn, id);
        }
    }

    public void resolver(Connection conn, long id) throws SQLException {
        String sql = "UPDATE alerta_inventario SET estado = ?, resolved_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, AlertaInventario.ESTADO_RESUELTA);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    private void setLongNullable(PreparedStatement stmt, int index, Long valor) throws SQLException {
        if (valor != null) {
            stmt.setLong(index, valor);
        } else {
            stmt.setNull(index, Types.BIGINT);
        }
    }

    private Long getLongOrNull(ResultSet rs, String columna) throws SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : valor;
    }

    private AlertaInventario map(ResultSet rs) throws SQLException {
        AlertaInventario a = new AlertaInventario();
        a.setId(rs.getLong("id"));
        a.setIngredienteId(getLongOrNull(rs, "ingrediente_id"));
        a.setProductoId(getLongOrNull(rs, "producto_id"));
        a.setStockActual(rs.getBigDecimal("stock_actual"));
        a.setStockMinimo(rs.getBigDecimal("stock_minimo"));
        a.setCantidadSugerida(rs.getBigDecimal("cantidad_sugerida"));
        a.setEstado(rs.getString("estado"));
        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp ra = rs.getTimestamp("resolved_at");
        if (ra != null) {
            a.setResolvedAt(ra.toLocalDateTime());
        }
        return a;
    }
}