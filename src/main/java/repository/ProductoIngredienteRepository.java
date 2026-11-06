package repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import ConexionBD.ConexionBD;
import entity.ProductoIngrediente;

public class ProductoIngredienteRepository {

    public void guardar(ProductoIngrediente pi) throws SQLException {
        String sql = "INSERT INTO producto_ingrediente (producto_id, ingrediente_id, cantidad_necesaria) VALUES (?, ?, ?)";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, pi.getProductoId());
            stmt.setLong(2, pi.getIngredienteId());
            stmt.setBigDecimal(3, pi.getCantidadNecesaria());
            stmt.executeUpdate();
        }
    }

    // Trae todos los ingredientes que necesita un producto específico.
    public List<ProductoIngrediente> obtenerPorProducto(long productoId) throws SQLException {
        List<ProductoIngrediente> lista = new ArrayList<>();

        // Actualizado a la tabla y columnas correctas del nuevo esquema
        String sql = "SELECT producto_id, ingrediente_id, cantidad_necesaria " +
                "FROM producto_ingrediente " +
                "WHERE producto_id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, productoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long prodId = rs.getLong("producto_id");
                    long ingredienteId = rs.getLong("ingrediente_id");
                    BigDecimal cantidad = rs.getBigDecimal("cantidad_necesaria");

                    // Instanciación correcta utilizando el constructor de la entidad
                    ProductoIngrediente pi = new ProductoIngrediente(prodId, ingredienteId, cantidad);
                    lista.add(pi);
                }
            }
        }

        return lista;
    }

    public void eliminarPorProducto(long productoId) throws SQLException {
        // Corregido para apuntar a la tabla correcta producto_ingrediente
        String sql = "DELETE FROM producto_ingrediente WHERE producto_id = ?";

        try (Connection cn = ConexionBD.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, productoId);
            ps.executeUpdate();
        }
    }
}