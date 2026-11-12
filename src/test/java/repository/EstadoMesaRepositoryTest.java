package repository;

import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoMesaRepositoryTest {
    @Test
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            assertTrue(new EstadoMesaRepository().findByCodigoEstado(" RESERVADA ").isEmpty());
            verify(jdbc.sentencia).setString(1, "RESERVADA");
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}

