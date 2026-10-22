package repository;

import entity.DetallePedido;
import ConexionBD.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class DetallePedidoRepository {

    public void guardarDetalles(List<DetallePedido> detalles) throws SQLException {
        String sql = "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad, subtotal) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (DetallePedido detalle : detalles) {
                stmt.setLong(1, detalle.getPedidoId());
                stmt.setLong(2, detalle.getProductoId());
                stmt.setInt(3, detalle.getCantidad());
                stmt.setBigDecimal(4, detalle.getSubtotal());
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }
}