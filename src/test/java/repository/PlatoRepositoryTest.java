package repository;

import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatoRepositoryTest {
    @Test
    void enviaLosDatosAlInsert() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            new PlatoRepository().guardar(new entity.Plato("P1", "Sopa", "", "Entrada", 10000, 4000));
            verify(jdbc.sentencia).setString(1, "P1");
            verify(jdbc.sentencia).setDouble(5, 10000);
            verify(jdbc.sentencia).setDouble(6, 4000);
            verify(jdbc.sentencia).executeUpdate();
        }
    }
}

