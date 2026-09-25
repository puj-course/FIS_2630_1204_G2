package repository;

import ConexionDB.ConexionBD;
import entity.AsignacionMesa;

import java.sql.*;
import java.util.Optional;

public class AsignacionMesaRepository {

    public Optional<AsignacionMesa> findActivaByMesaId(long mesaId) throws SQLException {
        String sql = "SELECT id, mesa_id, usuario_id, fecha_asignacion, is_active " +
                "FROM asignacion_mesa WHERE mesa_id = ? AND is_active = TRUE LIMIT 1";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, mesaId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public AsignacionMesa save(AsignacionMesa a) throws SQLException {
        if (a.getId() == null) {
            String sql = "INSERT INTO asignacion_mesa (mesa_id, usuario_id, fecha_asignacion, is_active) " +
                    "VALUES (?, ?, ?, ?) RETURNING id";
            try (Connection conn = ConexionBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, a.getMesaId());
                stmt.setLong(2, a.getUsuarioId());
                stmt.setTimestamp(3, Timestamp.valueOf(
                        a.getFechaAsignacion() != null ? a.getFechaAsignacion() : java.time.LocalDateTime.now()));
                stmt.setBoolean(4, a.getIsActive() != null ? a.getIsActive() : true);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        a.setId(rs.getLong(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE asignacion_mesa SET mesa_id = ?, usuario_id = ?, is_active = ? WHERE id = ?";
            try (Connection conn = ConexionBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, a.getMesaId());
                stmt.setLong(2, a.getUsuarioId());
                stmt.setBoolean(3, a.getIsActive() != null ? a.getIsActive() : true);
                stmt.setLong(4, a.getId());
                stmt.executeUpdate();
            }
        }
        return a;
    }

    private AsignacionMesa map(ResultSet rs) throws SQLException {
        AsignacionMesa a = new AsignacionMesa();
        a.setId(rs.getLong("id"));
        a.setMesaId(rs.getLong("mesa_id"));
        a.setUsuarioId(rs.getLong("usuario_id"));
        Timestamp ts = rs.getTimestamp("fecha_asignacion");
        if (ts != null) {
            a.setFechaAsignacion(ts.toLocalDateTime());
        }
        a.setIsActive(rs.getBoolean("is_active"));
        return a;
    }
}