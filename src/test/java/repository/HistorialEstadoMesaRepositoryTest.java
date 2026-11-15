package repository;

import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HistorialEstadoMesaRepositoryTest {
    @Test
    void recuperaElIdDelCambioRegistrado() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            var cambio = new entity.HistorialEstadoMesa();
            cambio.setMesaId(4L);
            cambio.setEstadoNuevoId(2L);
            when(jdbc.filas.next()).thenReturn(true);
            when(jdbc.filas.getLong(1)).thenReturn(12L);
            assertEquals(12L, new HistorialEstadoMesaRepository().save(cambio).getId());
            verify(jdbc.sentencia).setNull(2, java.sql.Types.BIGINT);
            verify(jdbc.sentencia).setLong(3, 2L);
        }
    }
}

