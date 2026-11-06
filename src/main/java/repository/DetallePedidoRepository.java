package repository;

import ConexionBD.ConexionBD;
import entity.DetallePedido;
import entity.DetallePedidoAdicional;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DetallePedidoRepository {

    /**
     * Guarda o actualiza un detalle de pedido de manera independiente.
     */
    public DetallePedido save(DetallePedido d) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            return saveWithConn(conn, d);
        }
    }

    /**
     * Guarda o actualiza un detalle de pedido utilizando una conexión transaccional existente.
     */
    public DetallePedido saveWithConn(Connection conn, DetallePedido d) throws SQLException {
        if (d.getId() == null) {
            String sql = "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad, precio_unitario, subtotal) " +
                    "VALUES (?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, d.getPedidoId());
                stmt.setLong(2, d.getProductoId());
                stmt.setInt(3, d.getCantidad());
                stmt.setBigDecimal(4, d.getPrecioUnitario());
                stmt.setBigDecimal(5, d.getSubtotal());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        d.setId(rs.getLong(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE detalle_pedido SET pedido_id = ?, producto_id = ?, cantidad = ?, precio_unitario = ?, subtotal = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, d.getPedidoId());
                stmt.setLong(2, d.getProductoId());
                stmt.setInt(3, d.getCantidad());
                stmt.setBigDecimal(4, d.getPrecioUnitario());
                stmt.setBigDecimal(5, d.getSubtotal());
                stmt.setLong(6, d.getId());
                stmt.executeUpdate();
            }
        }
        return d;
    }

    /**
     * Guarda una lista de detalles en lote utilizando una conexión transaccional.
     */
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

    /**
     * Consulta todos los detalles asociados a un ID de pedido específico.
     */
    public List<DetallePedido> findByPedidoId(long pedidoId) throws SQLException {
        List<DetallePedido> detalles = new ArrayList<>();
        String sql = "SELECT id, pedido_id, producto_id, cantidad, precio_unitario, subtotal " +
                "FROM detalle_pedido WHERE pedido_id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pedidoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DetallePedido d = new DetallePedido();
                    d.setId(rs.getLong("id"));
                    d.setPedidoId(rs.getLong("pedido_id"));
                    d.setProductoId(rs.getLong("producto_id"));
                    d.setCantidad(rs.getInt("cantidad")); // Mapeo como int
                    d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    d.setSubtotal(rs.getBigDecimal("subtotal"));
                    detalles.add(d);
                }
            }
        }
        return detalles;
    }

    /**
     * Gestiona el almacenamiento de los adicionales asociados al detalle del pedido.
     */
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
                stmt.setInt(3, d.getCantidad() != null ? d.getCantidad() : 1);
                stmt.setLong(4, d.getId());
                stmt.executeUpdate();
            }
        }
        return d;
    }
}