package repository;

import database.ConexionBD;
import entity.AlertaInventario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlertaInventarioRepository {

    /**
     * Obtiene todas las alertas pendientes.
     */
    public List<AlertaInventario> findPendientes()
            throws SQLException {

        String sql = """
                SELECT id,
                       ingrediente_id,
                       producto_id,
                       stock_actual,
                       stock_minimo,
                       cantidad_sugerida,
                       estado,
                       created_at,
                       resolved_at
                FROM alerta_inventario
                WHERE estado = 'PENDIENTE'
                ORDER BY created_at DESC
                """;

        List<AlertaInventario> alertas = new ArrayList<>();

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                alertas.add(map(rs));
            }
        }

        return alertas;
    }

    /**
     * Busca una alerta pendiente asociada a un ingrediente.
     */
    public Optional<AlertaInventario> findPendienteByIngredienteId(
            long ingredienteId
    ) throws SQLException {

        String sql = """
                SELECT id,
                       ingrediente_id,
                       producto_id,
                       stock_actual,
                       stock_minimo,
                       cantidad_sugerida,
                       estado,
                       created_at,
                       resolved_at
                FROM alerta_inventario
                WHERE ingrediente_id = ?
                  AND estado = 'PENDIENTE'
                ORDER BY created_at DESC
                LIMIT 1
                """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, ingredienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Cuenta las alertas pendientes.
     */
    public long countPendientes()
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM alerta_inventario
                WHERE estado = 'PENDIENTE'
                """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        }

        return 0;
    }

    /**
     * Guarda una nueva alerta.
     */
    public AlertaInventario save(
            AlertaInventario alerta
    ) throws SQLException {

        String sql = """
                INSERT INTO alerta_inventario (
                    ingrediente_id,
                    producto_id,
                    stock_actual,
                    stock_minimo,
                    cantidad_sugerida,
                    estado
                )
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id, created_at
                """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            if (alerta.getIngredienteId() != null) {
                stmt.setLong(1, alerta.getIngredienteId());
            } else {
                stmt.setNull(
                        1,
                        java.sql.Types.BIGINT
                );
            }

            if (alerta.getProductoId() != null) {
                stmt.setLong(2, alerta.getProductoId());
            } else {
                stmt.setNull(
                        2,
                        java.sql.Types.BIGINT
                );
            }

            stmt.setBigDecimal(
                    3,
                    alerta.getStockActual()
            );

            stmt.setBigDecimal(
                    4,
                    alerta.getStockMinimo()
            );

            stmt.setBigDecimal(
                    5,
                    alerta.getCantidadSugerida()
            );

            stmt.setString(
                    6,
                    alerta.getEstado() != null
                            ? alerta.getEstado()
                            : "PENDIENTE"
            );

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    alerta.setId(rs.getLong("id"));
                    alerta.setCreatedAt(
                            rs.getTimestamp("created_at")
                                    .toLocalDateTime()
                    );
                }
            }
        }

        return alerta;
    }

    /**
     * Marca una alerta como resuelta.
     */
    public void resolver(long alertaId)
            throws SQLException {

        String sql = """
                UPDATE alerta_inventario
                SET estado = 'RESUELTA',
                    resolved_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setLong(1, alertaId);
            stmt.executeUpdate();
        }
    }

    private AlertaInventario map(ResultSet rs)
            throws SQLException {

        AlertaInventario alerta =
                new AlertaInventario();

        alerta.setId(
                rs.getLong("id")
        );

        long ingredienteId =
                rs.getLong("ingrediente_id");

        if (!rs.wasNull()) {
            alerta.setIngredienteId(ingredienteId);
        }

        long productoId =
                rs.getLong("producto_id");

        if (!rs.wasNull()) {
            alerta.setProductoId(productoId);
        }

        alerta.setStockActual(
                rs.getBigDecimal("stock_actual")
        );

        alerta.setStockMinimo(
                rs.getBigDecimal("stock_minimo")
        );

        alerta.setCantidadSugerida(
                rs.getBigDecimal("cantidad_sugerida")
        );

        alerta.setEstado(
                rs.getString("estado")
        );

        if (rs.getTimestamp("created_at") != null) {
            alerta.setCreatedAt(
                    rs.getTimestamp("created_at")
                            .toLocalDateTime()
            );
        }

        if (rs.getTimestamp("resolved_at") != null) {
            alerta.setResolvedAt(
                    rs.getTimestamp("resolved_at")
                            .toLocalDateTime()
            );
        }

        return alerta;
    }
}