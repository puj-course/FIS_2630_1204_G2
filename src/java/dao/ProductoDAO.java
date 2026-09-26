package dao;

import database.ConexionBD;
import entity.Producto;

import java.sql.*;
import java.util.*;

public class ProductoDAO {
    private final DisponibilidadDAO disponibilidadDAO = new DisponibilidadDAO();

    public Map<String, List<Producto>> obtenerMenuPorCategorias() throws SQLException {
        disponibilidadDAO.recalcularTodos();

        String sql = """
                SELECT p.producto_id, p.codigo, p.nombre, p.descripcion,
                       c.nombre AS categoria,
                       p.precio_venta, p.estado
                FROM productos p
                INNER JOIN categorias c ON c.categoria_id = p.categoria_id
                WHERE p.estado IN ('DISPONIBLE', 'AGOTADO')
                  AND c.estado = 'ACTIVO'
                ORDER BY c.orden_presentacion, c.nombre, p.nombre
                """;

        Map<String, List<Producto>> menu = new LinkedHashMap<>();
        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Producto producto = new Producto(
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
