package dao;

import database.ConexionBD;
import dto.ProductoDisponibilidad;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * Valida el pedido COMPLETO en una sola consulta, sumando el consumo de los
     * productos que comparten ingrediente.
     *
     * Preguntar producto por producto no sirve: con 400 g de carne, una
     * hamburguesa de 200 g y una lasana de 300 g pasan las dos por separado,
     * pero juntas no alcanzan.
     *
     * @param lineas producto_id -> cantidad pedida
     * @return descripcion de lo que falta; vacia si el pedido se puede preparar
     */
    public List<String> faltantesDelPedido(Map<Long, Integer> lineas) throws SQLException {
        List<String> faltantes = new ArrayList<>();

        if (lineas == null || lineas.isEmpty()) {
            return faltantes;
        }

        StringBuilder json = new StringBuilder("[");
        for (Map.Entry<Long, Integer> linea : lineas.entrySet()) {
            if (json.length() > 1) json.append(',');
            json.append("{\"producto_id\":").append(linea.getKey())
                .append(",\"cantidad\":").append(linea.getValue()).append('}');
        }
        json.append(']');

        String sql = "SELECT faltante, requerido, disponible FROM fn_pedido_faltantes(?::jsonb)";

        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, json.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    faltantes.add(String.format(
                            "%s — se necesitan %s y hay %s",
                            rs.getString("faltante"),
                            rs.getBigDecimal("requerido").stripTrailingZeros().toPlainString(),
                            rs.getBigDecimal("disponible").stripTrailingZeros().toPlainString()));
                }
            }
        }
        return faltantes;
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
