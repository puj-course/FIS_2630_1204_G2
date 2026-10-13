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
                    "stock_actual, stock_minimo, stock_maximo, proveedor, lote, fecha_vencimiento, " +
                    "categoria, estado, fecha_creacion, fecha_actualizacion " +
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

    // Equivalente al @Lock(PESSIMISTIC_WRITE) del repositorio JPA original:
    // bloquea la fila hasta que termine la transacción de la Connection recibida.
    // Solo tiene efecto con conn.setAutoCommit(false).
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
                    "stock_actual, stock_minimo, stock_maximo, proveedor, lote, fecha_vencimiento, " +
                    "categoria, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING ingrediente_id";
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
                    "proveedor = ?, lote = ?, fecha_vencimiento = ?, categoria = ?, estado = ?, " +
                    "fecha_actualizacion = CURRENT_TIMESTAMP WHERE ingrediente_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                bindCampos(stmt, i);
                stmt.setLong(13, i.getId());
                stmt.executeUpdate();
            }
        }
        return i;
    }

    // Para descontar/reponer stock dentro de una transacción compartida con un movimiento de inventario
    public void actualizarStock(Connection conn, long id, BigDecimal nuevoStock) throws SQLException {
        String sql = "UPDATE ingredientes SET stock_actual = ?, fecha_actualizacion = CURRENT_TIMESTAMP " +
                "WHERE ingrediente_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, nuevoStock);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    private void bindCampos(PreparedStatement stmt, Ingrediente i) throws SQLException {
        stmt.setString(1, i.getNombre());
        stmt.setString(2, i.getDescripcion());
        stmt.setLong(3, i.getUnidadMedidaId());
        stmt.setBigDecimal(4, i.getCostoUnitario());
        stmt.setBigDecimal(5, i.getStockActual());
        stmt.setBigDecimal(6, i.getStockMinimo());
        stmt.setBigDecimal(7, i.getStockMaximo());
        stmt.setString(8, i.getProveedor());
        stmt.setString(9, i.getLote());
        if (i.getFechaVencimiento() != null) {
            stmt.setDate(10, Date.valueOf(i.getFechaVencimiento()));
        } else {
            stmt.setNull(10, Types.DATE);
        }
        stmt.setString(11, i.getCategoria());
        stmt.setString(12, i.getEstado());
    }

    private Ingrediente map(ResultSet rs) throws SQLException {
        Ingrediente i = new Ingrediente();
        i.setId(rs.getLong("ingrediente_id"));
        i.setNombre(rs.getString("nombre"));
        i.setDescripcion(rs.getString("descripcion"));
        i.setUnidadMedidaId(rs.getLong("unidad_medida_id"));
        i.setCostoUnitario(rs.getBigDecimal("costo_unitario"));
        i.setStockActual(rs.getBigDecimal("stock_actual"));
        i.setStockMinimo(rs.getBigDecimal("stock_minimo"));
        i.setStockMaximo(rs.getBigDecimal("stock_maximo"));
        i.setProveedor(rs.getString("proveedor"));
        i.setLote(rs.getString("lote"));
        Date fv = rs.getDate("fecha_vencimiento");
        if (fv != null) {
            i.setFechaVencimiento(fv.toLocalDate());
        }
        i.setCategoria(rs.getString("categoria"));
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