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
            "SELECT ingrediente_id, nombre, descripcion, unidad_medida_id, costo_unitario, " +
                    "stock_actual, stock_minimo, stock_maximo, lote, fecha_vencimiento, estado " +
                    "FROM ingredientes ";

    public Optional<Ingrediente> findById(long id) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            return findById(conn, id);
        }
    }

    public Optional<Ingrediente> findById(Connection conn, long id) throws SQLException {
        String sql = SELECT_BASE + "WHERE ingrediente_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    // Equivalente al @Lock(PESSIMISTIC_WRITE): bloquea la fila hasta que termine la transacción.
    public Optional<Ingrediente> findByIdForUpdate(Connection conn, long id) throws SQLException {
        String sql = SELECT_BASE + "WHERE ingrediente_id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Ingrediente> findActivos() throws SQLException {
        String sql = SELECT_BASE + "WHERE estado = 'ACTIVO' ORDER BY nombre";
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
        String sql = SELECT_BASE + "WHERE estado = 'ACTIVO' AND LOWER(nombre) LIKE LOWER(?) ORDER BY nombre";
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
        if (i.getUnidadMedidaId() == null) {
            throw new SQLException("unidad_medida_id es obligatorio para guardar un ingrediente");
        }

        if (i.getId() == null) {
            String sql = "INSERT INTO ingredientes (nombre, descripcion, unidad_medida_id, costo_unitario, " +
                    "stock_actual, stock_minimo, stock_maximo, lote, fecha_vencimiento, " +
                    "estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING ingrediente_id";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                bindCampos(stmt, i);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        i.setId(rs.getLong(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE ingredientes SET nombre = ?, descripcion = ?, unidad_medida_id = ?, " +
                    "costo_unitario = ?, stock_actual = ?, stock_minimo = ?, stock_maximo = ?, " +
                    "lote = ?, fecha_vencimiento = ?, estado = ? " +
                    "WHERE ingrediente_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                bindCampos(stmt, i);
                stmt.setLong(11, i.getId());
                stmt.executeUpdate();
            }
        }
        return i;
    }

    // Para actualizar stock dentro de una transacción compartida
    public void actualizarStock(Connection conn, long id, BigDecimal nuevoStock) throws SQLException {
        String sql = "UPDATE ingredientes SET stock_actual = ?, fecha_actualizacion = CURRENT_TIMESTAMP " +
                "WHERE ingrediente_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, nuevoStock);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public List<Ingrediente> findAllActivos() throws SQLException {
        String sql = SELECT_BASE + "WHERE estado = 'ACTIVO' ORDER BY nombre";

        List<Ingrediente> ingredientes = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ingredientes.add(map(rs));
            }
        }
        return ingredientes;
    }

    private void bindCampos(PreparedStatement stmt, Ingrediente i) throws SQLException {
        stmt.setString(1, i.getNombre());
        stmt.setLong(2, i.getUnidadMedidaId());
        stmt.setBigDecimal(3, i.getCostoUnitario() != null ? i.getCostoUnitario() : BigDecimal.ZERO);
        stmt.setBigDecimal(4, i.getStockActual() != null ? i.getStockActual() : BigDecimal.ZERO);
        stmt.setBigDecimal(5, i.getStockMinimo() != null ? i.getStockMinimo() : BigDecimal.ZERO);
        stmt.setBigDecimal(6, i.getStockMaximo());
        stmt.setString(7, i.getLote());

        if (i.getFechaVencimiento() != null) {
            stmt.setDate(9, Date.valueOf(i.getFechaVencimiento()));
        } else {
            stmt.setNull(9, Types.DATE);
        }

        stmt.setString(10, i.getEstado() != null ? i.getEstado() : "ACTIVO");
    }

    private Ingrediente map(ResultSet rs) throws SQLException {
        Ingrediente i = new Ingrediente();
        i.setId(rs.getLong("ingrediente_id"));
        i.setNombre(rs.getString("nombre"));
        i.setUnidadMedidaId(rs.getLong("unidad_medida_id"));
        i.setCostoUnitario(rs.getBigDecimal("costo_unitario"));
        i.setStockActual(rs.getBigDecimal("stock_actual"));
        i.setStockMinimo(rs.getBigDecimal("stock_minimo"));
        i.setStockMaximo(rs.getBigDecimal("stock_maximo"));
        i.setLote(rs.getString("lote"));

        Date fv = rs.getDate("fecha_vencimiento");
        if (fv != null) {
            i.setFechaVencimiento(fv.toLocalDate());
        }

        i.setEstado(rs.getString("estado"));
        return i;
    }
}