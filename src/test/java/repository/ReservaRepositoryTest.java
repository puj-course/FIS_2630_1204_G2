package repository;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

import java.sql.Timestamp;

class ReservaRepositoryTest {

    @Test
    @DisplayName("devuelve un Optional vacío cuando el ID de la reserva no se encuentra registrado")
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Explicitamos que el ResultSet no encuentra registros
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertTrue(new ReservaRepository().findById(4).isEmpty());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setInt(1, 4);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }

    @Test
    @DisplayName("devuelve la reserva mapeada correctamente cuando el ID existe en la base de datos")
    void devuelveReservaCuandoExisteRegistro() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Simulamos que el ResultSet encuentra un registro
            when(jdbc.filas.next()).thenReturn(true);

            // Configuramos los mocks de manera flexible para lectura por nombre de columna o índice
            when(jdbc.filas.getInt(anyString())).thenReturn(4);
            when(jdbc.filas.getInt(anyInt())).thenReturn(4);

            when(jdbc.filas.getLong(anyString())).thenReturn(1L);
            when(jdbc.filas.getLong(anyInt())).thenReturn(1L);

            when(jdbc.filas.getString(anyString())).thenReturn("CONFIRMADA");
            when(jdbc.filas.getString(anyInt())).thenReturn("CONFIRMADA");

            // Proveemos un Timestamp válido para evitar NullPointerExceptions al mapear fechas/horas
            when(jdbc.filas.getTimestamp(anyString())).thenReturn(new Timestamp(System.currentTimeMillis()));
            when(jdbc.filas.getTimestamp(anyInt())).thenReturn(new Timestamp(System.currentTimeMillis()));

            var resultado = new ReservaRepository().findById(4);

            // Etapa de assert
            assertTrue(resultado.isPresent());
            var reserva = resultado.get();
            assertNotNull(reserva);

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setInt(1, 4);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}