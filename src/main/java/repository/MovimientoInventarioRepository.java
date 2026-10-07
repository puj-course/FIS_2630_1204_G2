package repository;

import database.ConexionBD;
import entity.MovimientoInventario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MovimientoInventarioRepository {

    /**
     * Obtiene todos los movimientos de un ingrediente.
     */
    public List<MovimientoInventario> findByIngredienteId(
            long ingredienteId
    ) throws SQLException {

        String sql = """
                SELECT id_movimiento,
                       producto_id,
                       ingrediente_id,
                       tipo_movimiento,
                       cantidad,
                       motivo,
                       usuario_id,
                       fecha_movimiento
                FROM movimientos_inventario
                WHERE ingrediente_id = ?
                ORDER BY fecha_movimiento DESC
                """;

        List<MovimientoInventario> movimientos =
                new ArrayList<>();

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, ingredienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(map(rs));
                }
            }
        }

        return movimientos;
    }

    /**
     * Obtiene movimientos asociados a un producto.
     */
    public List<MovimientoInventario> findByProductoId(
            long productoId
    ) throws SQLException {

        String sql = """
                SELECT id_movimiento,
                       producto_id,
                       ingrediente_id,
                       tipo_movimiento,
                       cantidad,
                       motivo,
                       usuario_id,
                       fecha_movimiento
                FROM movimientos_inventario
                WHERE producto_id = ?
                ORDER BY fecha_movimiento DESC
                """;

        List<MovimientoInventario> movimientos =
                new ArrayList<>();

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, productoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(map(rs));
                }
            }
        }

        return movimientos;
    }

    /**
     * Obtiene los últimos movimientos del inventario.
     */
    public List<MovimientoInventario> findUltimos(
            int limite
    ) throws SQLException {

        String sql = """
                SELECT id_movimiento,
                       producto_id,
                       ingrediente_id,
                       tipo_movimiento,
                       cantidad,
                       motivo,
                       usuario_id,
                       fecha_movimiento
                FROM movimientos_inventario
                ORDER BY fecha_movimiento DESC
                LIMIT ?
                """;

        List<MovimientoInventario> movimientos =
                new ArrayList<>();

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limite);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(map(rs));
                }
            }
        }

        return movimientos;
    }

    /**
     * Registra un movimiento de inventario.
     *
     * El movimiento debe corresponder a un ingrediente
     * o a un producto, pero no a ambos.
     */
    public long save(
            MovimientoInventario movimiento
    ) throws SQLException {

        String sql = """
                INSERT INTO movimientos_inventario (
                    producto_id,
                    ingrediente_id,
                    tipo_movimiento,
                    cantidad,
                    motivo,
                    usuario_id
                )
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id_movimiento
                """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            if (movimiento.getProductoId() != null) {
                stmt.setLong(
                        1,
                        movimiento.getProductoId()
                );
            } else {
                stmt.setNull(
                        1,
                        java.sql.Types.BIGINT
                );
            }

            if (movimiento.getIngredienteId() != null) {
                stmt.setLong(
                        2,
                        movimiento.getIngredienteId()
                );
            } else {
                stmt.setNull(
                        2,
                        java.sql.Types.BIGINT
                );
            }

            stmt.setString(
                    3,
                    movimiento.getTipoMovimiento()
            );

            stmt.setBigDecimal(
                    4,
                    movimiento.getCantidad()
            );

            stmt.setString(
                    5,
                    movimiento.getMotivo()
            );

            if (movimiento.getUsuarioId() != null) {
                stmt.setLong(
                        6,
                        movimiento.getUsuarioId()
                );
            } else {
                stmt.setNull(
                        6,
                        java.sql.Types.BIGINT
                );
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong(
                            "id_movimiento"
                    );

                    movimiento.setIdMovimiento(id);

                    return id;
                }
            }
        }

        return 0;
    }

    private MovimientoInventario map(
            ResultSet rs
    ) throws SQLException {

        MovimientoInventario movimiento =
                new MovimientoInventario();

        movimiento.setIdMovimiento(
                rs.getLong("id_movimiento")
        );

        long productoId =
                rs.getLong("producto_id");

        if (!rs.wasNull()) {
            movimiento.setProductoId(productoId);
        }

        long ingredienteId =
                rs.getLong("ingrediente_id");

        if (!rs.wasNull()) {
            movimiento.setIngredienteId(ingredienteId);
        }

        movimiento.setTipoMovimiento(
                rs.getString("tipo_movimiento")
        );

        movimiento.setCantidad(
                rs.getBigDecimal("cantidad")
        );

        movimiento.setMotivo(
                rs.getString("motivo")
        );

        movimiento.setUsuarioId(
                rs.getLong("usuario_id")
        );

        if (rs.wasNull()) {
            movimiento.setUsuarioId(null);
        }

        if (rs.getTimestamp("fecha_movimiento") != null) {
            movimiento.setFechaMovimiento(
                    rs.getTimestamp("fecha_movimiento")
                            .toLocalDateTime()
            );
        }

        return movimiento;
    }
}