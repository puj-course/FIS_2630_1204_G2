package dao;

import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisponibilidadDAOTest {
    @Test
    void consultaDisponibilidadParaLaCantidadSolicitada() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            when(jdbc.filas.next()).thenReturn(true);
            when(jdbc.filas.getBoolean(1)).thenReturn(true);
            assertTrue(new DisponibilidadDAO().estaDisponible(4L, 3));
            verify(jdbc.sentencia, times(2)).setLong(1, 4L);
            verify(jdbc.sentencia).setInt(2, 3);
        }
    }
}


