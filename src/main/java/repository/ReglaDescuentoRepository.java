package repository;//hecho con ayuda de una LLM, sujeto a cambios para hacer pruebas con la base de datos gracias por entender

import java.sql.*;
import java.util.*;
import conf.ConexionDB;
import entity.ReglaDescuento;

public class ReglaDescuentoRepository {

    public void guardar(ReglaDescuento regla) throws SQLException { // Método para guardar una nueva regla de descuento en la base de datos
        String sql = "INSERT INTO regla_descuento (tipo_cambio, valor, es_porcentaje) VALUES (?, ?, ?)";
        try (Connection conn = ConexionDB.obtenerConexion(); // Obtener la conexión a la base de datos
             PreparedStatement stmt = conn.prepareStatement(sql)) { // Preparar la sentencia SQL para insertar la nueva regla de descuento

            stmt.setString(1, regla.getTipoCambio()); // Establecer el valor del tipo de cambio en la sentencia SQL
            stmt.setDouble(2, regla.getValor()); // Establecer el valor del descuento en la sentencia SQL
            stmt.setBoolean(3, regla.isEsPorcentaje());// Establecer si el descuento es un porcentaje o un valor fijo en la sentencia SQL
            stmt.executeUpdate();// Ejecutar la sentencia SQL para insertar la nueva regla de descuento
        }
    }

    public List<ReglaDescuento> obtenerTodos() throws SQLException { // Método para obtener todas las reglas de descuento de la base de datos
        List<ReglaDescuento> reglas = new ArrayList<>();
        String sql = "SELECT id, tipo_cambio, valor, es_porcentaje FROM regla_descuento";

        try (Connection conn = ConexionDB.obtenerConexion(); // Obtener la conexión a la base de datos
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {// Iterar sobre los resultados de la consulta y crear objetos ReglaDescuento para cada fila
                ReglaDescuento r = new ReglaDescuento();
                r.setId(rs.getInt("id"));
                r.setTipoCambio(rs.getString("tipo_cambio"));
                r.setValor(rs.getDouble("valor"));
                r.setEsPorcentaje(rs.getBoolean("es_porcentaje"));
                reglas.add(r);
            }
        }
        return reglas;
    }
    
}
