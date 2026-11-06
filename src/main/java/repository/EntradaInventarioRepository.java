package repository;

import ConexionBD.ConexionBD; //importa la conexión a la base de datos
import java.sql.Connection; // Importa Connection de Java SQL.
import java.sql.PreparedStatement; // Sirve para ejecutar consultas SQL de manera preparada y segura,
import java.sql.SQLException; // Es una excepción que puede ocurrir cuando hay un problema con la base de datos,
import entity.EntradaInventario; //importa el modelo de EntradaInventario

public class EntradaInventarioRepository {

    // Método que acepta la conexión transaccional
    public void guardar(Connection conn, EntradaInventario entrada) throws SQLException {
        String sql = "INSERT INTO movimientos_inventario (ingrediente_id, cantidad, motivo, tipo_movimiento, fecha_movimiento) " +
                "VALUES (?, ?, ?, 'ENTRADA', CURRENT_TIMESTAMP)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, entrada.getIngredienteId());
            stmt.setDouble(2, entrada.getCantidad());
            stmt.setString(3, entrada.getMotivo());
            stmt.executeUpdate();
        }
    }
}