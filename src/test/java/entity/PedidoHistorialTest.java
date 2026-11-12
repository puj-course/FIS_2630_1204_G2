package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PedidoHistorial es la bitacora de un pedido: cuando se abrio, cuando paso a
 * cocina, cuando se cerro y quien hizo cada cosa.
 */
class PedidoHistorialTest {

    private PedidoHistorial evento(String tipo, String descripcion) {
        var h = new PedidoHistorial();
        h.setPedidoId(7L);
        h.setTipoEvento(tipo);
        h.setDescripcion(descripcion);
        h.setUsuarioId(9L);
        return h;
    }

    @Test
    void conservaElTipoDeEvento() throws Exception {
        assertEquals("APERTURA_PEDIDO", evento("APERTURA_PEDIDO", "x").getTipoEvento());
    }

    @Test
    @DisplayName("guarda los eventos que marcan la vida del pedido")
    void guardaLosEventosDelPedido() {
        for (String tipo : new String[]{"APERTURA_PEDIDO", "CIERRE_PEDIDO", "CANCELACION_PEDIDO"}) {
            assertEquals(tipo, evento(tipo, "x").getTipoEvento());
        }
    }

    @Test
    @DisplayName("siempre dice de que pedido y de que usuario se trata")
    void diceDeQuePedidoYDeQuienFue() {
        var e = evento("CIERRE_PEDIDO", "Pedido cerrado por el cajero");
        assertEquals(7L, e.getPedidoId());
        assertEquals(9L, e.getUsuarioId());
    }

    @Test
    @DisplayName("la descripcion explica el evento en palabras")
    void laDescripcionExplicaElEvento() {
        var e = evento("CANCELACION_PEDIDO", "El cliente se retiro antes de que saliera la comida");
        assertTrue(e.getDescripcion().contains("cliente"));
    }

    @Test
    @DisplayName("un evento nuevo no trae fecha hasta que se guarda")
    void naceSinFecha() {
        assertNull(new PedidoHistorial().getFechaEvento());
    }

    @Test
    @DisplayName("al leerlo de la base trae la fecha del evento")
    void alLeerloTraeLaFecha() {
        var cuando = LocalDateTime.of(2026, 11, 10, 20, 5);
        var e = evento("APERTURA_PEDIDO", "x");
        e.setFechaEvento(cuando);
        assertEquals(cuando, e.getFechaEvento());
    }

    @Test
    @DisplayName("un evento nuevo nace sin ningun dato")
    void naceVacio() {
        var e = new PedidoHistorial();
        assertNull(e.getId());
        assertNull(e.getPedidoId());
        assertNull(e.getTipoEvento());
        assertNull(e.getUsuarioId());
    }

    @Test
    @DisplayName("dos eventos del mismo pedido se distinguen por el tipo")
    void dosEventosSeDistinguenPorElTipo() {
        var apertura = evento("APERTURA_PEDIDO", "x");
        var cierre = evento("CIERRE_PEDIDO", "y");
        assertEquals(apertura.getPedidoId(), cierre.getPedidoId());
        assertNotEquals(apertura.getTipoEvento(), cierre.getTipoEvento());
    }
}
