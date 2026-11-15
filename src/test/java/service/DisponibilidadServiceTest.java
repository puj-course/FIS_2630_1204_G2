package service;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisponibilidadServiceTest {
    @Test
    void marcaDisponibleUnPlatoSinInsumosRequeridos() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            new DisponibilidadService().actualizarDisponibilidad(7);
            verify(jdbc.sentencia).setString(1, "DISPONIBLE");
            verify(jdbc.sentencia).setInt(2, 7);
            verify(jdbc.sentencia).executeUpdate();
        }
    }
}

