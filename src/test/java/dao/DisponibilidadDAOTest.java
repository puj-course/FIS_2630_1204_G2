package dao;

import support.JdbcSimulado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisponibilidadDAOTest {

    @Test
    @DisplayName("retorna verdadero cuando el producto tiene disponibilidad para la cantidad solicitada")
    void consultaDisponibilidadParaLaCantidadSolicitada() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            when(jdbc.filas.next()).thenReturn(true);
            when(jdbc.filas.getBoolean(1)).thenReturn(true);

            // Etapa de assert
            assertTrue(new DisponibilidadDAO().estaDisponible(4L, 3));

            // Verificaciones de interacción con JDBC
            verify(jdbc.sentencia, times(2)).setLong(1, 4L);
            verify(jdbc.sentencia).setInt(2, 3);
        }
    }

    @Test
    @DisplayName("retorna falso cuando el producto no cuenta con la disponibilidad requerida")
    void retornaFalsoCuandoNoHayDisponibilidad() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            when(jdbc.filas.next()).thenReturn(true);
            when(jdbc.filas.getBoolean(1)).thenReturn(false);

            // Etapa de assert
            assertFalse(new DisponibilidadDAO().estaDisponible(4L, 3));

            // Verificaciones de interacción con JDBC
            verify(jdbc.sentencia, times(2)).setLong(1, 4L);
            verify(jdbc.sentencia).setInt(2, 3);
        }
    }

    @Test
    @DisplayName("retorna falso cuando la consulta no arroja ningún resultado")
    void retornaFalsoCuandoNoExistenRegistros() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertFalse(new DisponibilidadDAO().estaDisponible(4L, 3));

            // Verificaciones de interacción con JDBC
            verify(jdbc.sentencia, times(2)).setLong(1, 4L);
            verify(jdbc.sentencia).setInt(2, 3);
        }
    }
}