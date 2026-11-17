package repository;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

class EstadoMesaRepositoryTest {

    @Test
    @DisplayName("devuelve un Optional vacío cuando el código de estado no se encuentra registrado")
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            when(jdbc.filas.next()).thenReturn(false);

            assertTrue(new EstadoMesaRepository().findByCodigoEstado("RESERVADA").isEmpty());

            verify(jdbc.sentencia).setString(1, "RESERVADA");
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }

    @Test
    @DisplayName("devuelve el estado de mesa mapeado correctamente cuando el código existe")
    void devuelveEstadoMesaCuandoExisteRegistro() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            when(jdbc.filas.next()).thenReturn(true);

            // Configuramos los mocks de forma flexible para nombre de columna e índice numérico
            when(jdbc.filas.getLong(anyString())).thenReturn(1L);
            when(jdbc.filas.getLong(anyInt())).thenReturn(1L);

            when(jdbc.filas.getString(anyString())).thenReturn("RESERVADA");
            when(jdbc.filas.getString(anyInt())).thenReturn("RESERVADA");

            var resultado = new EstadoMesaRepository().findByCodigoEstado("RESERVADA");

            // Etapa de assert
            assertTrue(resultado.isPresent());
            var estado = resultado.get();
            assertEquals(1L, estado.getId()); // <- Ahora coincidirá correctamente con 1

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setString(1, "RESERVADA");
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}