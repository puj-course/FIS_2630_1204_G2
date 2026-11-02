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

    // Selección base uniendo con la tabla estados_mesa / estado_pedido según corresponda para obtener el código o ID del estado
    private static final String SELECT_BASE =
            "SELECT p.id_pedido, p.id_cliente, p.id_mesa, p.id_mesero, " +
                    "       p.id_estado_pedido, ep.codigo_estado AS estado_str, " +
                    "       p.fecha_creacion, p.observaciones " +
                    "FROM pedido p " +
                    "JOIN estado_pedido ep ON p.id_estado_pedido = ep.id_estado_pedido ";

    public Optional<Pedido> findById(long id) throws SQLException {
        String sql = SELECT_BASE + "WHERE p.id_pedido = ?";
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
        // Se valida consultando el código del estado en la tabla relacionada
        String sql =
                "SELECT 1 FROM pedido p " +
                        "JOIN estado_pedido ep ON p.id_estado_pedido = ep.id_estado_pedido " +
                        "WHERE p.id_mesa = ? " +
                        "AND ep.codigo_estado <> ? " +
                        "AND ep.codigo_estado NOT IN (?, ?, ?) " +
                        "LIMIT 1";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, mesaId);
            stmt.setString(2, estadoExcluido.name());
            stmt.setString(3, EstadoPedido.COMPLETADO.name());
            stmt.setString(4, EstadoPedido.ENTREGADO.name());
            stmt.setString(5, EstadoPedido.CANCELADO.name());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    //Para operaciones de pedido en estilo de transaccion
    public Optional<Pedido> findActivoByMesaIdWithConn(Connection conn, long mesaId) throws SQLException {
        String sql = SELECT_BASE +
                "WHERE p.id_mesa = ? " +
                "AND ep.codigo_estado NOT IN ('CANCELADO', 'ENTREGADO', 'COMPLETADO', 'PAGADO') " +
                "ORDER BY p.id_pedido DESC " +
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
            String sql = "INSERT INTO pedido " +
                    "(id_cliente, id_mesa, id_mesero, id_estado_pedido, fecha_creacion, observaciones) " +
                    "VALUES (?, ?, ?, (SELECT id_estado_pedido FROM estado_pedido WHERE UPPER(codigo_estado) = UPPER(?)), ?, ?) " +
                    "RETURNING id_pedido";

            try (Connection conn = ConexionBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                // 1. id_cliente (si tu entidad lo maneja, de lo contrario se envía null)
                if (p.getClienteId() != null) {
                    stmt.setLong(1, p.getClienteId());
                } else {
                    stmt.setNull(1, java.sql.Types.INTEGER);
                }

                stmt.setLong(2, p.getMesaId());
                stmt.setLong(3, p.getUsuarioId());
                String estadoStr = p.getEstado() != null ? p.getEstado().name() : EstadoPedido.PENDIENTE.name();
                stmt.setString(4, estadoStr);
                stmt.setTimestamp(5, Timestamp.valueOf(p.getFechaPedido() != null ? p.getFechaPedido() : java.time.LocalDateTime.now()));
                stmt.setString(6, p.getObservaciones() != null ? p.getObservaciones() : "");

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        p.setId(rs.getLong(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE pedido SET "
                    + "id_cliente = ?, "
                    + "id_mesa = ?, "
                    + "id_mesero = ?, "
                    + "id_estado_pedido = (SELECT id_estado_pedido FROM estado_pedido WHERE UPPER(codigo_estado) = UPPER(?)), "
                    + "observaciones = ? "
                    + "WHERE id_pedido = ?";

            try (Connection conn = ConexionBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                if (p.getClienteId() != null) {
                    stmt.setLong(1, p.getClienteId());
                } else {
                    stmt.setNull(1, java.sql.Types.INTEGER);
                }

                stmt.setLong(2, p.getMesaId());
                stmt.setLong(3, p.getUsuarioId());
                String estadoStr = p.getEstado() != null ? p.getEstado().name() : EstadoPedido.PENDIENTE.name();
                stmt.setString(4, estadoStr);
                stmt.setString(5, p.getObservaciones() != null ? p.getObservaciones() : "");
                stmt.setLong(6, p.getId());

                stmt.executeUpdate();
            }
        }
        return p;
    }

    // Para operaciones de pedido en estilo de transacción
    public Pedido saveWithConn(Connection conn, Pedido p) throws SQLException {
        if (p.getId() == null) {
            String sql = "INSERT INTO pedido " +
                    "(id_cliente, id_mesa, id_mesero, id_estado_pedido, fecha_creacion, observaciones) " +
                    "VALUES (?, ?, ?, (SELECT id_estado_pedido FROM estado_pedido WHERE UPPER(codigo_estado) = UPPER(?)), ?, ?) " +
                    "RETURNING id_pedido";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // 1. id_cliente (si la entidad lo maneja, de lo contrario se envía null)
                if (p.getClienteId() != null) {
                    stmt.setLong(1, p.getClienteId());
                } else {
                    stmt.setNull(1, java.sql.Types.INTEGER);
                }

                stmt.setLong(2, p.getMesaId());
                stmt.setLong(3, p.getUsuarioId());
                String estadoStr = p.getEstado() != null ? p.getEstado().name() : EstadoPedido.PENDIENTE.name();
                stmt.setString(4, estadoStr);
                stmt.setTimestamp(5, Timestamp.valueOf(p.getFechaPedido() != null ? p.getFechaPedido() : java.time.LocalDateTime.now()));
                stmt.setString(6, p.getObservaciones() != null ? p.getObservaciones() : "");

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        p.setId(rs.getLong(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE pedido SET "
                    + "id_cliente = ?, "
                    + "id_mesa = ?, "
                    + "id_mesero = ?, "
                    + "id_estado_pedido = (SELECT id_estado_pedido FROM estado_pedido WHERE UPPER(codigo_estado) = UPPER(?)), "
                    + "observaciones = ? "
                    + "WHERE id_pedido = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (p.getClienteId() != null) {
                    stmt.setLong(1, p.getClienteId());
                } else {
                    stmt.setNull(1, java.sql.Types.INTEGER);
                }

                stmt.setLong(2, p.getMesaId());
                stmt.setLong(3, p.getUsuarioId());
                String estadoStr = p.getEstado() != null ? p.getEstado().name() : EstadoPedido.PENDIENTE.name();
                stmt.setString(4, estadoStr);
                stmt.setString(5, p.getObservaciones() != null ? p.getObservaciones() : "");
                stmt.setLong(6, p.getId());
                stmt.executeUpdate();
            }
        }
        return p;
    }

    public void actualizarEstado(long pedidoId, EstadoPedido nuevoEstado) throws SQLException {
        // Actualiza el id_estado_pedido buscando primero su ID a partir del enum/código,
        // blindando para que no actualice si ya está cancelado o cerrado.
        String sql = "UPDATE pedido SET id_estado_pedido = (" +
                "    SELECT id_estado_pedido FROM estado_pedido WHERE UPPER(codigo_estado) = UPPER(?)" +
                ") " +
                "WHERE id_pedido = ? " +
                "AND id_estado_pedido NOT IN (" +
                "    SELECT id_estado_pedido FROM estado_pedido WHERE UPPER(codigo_estado) IN ('CANCELADO', 'ENTREGADO', 'COMPLETADO', 'PAGADO')" +
                ")";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado.name());
            stmt.setLong(2, pedidoId);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas == 0) {
                throw new PedidoNoEditableException(
                        "No se pudo actualizar el estado del pedido #" + pedidoId +
                                ": el pedido ya se encuentra cancelado o en un estado final."
                );
            }
        }
    }

    /**
     * Actualiza productos e importes SIN pisar propina ni descuento:
     * el total lo calcula la base con los valores que ya tiene guardados.
     * Si la cuenta ya fue pagada, no modifica nada y lanza excepción.
     */
    public void actualizarImportes(long pedidoId, String productosJson,
                                   BigDecimal subtotal, BigDecimal impuestos) throws SQLException {
        String sql = "UPDATE pedido SET producto = ?, subtotal = ?, impuestos = ?, "
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

    private Pedido map(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setId(rs.getLong("id_pedido"));
        p.setMesaId(rs.getLong("id_mesa"));
        p.setUsuarioId(rs.getLong("id_mesero")); // Mapeo de id_mesero al campo de usuario en la entidad

        String estadoStr = rs.getString("estado_str");
        if (estadoStr != null) {
            p.setEstado(EstadoPedido.valueOf(estadoStr.toUpperCase()));
        }

        Timestamp ts = rs.getTimestamp("fecha_creacion");
        if (ts != null) {
            p.setFechaPedido(ts.toLocalDateTime());
        }

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
