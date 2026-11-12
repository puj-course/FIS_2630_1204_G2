package dao;

import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductoDAOTest {
    @Test
    void devuelveMenuVacioSiNoHayProductos() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            assertTrue(new ProductoDAO().obtenerMenuPorCategorias().isEmpty());
            verify(jdbc.sentencia, times(2)).executeQuery();
            verify(jdbc.conexion, times(2)).close();
        }
    }
}



