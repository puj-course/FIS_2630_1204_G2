package com.gastroflow.dao;

import com.gastroflow.database.ConexionBD;
import com.gastroflow.model.Producto;

import java.sql.*;
import java.util.*;

public class ProductoDAO {
    private final DisponibilidadDAO disponibilidadDAO = new DisponibilidadDAO();

    public Map<String, List<Producto>> obtenerMenuPorCategorias() throws SQLException {
        disponibilidadDAO.recalcularTodos();

        String sql = """
                SELECT producto_id, codigo, nombre, descripcion,
                       COALESCE(categoria, 'Otros') AS categoria,
                       precio_venta, estado
                FROM productos
                WHERE estado IN ('DISPONIBLE', 'AGOTADO')
                ORDER BY COALESCE(categoria, 'Otros'), nombre
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
