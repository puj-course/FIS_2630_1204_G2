package repository;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import entity.EntradaInventario;
import org.junit.jupiter.api.DisplayName;

class EntradaInventarioRepositoryTest {

    @Test
    @DisplayName("envía correctamente todos los campos de la entrada de inventario al ejecutar la sentencia de inserción")
    void enviaLosDatosAlInsert() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Creamos la entrada de inventario de prueba
            EntradaInventario entrada = new EntradaInventario(4L, 2.5, "Compra");

            // Ejecutamos la acción del repositorio
            new EntradaInventarioRepository().guardar(entrada);

            // Verificaciones de interacciones exactas basadas en el orden de las sentencias SQL
            verify(jdbc.sentencia).setLong(1, 4L);          // ID del ingrediente
            verify(jdbc.sentencia).setString(2, "entrada"); // Tipo de movimiento
            verify(jdbc.sentencia).setDouble(3, 2.5);      // Cantidad
            verify(jdbc.sentencia).setString(4, "Compra"); // Motivo / Observación

            // Confirmamos que la sentencia se ejecutó y se cerró correctamente
            verify(jdbc.sentencia).executeUpdate();
            verify(jdbc.sentencia).close();
        }
    }
}