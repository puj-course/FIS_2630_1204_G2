package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReservaTest {
    @Test
    void conservaLaCancelacion() throws Exception {
        var reserva = new Reserva();
        reserva.setActiva(true);
        assertTrue(reserva.isActiva());
        reserva.setActiva(false);
        var fecha = java.time.LocalDateTime.of(2026, 1, 1, 12, 30);
        reserva.setFechaCancelacion(fecha);
        assertFalse(reserva.isActiva());
        assertEquals(fecha, reserva.getFechaCancelacion());
    }
}

