package repository;

import ConexionDB.ConexionBD;
import entity.HistorialEstadoMesa;

import java.sql.*;

public class HistorialEstadoMesaRepository {

    // Versión normal: abre su propia conexión (para usos sueltos, fuera de una transacción)
    public HistorialEstadoMesa save(HistorialEstadoMesa h) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            return save(conn, h);
        }
    }

    // Versión transaccional: reutiliza la Connection que le pasen, sin abrir ni cerrar nada.
    // Así puede compartir la misma transacción que el UPDATE de la mesa en MesaService.
    public HistorialEstadoMesa save(Connection conn, HistorialEstadoMesa h) throws SQLException {
        String sql = "INSERT INTO historial_estado_mesa " +
                "(mesa_id, estado_anterior_id, estado_nuevo_id, motivo, fecha_cambio) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, h.getMesaId());

            if (h.getEstadoAnteriorId() != null) {
                stmt.setLong(2, h.getEstadoAnteriorId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }

            stmt.setLong(3, h.getEstadoNuevoId());
            stmt.setString(4, h.getMotivo());
            stmt.setTimestamp(5, Timestamp.valueOf(
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