package repository;

import ConexionBD.ConexionBD;
import enums.EstadoPedido;
import entity.Pedido;
import entity.DetallePedido;
import exceptions.PedidoNoEditableException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

public class PedidoRepository {

    public Optional<Pedido> findById(long id) throws SQLException {
        String sql = "SELECT pedido_id, numero_pedido, cliente_id, mesa_id, usuario_id, " +
                "estado, fecha_pedido, subtotal, impuestos, total, " +
                "inventario_descontado " +
                "FROM pedidos " +
                "WHERE pedido_id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Pedido pedido = map(rs);
                    // Cargamos los detalles relacionales asociados
                    pedido.setDetalles(obtenerDetallesPorPedidoId(conn, id));
                    return Optional.of(pedido);
                }
            }
        }
        return Optional.empty();
    }

    public boolean existsByMesaIdAndEstadoNot(long mesaId, EstadoPedido estadoExcluido) throws SQLException {
        String sql =
                "SELECT 1 FROM pedidos " +
                        "WHERE mesa_id = ? " +
                        "AND estado <> ? " +
                        "AND estado NOT IN (?, ?) " +
                        "LIMIT 1";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, mesaId);
            stmt.setString(2, estadoExcluido.name());
            stmt.setString(3, EstadoPedido.COMPLETADO.name());
            stmt.setString(4, EstadoPedido.ENTREGADO.name());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Optional<Pedido> findActivoByMesaIdWithConn(Connection conn, long mesaId) throws SQLException {
        String sql = "SELECT pedido_id, numero_pedido, cliente_id, mesa_id, usuario_id, " +
                "estado, fecha_pedido, subtotal, impuestos, total, " +
                "inventario_descontado " +
                "FROM pedidos " +
                "WHERE mesa_id = ? " +
                "AND estado NOT IN ('CANCELADO', 'ENTREGADO', 'COMPLETADO', 'PAGADO') " +
                "ORDER BY pedido_id DESC " +
                "LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, mesaId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Pedido pedido = map(rs);
                    pedido.setDetalles(obtenerDetallesPorPedidoId(conn, pedido.getId()));
                    return Optional.of(pedido);
                }
            }
        }
        return Optional.empty();
    }

    public Pedido save(Pedido p) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Pedido guardado = saveWithConn(conn, p);
                conn.commit();
                return guardado;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public Pedido saveWithConn(Connection conn, Pedido p) throws SQLException {
        if (p.getId() == null) {
            String sql = "INSERT INTO pedidos " +
                    "(numero_pedido, cliente_id, mesa_id, usuario_id, " +
                    "estado, fecha_pedido, subtotal, impuestos, total, " +
                    "inventario_descontado) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "RETURNING pedido_id";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, p.getNumeroPedido());
                if (p.getClienteId() != null) {
                    stmt.setLong(2, p.getClienteId());
                } else {
                    stmt.setNull(2, java.sql.Types.BIGINT);
                }
                stmt.setLong(3, p.getMesaId());
                stmt.setLong(4, p.getUsuarioId());
                stmt.setString(5, p.getEstado() != null ? p.getEstado().name() : EstadoPedido.PENDIENTE.name());
                stmt.setTimestamp(6, Timestamp.valueOf(p.getFechaPedido() != null ? p.getFechaPedido() : java.time.LocalDateTime.now()));
                stmt.setBigDecimal(7, p.getSubtotal() != null ? p.getSubtotal() : BigDecimal.ZERO);
                stmt.setBigDecimal(8, p.getImpuestos() != null ? p.getImpuestos() : BigDecimal.ZERO);
                stmt.setBigDecimal(9, p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO);
                stmt.setString(10, p.isInventarioDescontado() ? "S" : "N");

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        p.setId(rs.getLong(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE pedidos SET "
                    + "numero_pedido = ?, "
                    + "cliente_id = ?, "
                    + "mesa_id = ?, "
                    + "usuario_id = ?, "
                    + "estado = ?, "
                    + "subtotal = ?, "
                    + "impuestos = ?, "
                    + "total = ?, "
                    + "inventario_descontado = ?, "
                    + "fecha_actualizacion = CURRENT_TIMESTAMP "
                    + "WHERE pedido_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, p.getNumeroPedido());
                if (p.getClienteId() != null) {
                    stmt.setLong(2, p.getClienteId());
                } else {
                    stmt.setNull(2, java.sql.Types.BIGINT);
                }
                stmt.setLong(3, p.getMesaId());
                stmt.setLong(4, p.getUsuarioId());
                stmt.setString(5, p.getEstado() != null ? p.getEstado().name() : EstadoPedido.PENDIENTE.name());
                stmt.setBigDecimal(6, p.getSubtotal() != null ? p.getSubtotal() : BigDecimal.ZERO);
                stmt.setBigDecimal(7, p.getImpuestos() != null ? p.getImpuestos() : BigDecimal.ZERO);
                stmt.setBigDecimal(8, p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO);
                stmt.setString(9, p.isInventarioDescontado() ? "S" : "N");
                stmt.setLong(10, p.getId());

                stmt.executeUpdate();
            }
        }
        return p;
    }

    public void actualizarEstado(long pedidoId, EstadoPedido estado) throws SQLException {
        // Blindaje añadido: Evita reactivar pedidos que ya estén en estados cerrados/cancelados
        String sql = "UPDATE pedidos SET estado = ?, fecha_actualizacion = CURRENT_TIMESTAMP "
                + "WHERE pedido_id = ? "
                + "AND estado NOT IN ('CANCELADO', 'ENTREGADO', 'COMPLETADO', 'PAGADO')";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado.name());
            stmt.setLong(2, pedidoId);
            int filas = stmt.executeUpdate();
            if (filas == 0) {
                throw new PedidoNoEditableException(
                        "No se pudo actualizar el estado: el pedido #" + pedidoId + " ya se encuentra en un estado cerrado o cancelado."
                );
            }
        }
    }

    public void actualizarImportes(long pedidoId, BigDecimal subtotal, BigDecimal impuestos) throws SQLException {
        String sql = "UPDATE pedidos SET subtotal = ?, impuestos = ?, "
                + "total = ? + ? + propina - descuento, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP "
                + "WHERE pedido_id = ? "
                + "AND NOT EXISTS (SELECT 1 FROM pagos WHERE pedido_id = ? AND estado = 'PAGADO')";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, subtotal);
            stmt.setBigDecimal(2, impuestos);
            stmt.setBigDecimal(3, subtotal);
            stmt.setBigDecimal(4, impuestos);
            stmt.setLong(5, pedidoId);
            stmt.setLong(6, pedidoId);
            if (stmt.executeUpdate() == 0) {
                throw new PedidoNoEditableException(
                        "La cuenta ya fue pagada o el pedido no existe; no se puede modificar.");
            }
        }
    }

    public void marcarInventarioDescontado(long pedidoId) throws SQLException {
        String sql = "UPDATE pedidos SET inventario_descontado = 'S', "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE pedido_id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pedidoId);
            stmt.executeUpdate();
        }
    }

    public Optional<Pedido> findActivoByMesaId(long mesaId) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            return findActivoByMesaIdWithConn(conn, mesaId);
        }
    }

    public List<Pedido> findPedidosParaCocina() throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        String sql = "SELECT pedido_id, numero_pedido, cliente_id, mesa_id, usuario_id, estado, fecha_pedido, " +
                "subtotal, impuestos, total, inventario_descontado " +
                "FROM pedidos WHERE estado IN (?, ?, ?, ?) ORDER BY fecha_pedido";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, EstadoPedido.PENDIENTE.name());
            stmt.setString(2, EstadoPedido.ASIGNADO_MESA.name());
            stmt.setString(3, EstadoPedido.EN_PREPARACION.name());
            stmt.setString(4, EstadoPedido.COMPLETADO.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Pedido p = map(rs);
                    p.setDetalles(obtenerDetallesPorPedidoId(conn, p.getId()));
                    pedidos.add(p);
                }
            }
        }
        return pedidos;
    }

    private List<DetallePedido> obtenerDetallesPorPedidoId(Connection conn, long pedidoId) throws SQLException {
        List<DetallePedido> detalles = new ArrayList<>();
        // Ajusta los nombres de columnas según tu tabla detalle_pedido final
        String sql = "SELECT id, pedido_id, producto_id, cantidad, precio_unitario, subtotal " +
                "FROM detalle_pedido WHERE pedido_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pedidoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DetallePedido det = new DetallePedido();
                    // Configura los atributos de DetallePedido según tu entidad
                    det.setId(rs.getLong("id"));
                    det.setPedidoId(rs.getLong("pedido_id"));
                    det.setProductoId(rs.getLong("producto_id"));
                    det.setCantidad(rs.getInt("cantidad"));
                    det.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    det.setSubtotal(rs.getBigDecimal("subtotal"));
                    detalles.add(det);
                }
            }
        }
        return detalles;
    }

    private Pedido map(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setId(rs.getLong("pedido_id"));
        p.setNumeroPedido(rs.getString("numero_pedido"));

        long clienteId = rs.getLong("cliente_id");
        if (!rs.wasNull()) {
            p.setClienteId(clienteId);
        }

        p.setMesaId(rs.getLong("mesa_id"));
        p.setUsuarioId(rs.getLong("usuario_id"));

        p.setEstado(EstadoPedido.valueOf(rs.getString("estado")));

        Timestamp ts = rs.getTimestamp("fecha_pedido");
        if (ts != null) {
            p.setFechaPedido(ts.toLocalDateTime());
        }

        BigDecimal subtotal = rs.getBigDecimal("subtotal");
        if (subtotal != null) {
            p.setSubtotal(subtotal);
        }

        BigDecimal impuestos = rs.getBigDecimal("impuestos");
        if (impuestos != null) {
            p.setImpuestos(impuestos);
        }

        BigDecimal total = rs.getBigDecimal("total");
        if (total != null) {
            p.setTotal(total);
        }

        String inventarioDescontado = rs.getString("inventario_descontado");
        p.setInventarioDescontado(esVerdadero(inventarioDescontado));

        return p;
    }

    private boolean esVerdadero(String valor) {
        if (valor == null) {
            return false;
        }
        return valor.trim().toUpperCase().equals("S");
    }
}