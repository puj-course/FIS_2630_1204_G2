package repository;

import ConexionBD.ConexionBD;
import entity.AsignacionMesa;

import java.sql.*;
import java.util.Optional;

public class AsignacionMesaRepository {

    public Optional<AsignacionMesa> findActivaByMesaId(long mesaId) throws SQLException {
        String sql = "SELECT id_asignacion, id_mesa, id_cliente, id_usuario, cantidad_personas, fecha_asignacion, is_active " +
                "FROM asignacion_mesa WHERE id_mesa = ? AND is_active = TRUE LIMIT 1";
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
            String sql = "INSERT INTO asignacion_mesa (id_mesa,id_cliente, id_usuario, cantidad_personas, fecha_asignacion, is_active) " +
                    "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_asignacion";
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
            String sql = "UPDATE asignacion_mesa SET id_mesa = ?, id_usuario = ?,id_cliente = ?, is_active = ? WHERE id_asignacion = ?";
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
        a.setId(rs.getLong("id_asignacion"));
        a.setMesaId(rs.getLong("id_mesa"));
        a.setUsuarioId(rs.getLong("id_usuario"));
        Timestamp ts = rs.getTimestamp("fecha_asignacion");
        if (ts != null) {
            a.setFechaAsignacion(ts.toLocalDateTime());
        }
        a.setIsActive(rs.getBoolean("is_active"));
        return a;
    }
}