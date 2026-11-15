package enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EstadoPedido dice en que va un pedido desde que el mesero lo toma hasta que
 * sale de la mesa.
 *
 * Ojo con PAGADO: existe en el enum pero la base de datos no lo acepta, y eso
 * esta probado aqui abajo para que nadie lo use por accidente.
 */
class EstadoPedidoTest {

    /** Los unicos valores que deja pasar la restriccion ck_pedido_estado. */
    private static final Set<String> ACEPTADOS_POR_LA_BASE = Set.of(
            "PENDIENTE", "ASIGNADO_MESA", "EN_PREPARACION",
            "COMPLETADO", "ENTREGADO", "CANCELADO");

    @Test
    void conservaCodigosDeDominio() throws Exception {
        var codigos = Arrays.stream(EstadoPedido.values()).map(Enum::name).collect(Collectors.toSet());
        assertEquals(Set.of("PENDIENTE", "ASIGNADO_MESA", "EN_PREPARACION",
                "PAGADO", "COMPLETADO", "ENTREGADO", "CANCELADO"), codigos);
    }

    @Test
    @DisplayName("tiene exactamente siete estados")
    void tieneSieteEstados() {
        assertEquals(7, EstadoPedido.values().length);
    }

    @Test
    @DisplayName("la base de datos solo acepta seis de los siete")
    void laBaseAceptaSeisDeLosSiete() {
        assertEquals(6, ACEPTADOS_POR_LA_BASE.size());
        assertEquals(EstadoPedido.values().length - 1, ACEPTADOS_POR_LA_BASE.size());
    }

    @Test
    @DisplayName("PAGADO existe en Java pero no se puede guardar")
    void pagadoNoSePuedeGuardar() {
        // La restriccion ck_pedido_estado en db/ddl/pedidos.ddl no lo incluye.
        // Si alguien intenta guardar un pedido en PAGADO, la base lo rechaza.
        // Queda escrito aqui para que se descubra en las pruebas y no en
        // produccion.
        assertNotNull(EstadoPedido.PAGADO);
        assertFalse(ACEPTADOS_POR_LA_BASE.contains(EstadoPedido.PAGADO.name()));
    }

    @Test
    @DisplayName("los otros seis si se pueden guardar")
    void losOtrosSeisSiSeGuardan() {
        for (EstadoPedido estado : EstadoPedido.values()) {
            if (estado == EstadoPedido.PAGADO) continue;
            assertTrue(ACEPTADOS_POR_LA_BASE.contains(estado.name()),
                    "la base deberia aceptar " + estado.name());
        }
    }

    @Test
    @DisplayName("se reconstruye desde el texto que viene de la base")
    void seReconstruyeDesdeElTexto() {
        assertSame(EstadoPedido.EN_PREPARACION, EstadoPedido.valueOf("EN_PREPARACION"));
    }

    @Test
    @DisplayName("name() y valueOf() dan la vuelta completa sin perder nada")
    void laVueltaCompletaNoPierdeNada() {
        for (EstadoPedido valor : EstadoPedido.values()) {
            assertSame(valor, EstadoPedido.valueOf(valor.name()));
        }
    }

    @Test
    @DisplayName("un texto que no corresponde a ningun estado revienta de una")
    void textoDesconocidoRevienta() {
        assertThrows(IllegalArgumentException.class, () -> EstadoPedido.valueOf("CUALQUIER_COSA"));
        assertThrows(IllegalArgumentException.class, () -> EstadoPedido.valueOf("pendiente"));
    }

    @Test
    @DisplayName("ningun estado se repite")
    void ningunoSeRepite() {
        var valores = EstadoPedido.values();
        assertEquals(valores.length,
                Arrays.stream(valores).map(Enum::name).collect(Collectors.toSet()).size());
    }

    @Test
    @DisplayName("CANCELADO es el ultimo, despues de los estados normales")
    void canceladoVaAlFinal() {
        // El orden importa en las pantallas que listan los estados.
        assertEquals(EstadoPedido.values().length - 1, EstadoPedido.CANCELADO.ordinal());
        assertTrue(EstadoPedido.PENDIENTE.ordinal() < EstadoPedido.EN_PREPARACION.ordinal());
        assertTrue(EstadoPedido.EN_PREPARACION.ordinal() < EstadoPedido.ENTREGADO.ordinal());
    }
}
