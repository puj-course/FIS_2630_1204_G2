package repository;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

class ReglaDescuentoRepositoryTest {

    @Test
    @DisplayName("devuelve una lista vacía cuando no hay reglas de descuento registradas")
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Explicitamos que el ResultSet no encuentra registros
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertTrue(new ReglaDescuentoRepository().obtenerTodos().isEmpty());

            // Verificaciones de interacciones con JDBC (al ser un 'obtenerTodos', no lleva parámetros previos)
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }

    @Test
    @DisplayName("devuelve la lista de reglas de descuento mapeadas correctamente cuando existen registros")
    void devuelveListaConReglasCuandoExistenRegistros() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Simulamos que el ResultSet encuentra un registro y luego finaliza
            when(jdbc.filas.next()).thenReturn(true, false);

            // Configuramos los mocks de manera flexible para lectura por nombre de columna o índice
            when(jdbc.filas.getLong(anyString())).thenReturn(1L);
            when(jdbc.filas.getLong(anyInt())).thenReturn(1L);

            when(jdbc.filas.getString(anyString())).thenReturn("VIP_DISCOUNT");
            when(jdbc.filas.getString(anyInt())).thenReturn("VIP_DISCOUNT");

            when(jdbc.filas.getBigDecimal(anyString())).thenReturn(new BigDecimal("15.00"));
            when(jdbc.filas.getBigDecimal(anyInt())).thenReturn(new BigDecimal("15.00"));

            when(jdbc.filas.getDouble(anyString())).thenReturn(15.0);
            when(jdbc.filas.getDouble(anyInt())).thenReturn(15.0);

            var resultado = new ReglaDescuentoRepository().obtenerTodos();

            // Etapa de assert
            assertNotNull(resultado);
            assertFalse(resultado.isEmpty());
            assertEquals(1, resultado.size());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}