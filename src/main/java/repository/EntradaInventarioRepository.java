package repository;

import conf.ConexionDB; //importa la conexión a la base de datos
import java.sql.Connection; // Importa Connection de Java SQL.
import java.sql.PreparedStatement; // Sirve para ejecutar consultas SQL de manera preparada y segura,
import java.sql.SQLException; // Es una excepción que puede ocurrir cuando hay un problema con la base de datos,
import models.RegistroInventario.EntradaInventario; //importa el modelo de EntradaInventario

public class EntradaInventarioRepository {

    public void guardar(EntradaInventario entrada) throws SQLException {
        String sql = "INSERT INTO movimientos_inventario (ingrediente_id, tipo_movimiento, cantidad, motivo) " +
                "VALUES (?, ?, ?, ?)"; //variable que contiene la consulta en sql

        try (Connection conn = ConexionDB.obtenerConexion(); //conecta con la base de datos
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            //coloca el contenido del plato en el ?
            //el numero significa la posición en que va
            stmt.setLong(1, entrada.getIngredienteId());
            stmt.setString(2, entrada.getTipoMovimiento());
            stmt.setDouble(3, entrada.getCantidad());
            stmt.setString(4, entrada.getMotivo());

            stmt.executeUpdate();
        }
    }
    
}