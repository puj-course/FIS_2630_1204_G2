package repositories.RegistrarPlato;

import conf.ConexionDB; //importa la conexión a la base de datos
import java.sql.Connection; // Importa Connection de Java SQL.
import java.sql.PreparedStatement; // Sirve para ejecutar consultas SQL de manera preparada y segura,
import java.sql.SQLException; // Es una excepción que puede ocurrir cuando hay un problema con la base de datos,
import models.RegistrarPlato.Plato;

public class PlatoRepository {

    public void guardar(Plato plato) throws SQLException {
        String sql = "INSERT INTO productos (codigo, nombre, descripcion, categoria, precio_venta, costo) " +
                "VALUES (?, ?, ?, ?, ?, ?)"; //variable que contiene la consulta en sql

        try (Connection conn = ConexionDB.obtenerConexion(); //conecta con la base de datos
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            //coloca el contenido del plato en el ?
            //el numero significa la posición en que va
            stmt.setString(1, plato.getCodigo());
            stmt.setString(2, plato.getNombre());
            stmt.setString(3, plato.getDescripcion());
            stmt.setString(4, plato.getCategoria());
            stmt.setDouble(5, plato.getPrecioVenta());
            stmt.setDouble(6, plato.getCosto());

            stmt.executeUpdate(); //ejecuta la consulta
        }
    }
    
}