package repository;

import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListaEsperaRepositoryTest {
    @Test
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            assertTrue(new ListaEsperaRepository().findByEstadoOrderByHoraIngresoAsc(enums.EstadoListaEspera.EN_ESPERA).isEmpty());
            verify(jdbc.sentencia).setString(1, "EN_ESPERA");
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}

