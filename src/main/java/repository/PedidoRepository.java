package repository;

import ConexionDB.ConexionBD;
import entity.EstadoPedido;
import entity.Pedido;

import java.sql.*;
import java.util.Optional;

public class PedidoRepository {

    public Optional<Pedido> findById(long id) throws SQLException {
        String sql = "SELECT pedido_id, numero_pedido, mesa_id, usuario_id, productos, estado, fecha_pedido " +
                "FROM pedidos WHERE pedido_id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    /** true si la mesa tiene algún pedido cuyo estado NO es el indicado (p. ej. CANCELADO). */
    public boolean existsByMesaIdAndEstadoNot(long mesaId, EstadoPedido estadoExcluido) throws SQLException {
        String sql = "SELECT 1 FROM pedidos WHERE mesa_id = ? AND estado <> ? LIMIT 1";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, mesaId);
            stmt.setString(2, estadoExcluido.name());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Pedido save(Pedido p) throws SQLException {
        if (p.getId() == null) {
            String sql = "INSERT INTO pedidos (numero_pedido, mesa_id, usuario_id, productos, estado, fecha_pedido) " +
                    "VALUES (?, ?, ?, ?, ?, ?) RETURNING pedido_id";
            try (Connection conn = ConexionBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, p.getNumeroPedido());
                stmt.setLong(2, p.getMesaId());
                stmt.setLong(3, p.getUsuarioId());
                stmt.setString(4, p.getProductos() != null ? p.getProductos() : "[]");
                stmt.setString(5, p.getEstado() != null ? p.getEstado().name() : EstadoPedido.PENDIENTE.name());
                stmt.setTimestamp(6, Timestamp.valueOf(
                        p.getFechaPedido() != null ? p.getFechaPedido() : java.time.LocalDateTime.now()));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        p.setId(rs.getLong(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE pedidos SET numero_pedido = ?, mesa_id = ?, usuario_id = ?, productos = ?, estado = ? " +
                    "WHERE pedido_id = ?";
            try (Connection conn = ConexionBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, p.getNumeroPedido());
                stmt.setLong(2, p.getMesaId());
                stmt.setLong(3, p.getUsuarioId());
                stmt.setString(4, p.getProductos());
                stmt.setString(5, p.getEstado().name());
                stmt.setLong(6, p.getId());
                stmt.executeUpdate();
            }
        }
        return p;
    }

    private Pedido map(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setId(rs.getLong("pedido_id"));
        p.setNumeroPedido(rs.getString("numero_pedido"));
        p.setMesaId(rs.getLong("mesa_id"));
        p.setUsuarioId(rs.getLong("usuario_id"));
        p.setProductos(rs.getString("productos"));
        p.setEstado(EstadoPedido.valueOf(rs.getString("estado")));
        Timestamp ts = rs.getTimestamp("fecha_pedido");
        if (ts != null) {
            p.setFechaPedido(ts.toLocalDateTime());
        }
        return p;
    }
}