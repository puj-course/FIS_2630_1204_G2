package repository;

import entity.Cliente;
import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class ClienteRepositoryTest {

    @Test
    @DisplayName("envía correctamente todos los campos del cliente al ejecutar la sentencia de inserción")
    void enviaLosDatosAlInsert() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Creamos el cliente de prueba
            Cliente cliente = new Cliente("CC", "004", "Ana", "Paz", "ana@example.test", "555");

            // Ejecutamos la acción del repositorio
            new ClienteRepository().guardar(cliente);

            // Etapa de verificación y aserciones de interacciones JDBC (camino lineal)
            verify(jdbc.sentencia).setString(1, "004");      // Número de documento
            verify(jdbc.sentencia).setString(2, "Ana");      // Nombre
            verify(jdbc.sentencia).setString(3, "Paz");      // Apellido
            verify(jdbc.sentencia).setString(4, "ana@example.test"); // Correo
            verify(jdbc.sentencia).setString(5, "555");      // Teléfono
            verify(jdbc.sentencia).setString(6, "CC");       // Tipo de documento

            // Confirmamos que la sentencia de actualización/inserción fue ejecutada
            verify(jdbc.sentencia).executeUpdate();
        }
    }
}