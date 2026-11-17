package repository;

import support.JdbcSimulado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.Timestamp;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AsignacionMesaRepositoryTest {

    @Test
    @DisplayName("devuelve un Optional vacío cuando la mesa no tiene ninguna asignación activa")
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Explicitamos que el ResultSet no encuentra registros
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertTrue(new AsignacionMesaRepository().findActivaByMesaId(4L).isEmpty());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }

    @Test
    @DisplayName("devuelve la asignación activa mapeada correctamente cuando la mesa está ocupada")
    void devuelveAsignacionCuandoExisteRegistroActivo() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Simulamos que el ResultSet encuentra un registro activo
            when(jdbc.filas.next()).thenReturn(true);
            when(jdbc.filas.getLong("id")).thenReturn(10L);
            when(jdbc.filas.getLong("mesa_id")).thenReturn(4L);
            when(jdbc.filas.getString("estado")).thenReturn("ACTIVA");
            when(jdbc.filas.getTimestamp("created_at")).thenReturn(new Timestamp(System.currentTimeMillis()));

            var resultado = new AsignacionMesaRepository().findActivaByMesaId(4L);

            // Etapa de assert
            assertTrue(resultado.isPresent());
            var asignacion = resultado.get();
            assertEquals(10L, asignacion.getId());
            assertEquals(4L, asignacion.getMesaId());

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}