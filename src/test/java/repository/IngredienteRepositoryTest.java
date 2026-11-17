package repository;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;

class IngredienteRepositoryTest {

    @Test
    @DisplayName("devuelve un Optional vacío cuando el ID del ingrediente no se encuentra registrado")
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Explicitamos que el ResultSet no encuentra registros
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertTrue(new IngredienteRepository().findById(4L).isEmpty());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }

    @Test
    @DisplayName("devuelve el ingrediente mapeado correctamente cuando el ID existe en la base de datos")
    void devuelveIngredienteCuandoExisteRegistro() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Simulamos que el ResultSet encuentra un registro
            when(jdbc.filas.next()).thenReturn(true);

            // Configuramos los mocks de forma flexible para soportar lectura por nombre de columna o índice
            when(jdbc.filas.getLong(anyString())).thenReturn(4L);
            when(jdbc.filas.getLong(anyInt())).thenReturn(4L);

            when(jdbc.filas.getString(anyString())).thenReturn("Tomate");
            when(jdbc.filas.getString(anyInt())).thenReturn("Tomate");

            when(jdbc.filas.getBigDecimal(anyString())).thenReturn(new BigDecimal("10.00"));
            when(jdbc.filas.getBigDecimal(anyInt())).thenReturn(new BigDecimal("10.00"));

            var resultado = new IngredienteRepository().findById(4L);

            // Etapa de assert
            assertTrue(resultado.isPresent());
            var ingrediente = resultado.get();
            assertEquals(4L, ingrediente.getId());
            assertEquals("Tomate", ingrediente.getNombre());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}