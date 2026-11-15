package repository;

import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatoInsumoRepositoryTest {
    @Test
    void enviaLosDatosAlInsert() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            new PlatoInsumoRepository().guardar(new entity.PlatoInsumo(4, 5, 3));
            verify(jdbc.sentencia).setInt(1, 4);
            verify(jdbc.sentencia).setInt(2, 5);
            verify(jdbc.sentencia).setInt(3, 3);
            verify(jdbc.sentencia).executeUpdate();
        }
    }
}

