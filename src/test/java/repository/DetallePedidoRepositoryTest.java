package repository;

import support.JdbcSimulado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

class DetallePedidoRepositoryTest {

    @Test
    @DisplayName("devuelve una lista vacía cuando el pedido no tiene detalles registrados")
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Explicitamos que el ResultSet no encuentra registros
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertTrue(new DetallePedidoRepository().findByPedidoId(4L).isEmpty());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }

    @Test
    @DisplayName("devuelve la lista de detalles mapeados correctamente cuando el pedido contiene productos")
    void devuelveDetallesCuandoExistenRegistros() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Simulamos que el ResultSet encuentra al menos un registro y luego finaliza
            when(jdbc.filas.next()).thenReturn(true).thenReturn(false);
            when(jdbc.filas.getLong("id")).thenReturn(100L);
            when(jdbc.filas.getLong("pedido_id")).thenReturn(4L);
            when(jdbc.filas.getLong("producto_id")).thenReturn(10L);
            when(jdbc.filas.getInt("cantidad")).thenReturn(2);
            when(jdbc.filas.getBigDecimal("precio_unitario")).thenReturn(new BigDecimal("15000"));

            var resultados = new DetallePedidoRepository().findByPedidoId(4L);

            // Etapa de assert
            assertNotNull(resultados);
            assertFalse(resultados.isEmpty());
            assertEquals(1, resultados.size());
            assertEquals(100L, resultados.get(0).getId());
            assertEquals(2, resultados.get(0).getCantidad());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}