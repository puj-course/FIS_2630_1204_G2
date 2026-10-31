package repository;

import ConexionBD.ConexionBD;
import enums.EstadoPedido;
import entity.Pedido;
import org.postgresql.util.PGobject;
import exceptions.PedidoNoEditableException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public class PedidoRepository {

    public Optional<Pedido> findById(long id) throws SQLException {
        String sql = "SELECT pedido_id, numero_pedido, mesa_id, usuario_id, " + "productos, estado, fecha_pedido, subtotal, impuestos, total, " + "inventario_descontado " + "FROM pedidos " + "WHERE pedido_id = ?";
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

    //Para operaciones de pedido en estilo de transaccion
    public Optional<Pedido> findActivoByMesaIdWithConn(Connection conn, long mesaId) throws SQLException {
        String sql = "SELECT pedido_id, numero_pedido, mesa_id, usuario_id, " +
                "productos, estado, fecha_pedido, subtotal, impuestos, total, " +
                "inventario_descontado " +
                "FROM pedidos " +
                "WHERE mesa_id = ? " +
                "AND estado NOT IN ('CANCELADO', 'ENTREGADO') " +
                "ORDER BY pedido_id DESC " +
                "LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, mesaId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Pedido save(Pedido p) throws SQLException {
        if (p.getId() == null) {
            String sql = "INSERT INTO pedidos " +
                            "(numero_pedido, mesa_id, usuario_id, productos, " +
                            "estado, fecha_pedido, subtotal, impuestos, total, " +
                            "inventario_descontado) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "RETURNING pedido_id";
            try (Connection conn = ConexionBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, p.getNumeroPedido());
                stmt.setLong(2, p.getMesaId());
                stmt.setLong(3, p.getUsuarioId());
                stmt.setObject(4, crearJsonb(p.getProductos() != null ? p.getProductos() : "[]"));
                stmt.setString(5, p.getEstado() != null ? p.getEstado().name() : EstadoPedido.PENDIENTE.name());
                stmt.setTimestamp(6, Timestamp.valueOf(p.getFechaPedido() != null ? p.getFechaPedido() : java.time.LocalDateTime.now()));
                stmt.setBigDecimal(7, p.getSubtotal() != null ? p.getSubtotal() : BigDecimal.ZERO);
                stmt.setBigDecimal(8, p.getImpuestos() != null ? p.getImpuestos() : BigDecimal.ZERO);
                stmt.setBigDecimal(9, p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO);
                /*
                 * La base de datos utiliza:
                 * S = inventario descontado
                 * N = inventario no descontado
                 */
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
                + "mesa_id = ?, "
                + "usuario_id = ?, "
                + "productos = ?, "
                + "estado = ?, "
                + "subtotal = ?, "
                + "impuestos = ?, "
                + "total = ?, "
                + "inventario_descontado = ? "
                + "WHERE pedido_id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, p.getNumeroPedido());
            stmt.setLong(2, p.getMesaId());
            stmt.setLong(3, p.getUsuarioId());
            stmt.setObject(4, crearJsonb(
                    p.getProductos() != null ? p.getProductos() : "[]"));
            stmt.setString(5, p.getEstado() != null
                    ? p.getEstado().name()
                    : EstadoPedido.PENDIENTE.name());
            stmt.setBigDecimal(6, p.getSubtotal() != null
                    ? p.getSubtotal() : BigDecimal.ZERO);
            stmt.setBigDecimal(7, p.getImpuestos() != null
                    ? p.getImpuestos() : BigDecimal.ZERO);
            stmt.setBigDecimal(8, p.getTotal() != null
                    ? p.getTotal() : BigDecimal.ZERO);
            stmt.setString(9, p.isInventarioDescontado() ? "S" : "N");
            stmt.setLong(10, p.getId());

            stmt.executeUpdate();
        }
    }

return p;
    }

    //Para operaciones de pedido en estilo de transaccion
    public Pedido saveWithConn(Connection conn, Pedido p) throws SQLException {
        if (p.getId() == null) {
            String sql = "INSERT INTO pedidos " +
                    "(numero_pedido, mesa_id, usuario_id, productos, " +
                    "estado, fecha_pedido, subtotal, impuestos, total, " +
                    "inventario_descontado) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "RETURNING pedido_id";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, p.getNumeroPedido());
                stmt.setLong(2, p.getMesaId());
                stmt.setLong(3, p.getUsuarioId());
                stmt.setObject(4, crearJsonb(p.getProductos() != null ? p.getProductos() : "[]"));
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
                    + "numero_pedido = ?, mesa_id = ?, usuario_id = ?, "
                    + "estado = ?, inventario_descontado = ? "
                    + "WHERE pedido_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, p.getNumeroPedido());
                stmt.setLong(2, p.getMesaId());
                stmt.setLong(3, p.getUsuarioId());
                stmt.setString(4, p.getEstado() != null
                        ? p.getEstado().name() : EstadoPedido.PENDIENTE.name());
                stmt.setString(5, p.isInventarioDescontado() ? "S" : "N");
                stmt.setLong(6, p.getId());
                stmt.executeUpdate();
            }
        }
        return p;
    }

public void actualizarEstado(long pedidoId, EstadoPedido estado) throws SQLException {
        String sql = "UPDATE pedidos SET estado = ?, fecha_actualizacion = CURRENT_TIMESTAMP "
                + "WHERE pedido_id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado.name());
            stmt.setLong(2, pedidoId);
            stmt.executeUpdate();
        }
    }

    /**
     * Actualiza productos e importes SIN pisar propina ni descuento:
     * el total lo calcula la base con los valores que ya tiene guardados.
     * Si la cuenta ya fue pagada, no modifica nada y lanza excepción.
     */
    public void actualizarImportes(long pedidoId, String productosJson,
                                   BigDecimal subtotal, BigDecimal impuestos) throws SQLException {
        String sql = "UPDATE pedidos SET productos = ?, subtotal = ?, impuestos = ?, "
                + "total = ? + ? + propina - descuento, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP "
                + "WHERE pedido_id = ? "
                + "AND NOT EXISTS (SELECT 1 FROM pagos WHERE pedido_id = ? AND estado = 'PAGADO')";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, crearJsonb(productosJson != null ? productosJson : "[]"));
            stmt.setBigDecimal(2, subtotal);
            stmt.setBigDecimal(3, impuestos);
            stmt.setBigDecimal(4, subtotal);
            stmt.setBigDecimal(5, impuestos);
            stmt.setLong(6, pedidoId);
            stmt.setLong(7, pedidoId);
            if (stmt.executeUpdate() == 0) {
                throw new PedidoNoEditableException(
                        "La cuenta ya fue pagada o el pedido no existe; no se puede modificar.");
            }
        }
    }

    /** Marca el inventario como descontado, sin tocar nada más. */
    public void marcarInventarioDescontado(long pedidoId) throws SQLException {
        String sql = "UPDATE pedidos SET inventario_descontado = 'S', "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE pedido_id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pedidoId);
            stmt.executeUpdate();
        }
    }

    private PGobject crearJsonb(
            String json
    ) throws SQLException {

        PGobject jsonObject =
                new PGobject();

        jsonObject.setType(
                "jsonb"
        );

        jsonObject.setValue(
                json
        );

        return jsonObject;
    }

    public Optional<Pedido> findActivoByMesaId(
            long mesaId
    ) throws SQLException {

        String sql =
                "SELECT pedido_id, numero_pedido, mesa_id, usuario_id, " +
                        "productos, estado, fecha_pedido, subtotal, impuestos, total, " +
                        "inventario_descontado " +
                        "FROM pedidos " +
                        "WHERE mesa_id = ? " +
                        "AND estado NOT IN ('CANCELADO', 'ENTREGADO') " +
                        "ORDER BY pedido_id DESC " +
                        "LIMIT 1";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setLong(
                    1,
                    mesaId
            );

            try (ResultSet rs =
                         stmt.executeQuery()) {

                if (rs.next()) {

                    return Optional.of(
                            map(rs)
                    );
                }
            }
        }

        return Optional.empty();
    }

    public java.util.List<Pedido> findPedidosParaCocina() throws SQLException {
        java.util.List<Pedido> pedidos = new java.util.ArrayList<>();

        String sql = "SELECT pedido_id, numero_pedido, mesa_id, usuario_id, productos, estado, fecha_pedido, " +
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
                    pedidos.add(map(rs));
                }
            }
        }
        return pedidos;
    }

    private Pedido map(
            ResultSet rs
    ) throws SQLException {

        Pedido p =
                new Pedido();

        p.setId(
                rs.getLong(
                        "pedido_id"
                )
        );

        p.setNumeroPedido(
                rs.getString(
                        "numero_pedido"
                )
        );

        p.setMesaId(
                rs.getLong(
                        "mesa_id"
                )
        );

        p.setUsuarioId(
                rs.getLong(
                        "usuario_id"
                )
        );

        p.setProductos(
                rs.getString(
                        "productos"
                )
        );

        p.setEstado(
                EstadoPedido.valueOf(
                        rs.getString(
                                "estado"
                        )
                )
        );

        Timestamp ts =
                rs.getTimestamp(
                        "fecha_pedido"
                );

        if (ts != null) {

            p.setFechaPedido(
                    ts.toLocalDateTime()
            );
        }

        BigDecimal subtotal =
                rs.getBigDecimal(
                        "subtotal"
                );

        if (subtotal != null) {

            p.setSubtotal(
                    subtotal
            );
        }

        BigDecimal impuestos =
                rs.getBigDecimal(
                        "impuestos"
                );
        if (impuestos != null) {
            p.setImpuestos(impuestos);
        }

        BigDecimal total =
                rs.getBigDecimal(
                        "total"
                );

        if (total != null) {

            p.setTotal(
                    total
            );
        }

        String inventarioDescontado =
                rs.getString(
                        "inventario_descontado"
                );

        p.setInventarioDescontado(
                esVerdadero(
                        inventarioDescontado
                )
        );

        return p;
    }

    /**
     * Convierte el valor de la base de datos
     * a boolean.
     *
     * S = true
     * N = false
     */
    private boolean esVerdadero(
            String valor
    ) {

        if (valor == null) {
            return false;
        }

        String normalizado =
                valor.trim()
                        .toUpperCase();

        return normalizado.equals("S");
    }


}
