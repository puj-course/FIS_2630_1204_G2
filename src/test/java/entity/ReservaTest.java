package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reserva es una mesa apartada para una fecha y una hora. Mientras siga
 * activa, esa mesa no se le puede dar a otro.
 */
class ReservaTest {

    private Reserva reservaDe(String cliente, int personas, boolean activa) {
        var r = new Reserva();
        r.setIdMesa(4);
        r.setNumeroMesa(12);
        r.setNombreCliente(cliente);
        r.setCantidadPersonas(personas);
        r.setFechaHoraReserva(LocalDateTime.of(2026, 11, 15, 20, 0));
        r.setActiva(activa);
        return r;
    }

    @Test
    void conservaElClienteYLaFecha() throws Exception {
        var r = reservaDe("Carlos Ruiz", 4, true);
        assertEquals("Carlos Ruiz", r.getNombreCliente());
        assertEquals(LocalDateTime.of(2026, 11, 15, 20, 0), r.getFechaHoraReserva());
    }

    @Test
    @DisplayName("guarda que mesa quedo apartada y en que zona")
    void guardaLaMesaYLaZona() {
        var r = reservaDe("Carlos Ruiz", 4, true);
        r.setNombreZona("Terraza");
        assertEquals(4, r.getIdMesa());
        assertEquals(12, r.getNumeroMesa());
        assertEquals("Terraza", r.getNombreZona());
    }

    @Test
    @DisplayName("una reserva activa no tiene fecha de cancelacion")
    void laActivaNoTieneFechaDeCancelacion() {
        var r = reservaDe("Carlos Ruiz", 4, true);
        assertTrue(r.isActiva());
        assertNull(r.getFechaCancelacion());
    }

    @Test
    @DisplayName("al cancelarla se apaga y se le pone la fecha")
    void alCancelarlaSeApagaYSeFecha() {
        var r = reservaDe("Carlos Ruiz", 4, true);
        r.setActiva(false);
        r.setFechaCancelacion(LocalDateTime.of(2026, 11, 14, 10, 0));

        assertFalse(r.isActiva());
        assertNotNull(r.getFechaCancelacion());
    }

    @Test
    @DisplayName("la cancelacion queda antes de la hora reservada")
    void laCancelacionVaAntesDeLaReserva() {
        var r = reservaDe("Carlos Ruiz", 4, true);
        r.setActiva(false);
        r.setFechaCancelacion(LocalDateTime.of(2026, 11, 14, 10, 0));
        assertTrue(r.getFechaCancelacion().isBefore(r.getFechaHoraReserva()));
    }

    @Test
    @DisplayName("una reserva nueva nace inactiva y vacia")
    void naceInactivaYVacia() {
        // El booleano arranca en false: hay que marcarla activa a proposito.
        var r = new Reserva();
        assertFalse(r.isActiva());
        assertNull(r.getNombreCliente());
        assertNull(r.getFechaHoraReserva());
        assertEquals(0, r.getCantidadPersonas());
    }

    @Test
    @DisplayName("guarda para cuantas personas es")
    void guardaParaCuantasPersonas() {
        assertEquals(6, reservaDe("Carlos Ruiz", 6, true).getCantidadPersonas());
    }

    @Test
    @DisplayName("al leerla de la base trae su identificador")
    void traeSuIdentificador() {
        var r = reservaDe("Carlos Ruiz", 4, true);
        r.setIdReserva(31);
        assertEquals(31, r.getIdReserva());
    }
}
