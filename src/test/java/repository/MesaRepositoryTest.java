package repository;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

class MesaRepositoryTest {

    @Test
    @DisplayName("devuelve un Optional vacío cuando el ID de la mesa no se encuentra registrado")
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Explicitamos que el ResultSet no encuentra registros
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertTrue(new MesaRepository().findById(4).isEmpty());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setInt(1, 4);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }

    @Test
    @DisplayName("devuelve la mesa mapeada correctamente cuando el ID existe en la base de datos")
    void devuelveMesaCuandoExisteRegistro() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Simulamos que el ResultSet encuentra un registro
            when(jdbc.filas.next()).thenReturn(true);

            // Configuramos los mocks de forma flexible para soportar lectura por nombre de columna o índice numérico
            when(jdbc.filas.getInt(anyString())).thenReturn(4);
            when(jdbc.filas.getInt(anyInt())).thenReturn(4);

            when(jdbc.filas.getLong(anyString())).thenReturn(1L);
            when(jdbc.filas.getLong(anyInt())).thenReturn(1L);

            when(jdbc.filas.getString(anyString())).thenReturn("DISPONIBLE");
            when(jdbc.filas.getString(anyInt())).thenReturn("DISPONIBLE");

            var resultado = new MesaRepository().findById(4);

            // Etapa de assert
            assertTrue(resultado.isPresent());
            var mesa = resultado.get();
            assertEquals(4, mesa.getNumeroMesa()); // o getId() según la firma de tu entidad

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setInt(1, 4);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}