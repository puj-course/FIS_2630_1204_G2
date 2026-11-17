package repository;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;

class InventarioRepositoryTest {

    @Test
    @DisplayName("devuelve una receta vacía cuando el producto no tiene insumos asociados")
    void devuelveRecetaVaciaParaProductoSinInsumos() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            when(jdbc.filas.next()).thenReturn(true, false);
            when(jdbc.filas.wasNull()).thenReturn(true);

            // Etapa de assert
            assertTrue(new InventarioRepository().obtenerReceta(jdbc.conexion, 4L).isEmpty());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }

    @Test
    @DisplayName("devuelve la receta mapeada correctamente cuando el producto tiene insumos en el inventario")
    void devuelveRecetaConInsumosCuandoExistenRegistros() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Simulamos que el ResultSet encuentra un registro y luego finaliza
            when(jdbc.filas.next()).thenReturn(true, false);

            // Configuramos los mocks de manera flexible para evitar valores por defecto en cero o nulos
            when(jdbc.filas.getLong(anyString())).thenReturn(1L);
            when(jdbc.filas.getLong(anyInt())).thenReturn(1L);

            when(jdbc.filas.getString(anyString())).thenReturn("Ingrediente Base");
            when(jdbc.filas.getString(anyInt())).thenReturn("Ingrediente Base");

            when(jdbc.filas.getBigDecimal(anyString())).thenReturn(new BigDecimal("2.50"));
            when(jdbc.filas.getBigDecimal(anyInt())).thenReturn(new BigDecimal("2.50"));

            var receta = new InventarioRepository().obtenerReceta(jdbc.conexion, 4L);

            // Etapa de assert
            assertNotNull(receta);
            assertFalse(receta.isEmpty());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}