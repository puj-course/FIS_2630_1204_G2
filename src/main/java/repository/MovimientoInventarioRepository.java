package repository;

import ConexionDB.ConexionBD;
import entity.MovimientoInventario;
import entity.TipoMovimiento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovimientoInventarioRepository {

    private static final String SELECT_BASE =
            "SELECT id_movimiento, producto_id, ingrediente_id, tipo_movimiento, cantidad, " +
                    "motivo, usuario_id, fecha_movimiento FROM movimientos_inventario ";

    public Optional<MovimientoInventario> findById(long id) throws SQLException {
        String sql = SELECT_BASE + "WHERE id_movimiento = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    // Kardex de un ingrediente, del más reciente al más antiguo
    public List<MovimientoInventario> findByIngredienteId(long ingredienteId) throws SQLException {
        String sql = SELECT_BASE + "WHERE ingrediente_id = ? ORDER BY fecha_movimiento DESC";
        return consultarLista(sql, ingredienteId);
    }

    public List<MovimientoInventario> findByProductoId(long productoId) throws SQLException {
        String sql = SELECT_BASE + "WHERE producto_id = ? ORDER BY fecha_movimiento DESC";
        return consultarLista(sql, productoId);
    }

    public MovimientoInventario save(MovimientoInventario m) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            return save(conn, m);
        }
    }

    // Versión transaccional: reutiliza la Connection recibida para que el movimiento
    // y el cambio de stock del ingrediente puedan confirmarse o revertirse juntos.
    public MovimientoInventario save(Connection conn, MovimientoInventario m) throws SQLException {
        if (m.getUsuarioId() == null) {
            throw new SQLException("usuario_id es obligatorio para registrar un movimiento");
        }

        String sql = "INSERT INTO movimientos_inventario (producto_id, ingrediente_id, tipo_movimiento, " +
                "cantidad, motivo, usuario_id, fecha_movimiento) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING id_movimiento";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setLongNullable(stmt, 1, m.getProductoId());
            setLongNullable(stmt, 2, m.getIngredienteId());
            stmt.setString(3, m.getTipoMovimiento().name());
            stmt.setBigDecimal(4, m.getCantidad());
            stmt.setString(5, m.getMotivo());
            stmt.setLong(6, m.getUsuarioId());
            stmt.setTimestamp(7, Timestamp.valueOf(
                    m.getFechaMovimiento() != null ? m.getFechaMovimiento() : java.time.LocalDateTime.now()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    m.setId(rs.getLong(1));
                }
            }
        }
        return m;
    }

    private List<MovimientoInventario> consultarLista(String sql, long id) throws SQLException {
        List<MovimientoInventario> resultado = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resultado.add(map(rs));
                }
            }
        }
        return resultado;
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

    private MovimientoInventario map(ResultSet rs) throws SQLException {
        MovimientoInventario m = new MovimientoInventario();
        m.setId(rs.getLong("id_movimiento"));
        m.setProductoId(getLongOrNull(rs, "producto_id"));
        m.setIngredienteId(getLongOrNull(rs, "ingrediente_id"));
        m.setTipoMovimiento(TipoMovimiento.valueOf(rs.getString("tipo_movimiento")));
        m.setCantidad(rs.getBigDecimal("cantidad"));
        m.setMotivo(rs.getString("motivo"));
        m.setUsuarioId(rs.getLong("usuario_id"));
        m.setFechaMovimiento(rs.getTimestamp("fecha_movimiento").toLocalDateTime());
        return m;
    }
}