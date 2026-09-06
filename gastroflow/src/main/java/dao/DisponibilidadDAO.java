package dao;

import database.ConexionBD;
import dto.ProductoDisponibilidad;

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
                    p.producto_id,
                    p.codigo,
                    p.nombre,
                    c.nombre AS categoria,
                    p.estado,
                    fn_motivo_receta(p.ingredientes) AS motivo
                FROM productos p
                INNER JOIN categorias c ON c.categoria_id = p.categoria_id
                WHERE p.estado <> 'INACTIVO'
                ORDER BY c.orden_presentacion, c.nombre, p.nombre
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
