package repository;

import ConexionBD.ConexionBD;
import entity.DetallePedido;
import entity.DetallePedidoAdicional;

import java.sql.*;
import java.util.List;

public class DetallePedidoRepository {

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

    public void guardarDetallesConConn(Connection conn, List<DetallePedido> detalles) throws SQLException {
        if (detalles == null || detalles.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad, precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (DetallePedido detalle : detalles) {
                stmt.setLong(1, detalle.getPedidoId());
                stmt.setLong(2, detalle.getProductoId());
                stmt.setInt(3, detalle.getCantidad());
                stmt.setBigDecimal(4, detalle.getPrecioUnitario());
                stmt.setBigDecimal(5, detalle.getSubtotal());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}