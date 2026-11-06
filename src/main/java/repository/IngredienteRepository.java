package repository;

import ConexionBD.ConexionBD;
import entity.Ingrediente;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IngredienteRepository {

    private static final String SELECT_BASE =
            "SELECT i.ingrediente_id, i.nombre, i.descripcion, i.unidad_medida, " +
                    "i.costo_unitario, i.stock_actual, i.stock_minimo, i.stock_maximo, " +
                    "i.estado, i.fecha_creacion, i.fecha_actualizacion " +
                    "FROM ingredientes i ";

    public Optional<Ingrediente> findById(long id) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            return findById(conn, id);
        }
    }

    public Optional<Ingrediente> findById(Connection conn, long id) throws SQLException {
        String sql = SELECT_BASE + "WHERE i.ingrediente_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Ingrediente> findByIdForUpdate(Connection conn, long id)
            throws SQLException {

        String sql = SELECT_BASE + "WHERE i.ingrediente_id = ? FOR UPDATE OF i";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Ingrediente> findActivos() throws SQLException {
        String sql = SELECT_BASE +
                "WHERE i.estado = 'ACTIVO' ORDER BY i.nombre";

        List<Ingrediente> resultado = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                resultado.add(map(rs));
            }
        }
        return resultado;
    }

    public List<Ingrediente> findActivosPorNombre(String texto) throws SQLException {
        String sql = SELECT_BASE +
                "WHERE i.estado = 'ACTIVO' AND LOWER(i.nombre) LIKE LOWER(?) " +
                "ORDER BY i.nombre";

        List<Ingrediente> resultado = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + texto + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resultado.add(map(rs));
                }
            }
        }
        return resultado;
    }

    public Ingrediente save(Ingrediente i) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            return save(conn, i);
        }
    }

    public Ingrediente save(Connection conn, Ingrediente i) throws SQLException {
        if (i.getUnidadMedida() == null || i.getUnidadMedida().isBlank()) {
            throw new SQLException(
                    "La unidad de medida es obligatoria para guardar un ingrediente"
            );
        }

        if (i.getId() == null) {

            String sql = """
                    INSERT INTO ingredientes (
                        nombre,
                        descripcion,
                        unidad_medida,
                        costo_unitario,
                        stock_actual,
                        stock_minimo,
                        stock_maximo,
                        estado
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING ingrediente_id
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                bindCampos(stmt, i);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        i.setId(rs.getLong(1));
                    }
                }
            }
        } else {

            String sql = """
                    UPDATE ingredientes
                    SET nombre = ?,
                        descripcion = ?,
                        unidad_medida = ?,
                        costo_unitario = ?,
                        stock_actual = ?,
                        stock_minimo = ?,
                        stock_maximo = ?,
                        estado = ?,
                        fecha_actualizacion = CURRENT_TIMESTAMP
                    WHERE ingrediente_id = ?
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                bindCampos(stmt, i);
                stmt.setLong(9, i.getId());
                stmt.executeUpdate();
            }
        }
        return i;
    }

    public void actualizarStock(Connection conn, long id, BigDecimal nuevoStock)
            throws SQLException {

        String sql = """
                UPDATE ingredientes
                SET stock_actual = ?,
                    fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE ingrediente_id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, nuevoStock);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public List<Ingrediente> findAllActivos() throws SQLException {
        return findActivos();
    }

    private void bindCampos(PreparedStatement stmt, Ingrediente i) throws SQLException {
        stmt.setString(1, i.getNombre());
        stmt.setString(2, i.getDescripcion());
        stmt.setString(3, i.getUnidadMedida());
        stmt.setBigDecimal(4, i.getCostoUnitario());
        stmt.setBigDecimal(5, i.getStockActual());
        stmt.setBigDecimal(6, i.getStockMinimo());
        if (i.getStockMaximo() != null) {
            stmt.setBigDecimal(7, i.getStockMaximo());
        } else {
            stmt.setNull(7, Types.NUMERIC);
        }

        stmt.setString(8, i.getEstado());
    }

    private Ingrediente map(ResultSet rs) throws SQLException {
        Ingrediente i = new Ingrediente();
        i.setId(rs.getLong("ingrediente_id"));
        i.setNombre(rs.getString("nombre"));
        i.setDescripcion(rs.getString("descripcion"));
        i.setUnidadMedida(rs.getString("unidad_medida"));
        i.setCostoUnitario(rs.getBigDecimal("costo_unitario"));
        i.setStockActual(rs.getBigDecimal("stock_actual"));
        i.setStockMinimo(rs.getBigDecimal("stock_minimo"));
        i.setStockMaximo(rs.getBigDecimal("stock_maximo"));
        i.setEstado(rs.getString("estado"));
        Timestamp fc = rs.getTimestamp("fecha_creacion");
        if (fc != null) {
            i.setFechaCreacion(fc.toLocalDateTime());
        }
        Timestamp fa = rs.getTimestamp("fecha_actualizacion");
        if (fa != null) {
            i.setFechaActualizacion(fa.toLocalDateTime());
        }
        return i;
    }
}