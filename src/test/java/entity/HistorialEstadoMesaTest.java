package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HistorialEstadoMesa deja constancia de cada cambio de estado de una mesa:
 * de que estado venia, a cual paso y por que.
 */
class HistorialEstadoMesaTest {

    private HistorialEstadoMesa cambio(Long desde, Long hasta, String motivo) {
        var h = new HistorialEstadoMesa();
        h.setMesaId(4L);
        h.setEstadoAnteriorId(desde);
        h.setEstadoNuevoId(hasta);
        h.setMotivo(motivo);
        return h;
    }

    @Test
    void conservaElCambioDeEstado() throws Exception {
        var h = cambio(1L, 2L, "Llego el grupo de la reserva");
        assertEquals(1L, h.getEstadoAnteriorId());
        assertEquals(2L, h.getEstadoNuevoId());
    }

    @Test
    @DisplayName("el registro se marca solo con la fecha del cambio")
    void seMarcaSoloConLaFecha() {
        var antes = LocalDateTime.now().minusSeconds(5);
        assertTrue(new HistorialEstadoMesa().getFechaCambio().isAfter(antes));
    }

    @Test
    @DisplayName("guarda por que se hizo el cambio")
    void guardaElMotivo() {
        assertEquals("Llego el grupo de la reserva",
                cambio(1L, 2L, "Llego el grupo de la reserva").getMotivo());
    }

    @Test
    @DisplayName("el primer cambio de una mesa no tiene estado anterior")
    void elPrimerCambioNoTieneAnterior() {
        var h = cambio(null, 1L, "Mesa creada");
        assertNull(h.getEstadoAnteriorId());
        assertNotNull(h.getEstadoNuevoId());
    }

    @Test
    @DisplayName("el estado anterior y el nuevo tienen que ser distintos para que valga la pena")
    void elCambioLlevaDosEstadosDistintos() {
        var h = cambio(1L, 2L, "Se ocupo");
        assertNotEquals(h.getEstadoAnteriorId(), h.getEstadoNuevoId());
    }

    @Test
    @DisplayName("siempre dice de que mesa se trata")
    void siempreDiceDeQueMesa() {
        assertEquals(4L, cambio(1L, 2L, "x").getMesaId());
    }

    @Test
    @DisplayName("se puede fijar la fecha a mano al leerlo de la base")
    void seFijaLaFechaAlLeerlo() {
        var cuando = LocalDateTime.of(2026, 11, 10, 19, 45);
        var h = cambio(1L, 2L, "x");
        h.setFechaCambio(cuando);
        assertEquals(cuando, h.getFechaCambio());
    }

    @Test
    @DisplayName("el motivo es opcional: hay cambios que no necesitan explicacion")
    void elMotivoEsOpcional() {
        assertNull(cambio(1L, 2L, null).getMotivo());
    }
}
