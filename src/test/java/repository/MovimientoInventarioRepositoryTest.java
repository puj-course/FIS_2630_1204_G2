package repository;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import java.sql.Timestamp;

class MovimientoInventarioRepositoryTest {

    @Test
    @DisplayName("devuelve un Optional vacío cuando el ID del movimiento no se encuentra registrado")
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Explicitamos que el ResultSet no encuentra registros
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertTrue(new MovimientoInventarioRepository().findById(4L).isEmpty());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }

    @Test
    @DisplayName("devuelve el movimiento de inventario mapeado correctamente cuando el ID existe")
    void devuelveMovimientoCuandoExisteRegistro() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Simulamos que el ResultSet encuentra un registro
            when(jdbc.filas.next()).thenReturn(true);

            // Configuramos los mocks de manera flexible para lectura por nombre de columna o índice
            when(jdbc.filas.getLong(anyString())).thenReturn(4L);
            when(jdbc.filas.getLong(anyInt())).thenReturn(4L);

            when(jdbc.filas.getString(anyString())).thenReturn("ENTRADA");
            when(jdbc.filas.getString(anyInt())).thenReturn("ENTRADA");

            when(jdbc.filas.getDouble(anyString())).thenReturn(5.0);
            when(jdbc.filas.getDouble(anyInt())).thenReturn(5.0);

            when(jdbc.filas.getTimestamp(anyString())).thenReturn(new Timestamp(System.currentTimeMillis()));
            when(jdbc.filas.getTimestamp(anyInt())).thenReturn(new Timestamp(System.currentTimeMillis()));

            var resultado = new MovimientoInventarioRepository().findById(4L);

            // Etapa de assert
            assertTrue(resultado.isPresent());
            var movimiento = resultado.get();
            assertEquals(4L, movimiento.getId());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}