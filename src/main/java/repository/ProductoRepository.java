package repository;

import ConexionBD.ConexionBD;//importa la conexión a la base de datos
import entity.Productos;

import java.sql.Connection; // Importa Connection de Java SQL.
import java.sql.PreparedStatement; // Sirve para ejecutar consultas SQL de manera preparada y segura,
import java.sql.SQLException; // Es una excepción que puede ocurrir cuando hay un problema con la base de datos,

public class ProductoRepository {

    public void guardar(Productos producto) throws SQLException {
        String sql = "INSERT INTO productos (codigo, nombre, descripcion, categoria_id, precio_venta, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, producto.getCodigo());
            stmt.setString(2, producto.getNombre());
            stmt.setString(3, producto.getDescripcion());
            stmt.setString(4, producto.getCategoria());
            stmt.setBigDecimal(5, producto.getPrecioVenta());
            stmt.setString(6, producto.getEstado() != null ? producto.getEstado() : "DISPONIBLE");

            stmt.executeUpdate();
        }
    }
}