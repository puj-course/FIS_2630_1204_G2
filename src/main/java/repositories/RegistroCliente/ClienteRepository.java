package repositories.RegistroCliente;//hecho con ayuda de una LLM, sujeto a cambios para hacer pruebas con la base de datos gracias por entender

import conf.ConexionDB; //importa la conexión a la base de datos
import java.sql.Connection; // Importa Connection de Java SQL.
import java.sql.PreparedStatement; // Sirve para ejecutar consultas SQL de manera preparada y segura,
import java.sql.SQLException; // Es una excepción que puede ocurrir cuando hay un problema con la base de datos,
import models.RegistroCliente.Cliente; //importa el modelo de cliente


public class ClienteRepository {
    public void guardar(Cliente cliente) throws SQLException {

        String sql = "INSERT INTO cliente (nombre, telefono, correo) VALUES (?, ?, ?)"; //variable que contiene la consulta en sql

        try (Connection conn = ConexionDB.obtenerConexion(); //conecta con la base de datos
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            //coloca el contenido del plato en el ?
            //el numero significa la posición en que va
            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getTelefono());
            stmt.setString(3, cliente.getCorreo());

            stmt.executeUpdate();
        }
    }
}
