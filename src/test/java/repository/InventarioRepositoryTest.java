package repository;

import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventarioRepositoryTest {
    @Test
    void devuelveRecetaVaciaParaProductoSinInsumos() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            when(jdbc.filas.next()).thenReturn(true, false);
            when(jdbc.filas.wasNull()).thenReturn(true);
            assertTrue(new InventarioRepository().obtenerReceta(jdbc.conexion, 4L).isEmpty());
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}



