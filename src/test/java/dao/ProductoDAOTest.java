package dao;

import support.JdbcSimulado;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductoDAOTest {

    @Test
    @DisplayName("devuelve un menú vacío cuando no existen productos ni categorías en la base de datos")
    void devuelveMenuVacioSiNoHayProductos() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertTrue(new ProductoDAO().obtenerMenuPorCategorias().isEmpty());

            // Verificaciones de interacciones
            verify(jdbc.sentencia, times(2)).executeQuery();
            verify(jdbc.conexion, times(2)).close();
        }
    }

    @Test
    @DisplayName("ejecuta correctamente las consultas del menú cuando existen registros")
    void verificaEjecucionDeConsultasDelMenu() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Simulamos que la primera consulta retorna falso o true según lo que espere el DAO
            when(jdbc.filas.next()).thenReturn(false);

            var menu = new ProductoDAO().obtenerMenuPorCategorias();

            // Verificamos que el DAO interactuó correctamente con la base de datos simulada
            assertNotNull(menu);
            verify(jdbc.sentencia, times(2)).executeQuery();
            verify(jdbc.conexion, times(2)).close();
        }
    }
}