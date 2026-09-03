package com.gastroflow.dao;

import com.gastroflow.database.ConexionBD;
import com.gastroflow.model.ProductoDisponibilidad;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DisponibilidadDAO {

    public int recalcularTodos() throws SQLException {
        String sql = "SELECT fn_recalcular_estados_productos()";
        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public String recalcularProducto(long productoId) throws SQLException {
        String sql = "SELECT fn_recalcular_estado_producto(?)";
        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public boolean estaDisponible(long productoId) throws SQLException {
        return estaDisponible(productoId, 1);
    }

    public boolean estaDisponible(long productoId, int cantidad) throws SQLException {
        recalcularProducto(productoId);
        String sql = "SELECT fn_producto_disponible_cantidad(?, ?)";
        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, productoId);
            ps.setInt(2, cantidad);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    public List<ProductoDisponibilidad> listarProductos() throws SQLException {
        String sql = """
                SELECT
                    producto_id,
                    codigo,
                    nombre,
                    COALESCE(categoria, 'Otros') AS categoria,
                    estado,
                    fn_motivo_receta(ingredientes) AS motivo
                FROM productos
                WHERE estado <> 'INACTIVO'
                ORDER BY COALESCE(categoria, 'Otros'), nombre
                """;

        List<ProductoDisponibilidad> resultado = new ArrayList<>();
        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(new ProductoDisponibilidad(
                        rs.getLong("producto_id"),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getString("estado"),
                        rs.getString("motivo")
                ));
            }
        }
        return resultado;
    }
}
