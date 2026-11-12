package repository;

import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntradaInventarioRepositoryTest {
    @Test
    void enviaLosDatosAlInsert() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            new EntradaInventarioRepository().guardar(new entity.EntradaInventario(4L, 2.5, "Compra"));
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).setDouble(3, 2.5);
            verify(jdbc.sentencia).executeUpdate();
        }
    }
}

