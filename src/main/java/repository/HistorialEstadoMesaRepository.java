package repository;

import ConexionDB.ConexionBD;
import entity.HistorialEstadoMesa;

import java.sql.*;

public class HistorialEstadoMesaRepository {

    public HistorialEstadoMesa save(HistorialEstadoMesa h) throws SQLException {
        String sql = "INSERT INTO historial_estado_mesa (mesa_id, estado_anterior_id, estado_nuevo_id, fecha_cambio) " +
                "VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, h.getMesaId());
            if (h.getEstadoAnteriorId() != null) {
                stmt.setLong(2, h.getEstadoAnteriorId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setLong(3, h.getEstadoNuevoId());
            stmt.setTimestamp(4, Timestamp.valueOf(
                    h.getFechaCambio() != null ? h.getFechaCambio() : java.time.LocalDateTime.now()));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    h.setId(rs.getLong(1));
                }
            }
        }
        return h;
    }
}