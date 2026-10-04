package repository;

import ConexionDB.ConexionBD;
import entity.DetallePedidoAdicional;

import java.sql.*;

public class DetallePedidoAdicionalRepository {

    public DetallePedidoAdicional save(DetallePedidoAdicional d) throws SQLException {
        if (d.getId() == null) {
            String sql = "INSERT INTO detalle_pedido_adicional (detalle_pedido_id, adicional_id, cantidad) " +
                    "VALUES (?, ?, ?) RETURNING id";
            try (Connection conn = ConexionBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, d.getDetallePedidoId());
                stmt.setLong(2, d.getAdicionalId());
                stmt.setInt(3, d.getCantidad() != null ? d.getCantidad() : 1);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        d.setId(rs.getLong(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE detalle_pedido_adicional SET detalle_pedido_id = ?, adicional_id = ?, cantidad = ? WHERE id = ?";
            try (Connection conn = ConexionBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, d.getDetallePedidoId());
                stmt.setLong(2, d.getAdicionalId());
                stmt.setInt(3, d.getCantidad());
                stmt.setLong(4, d.getId());
                stmt.executeUpdate();
            }
        }
        return d;
    }
}