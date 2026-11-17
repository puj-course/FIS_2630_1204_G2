package repository;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

class ZonaRepositoryTest {

    @Test
    @DisplayName("devuelve un Optional vacío cuando el ID de la zona no se encuentra registrado")
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Explicitamos que el ResultSet no encuentra registros
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertTrue(new ZonaRepository().findById(4L).isEmpty());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }

    @Test
    @DisplayName("devuelve la zona mapeada correctamente cuando el ID existe en la base de datos")
    void devuelveZonaCuandoExisteRegistro() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Simulamos que el ResultSet encuentra un registro
            when(jdbc.filas.next()).thenReturn(true);

            // Configuramos los mocks de manera flexible para lectura por nombre de columna o índice
            when(jdbc.filas.getLong(anyString())).thenReturn(4L);
            when(jdbc.filas.getLong(anyInt())).thenReturn(4L);

            when(jdbc.filas.getString(anyString())).thenReturn("Terraza");
            when(jdbc.filas.getString(anyInt())).thenReturn("Terraza");

            var resultado = new ZonaRepository().findById(4L);

            // Etapa de assert
            assertTrue(resultado.isPresent());
            var zona = resultado.get();
            assertNotNull(zona);

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}