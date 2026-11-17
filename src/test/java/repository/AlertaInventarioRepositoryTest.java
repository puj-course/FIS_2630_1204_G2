package repository;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.sql.Timestamp;

class AlertaInventarioRepositoryTest {

    @Test
    @DisplayName("devuelve un Optional vacío cuando la consulta no encuentra ninguna alerta pendiente")
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Explicitamos que no hay registros en la base de datos simulada
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertTrue(new AlertaInventarioRepository().findPendienteByIngredienteId(4L).isEmpty());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }

    @Test
    @DisplayName("devuelve la alerta mapeada correctamente cuando existe un registro pendiente")
    void devuelveAlertaCuandoExisteRegistro() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Simulamos que el ResultSet encuentra un registro
            when(jdbc.filas.next()).thenReturn(true);
            when(jdbc.filas.getLong("id")).thenReturn(1L);
            when(jdbc.filas.getLong("ingrediente_id")).thenReturn(4L);
            when(jdbc.filas.getBigDecimal("stock_actual")).thenReturn(new BigDecimal("2.0000"));
            when(jdbc.filas.getBigDecimal("stock_minimo")).thenReturn(new BigDecimal("5.0000"));
            when(jdbc.filas.getBigDecimal("cantidad_sugerida")).thenReturn(new BigDecimal("3.0000"));
            when(jdbc.filas.getString("estado")).thenReturn("PENDIENTE");
            when(jdbc.filas.getTimestamp("created_at")).thenReturn(new Timestamp(System.currentTimeMillis()));

            var resultado = new AlertaInventarioRepository().findPendienteByIngredienteId(4L);

            // Etapa de assert
            assertTrue(resultado.isPresent());
            var alerta = resultado.get();
            assertEquals(1L, alerta.getId());
            assertEquals(4L, alerta.getIngredienteId());
            assertEquals(0, alerta.getStockActual().compareTo(new BigDecimal("2.0000")));
            assertTrue(alerta.estaPendiente());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}