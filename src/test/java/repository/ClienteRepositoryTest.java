package repository;

import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClienteRepositoryTest {
    @Test
    void enviaLosDatosAlInsert() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            new ClienteRepository().guardar(new entity.Cliente("CC", "004", "Ana", "Paz", "ana@example.test", "555"));
            verify(jdbc.sentencia).setString(1, "004");
            verify(jdbc.sentencia).setString(6, "CC");
            verify(jdbc.sentencia).executeUpdate();
        }
    }
}

