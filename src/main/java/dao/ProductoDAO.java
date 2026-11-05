package dao;

import ConexionBD.ConexionBD;
import entity.Productos;

import java.sql.*;
import java.util.*;

public class ProductoDAO {
    private final DisponibilidadDAO disponibilidadDAO = new DisponibilidadDAO();

    public Map<String, List<Productos>> obtenerMenuPorCategorias() throws SQLException {
        disponibilidadDAO.recalcularTodos();

        String sql = """
                SELECT p.producto_id, p.codigo, p.nombre, p.descripcion,
                       c.nombre AS categoria,
                       p.precio_venta, p.estado
                FROM productos p
                INNER JOIN categorias c ON c.categoria_id = p.categoria_id
                WHERE p.estado IN ('DISPONIBLE', 'AGOTADO')
                  AND c.estado = 'ACTIVO'
                ORDER BY c.orden_visual, c.nombre, p.nombre
                """;

        Map<String, List<Productos>> menu = new LinkedHashMap<>();
        try (Connection cn = ConexionBD.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Productos producto = new Productos(
                        rs.getLong("producto_id"),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getString("categoria"),
                        rs.getBigDecimal("precio_venta"),
                        rs.getString("estado")
                );
                menu.computeIfAbsent(producto.getCategoria(), k -> new ArrayList<>()).add(producto);
            }
        }
        return menu;
    }

    public boolean estaDisponible(long productoId) throws SQLException {
        return disponibilidadDAO.estaDisponible(productoId);
    }

    public boolean estaDisponible(long productoId, int cantidad) throws SQLException {
        return disponibilidadDAO.estaDisponible(productoId, cantidad);
    }
}
