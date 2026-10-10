package repository;

import ConexionBD.ConexionBD;
import enums.EstadoPedido;
import entity.Pedido;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public class PedidoRepository {

    public Optional<Pedido> findById(long id) throws SQLException {
        String sql = "SELECT pedido_id, numero_pedido, mesa_id, usuario_id, " + "productos, estado, fecha_pedido, subtotal, total, " + "inventario_descontado " + "FROM pedidos " + "WHERE pedido_id = ?";
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
        String sql = "SELECT 1 FROM pedidos " + "WHERE mesa_id = ? " + "AND estado <> ? " + "LIMIT 1";
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
            String sql = "INSERT INTO pedidos " +
                            "(numero_pedido, mesa_id, usuario_id, productos, " +
                            "estado, fecha_pedido, subtotal, total, " +
                            "inventario_descontado) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
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
                stmt.setBigDecimal(8, p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO);
                /*
                 * La base de datos utiliza:
                 * S = inventario descontado
                 * N = inventario no descontado
                 */
                stmt.setString(9, p.isInventarioDescontado() ? "S" : "N");
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        p.setId(rs.getLong(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE pedidos SET " +
                            "numero_pedido = ?, " +
                            "mesa_id = ?, " +
                            "usuario_id = ?, " +
                            "productos = ?, " +
                            "estado = ?, " +
                            "subtotal = ?, " +
                            "total = ?, " +
                            "inventario_descontado = ? " +
                            "WHERE pedido_id = ?";
            try (Connection conn = ConexionBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(
                        1,
                        p.getNumeroPedido()
                );

                stmt.setLong(
                        2,
                        p.getMesaId()
                );

                stmt.setLong(
                        3,
                        p.getUsuarioId()
                );

                stmt.setObject(
                        4,
                        crearJsonb(
                                p.getProductos() != null
                                        ? p.getProductos()
                                        : "[]"
                        )
                );

                stmt.setString(
                        5,
                        p.getEstado() != null
                                ? p.getEstado().name()
                                : EstadoPedido.PENDIENTE.name()
                );

                stmt.setBigDecimal(
                        6,
                        p.getSubtotal() != null
                                ? p.getSubtotal()
                                : BigDecimal.ZERO
                );

                stmt.setBigDecimal(
                        7,
                        p.getTotal() != null
                                ? p.getTotal()
                                : BigDecimal.ZERO
                );

                /*
                 * La base de datos utiliza:
                 * S = inventario descontado
                 * N = inventario no descontado
                 */
                stmt.setString(
                        8,
                        p.isInventarioDescontado()
                                ? "S"
                                : "N"
                );

                stmt.setLong(
                        9,
                        p.getId()
                );

                stmt.executeUpdate();
            }
        }

        return p;
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
                        "productos, estado, fecha_pedido, subtotal, total, " +
                        "inventario_descontado " +
                        "FROM pedidos " +
                        "WHERE mesa_id = ? " +
                        "AND estado <> ? " +
                        "ORDER BY pedido_id DESC " +
                        "LIMIT 1";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setLong(
                    1,
                    mesaId
            );

            stmt.setString(
                    2,
                    EstadoPedido.CANCELADO.name()
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

        String sql = "SELECT pedido_id, numero_pedido, mesa_id, usuario_id, productos, estado, fecha_pedido " +
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
