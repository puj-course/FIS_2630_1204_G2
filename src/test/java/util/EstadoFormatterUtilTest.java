package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El formateador es lo que traduce los codigos internos a lo que ve el usuario
 * en el mapa del salon y en el tablero de cocina.
 */
class EstadoFormatterUtilTest {

    @Test
    void traduceLosEstadosDeMesa() throws Exception {
        assertEquals("Disponible", EstadoFormatterUtil.formatearEstadoMesa("DISPONIBLE"));
    }

    // ------------------------------------------------- estados de la mesa --

    @Test
    @DisplayName("traduce los cinco estados de mesa")
    void traduceLosCincoEstados() {
        assertEquals("Disponible",        EstadoFormatterUtil.formatearEstadoMesa("DISPONIBLE"));
        assertEquals("Ocupada",           EstadoFormatterUtil.formatearEstadoMesa("OCUPADA"));
        assertEquals("Reservada",         EstadoFormatterUtil.formatearEstadoMesa("RESERVADA"));
        assertEquals("Pendiente de pago", EstadoFormatterUtil.formatearEstadoMesa("PENDIENTE_PAGO"));
        assertEquals("Inhabilitada",      EstadoFormatterUtil.formatearEstadoMesa("MANTENIMIENTO"));
    }

    @Test
    @DisplayName("acepta los sinonimos que conviven en la base")
    void aceptaLosSinonimos() {
        assertEquals("Disponible",        EstadoFormatterUtil.formatearEstadoMesa("LIBRE"));
        assertEquals("Pendiente de pago", EstadoFormatterUtil.formatearEstadoMesa("PENDIENTE_DE_PAGO"));
        assertEquals("Inhabilitada",      EstadoFormatterUtil.formatearEstadoMesa("INHABILITADA"));
        assertEquals("Inhabilitada",      EstadoFormatterUtil.formatearEstadoMesa("INHABILITADO"));
    }

    @Test
    @DisplayName("no le importan mayusculas, espacios ni guiones")
    void normalizaLaEntrada() {
        assertEquals("Disponible", EstadoFormatterUtil.formatearEstadoMesa("disponible"));
        assertEquals("Disponible", EstadoFormatterUtil.formatearEstadoMesa("  DISPONIBLE  "));
        assertEquals("Pendiente de pago", EstadoFormatterUtil.formatearEstadoMesa("pendiente pago"));
        assertEquals("Pendiente de pago", EstadoFormatterUtil.formatearEstadoMesa("pendiente-pago"));
    }

    @Test
    @DisplayName("sin estado muestra Desconocido en vez de quedarse en blanco")
    void sinEstadoMuestraDesconocido() {
        assertEquals("Desconocido", EstadoFormatterUtil.formatearEstadoMesa(null));
        assertEquals("Desconocido", EstadoFormatterUtil.formatearEstadoMesa(""));
        assertEquals("Desconocido", EstadoFormatterUtil.formatearEstadoMesa("   "));
    }

    @Test
    @DisplayName("un estado que no conoce lo devuelve en minuscula, sin reventar")
    void estadoDesconocidoNoRevienta() {
        assertEquals("en_limpieza", EstadoFormatterUtil.formatearEstadoMesa("EN_LIMPIEZA"));
        assertEquals("cualquier cosa", EstadoFormatterUtil.formatearEstadoMesa("Cualquier Cosa"));
    }

    // ----------------------------------------------- estados del pedido --

    @Test
    @DisplayName("traduce los seis estados de pedido")
    void traduceLosEstadosDePedido() {
        assertEquals("Pendiente",        EstadoFormatterUtil.formatearEstadoPedido("PENDIENTE"));
        assertEquals("Asignado a mesa",  EstadoFormatterUtil.formatearEstadoPedido("ASIGNADO_MESA"));
        assertEquals("En preparación",   EstadoFormatterUtil.formatearEstadoPedido("EN_PREPARACION"));
        assertEquals("Completado",       EstadoFormatterUtil.formatearEstadoPedido("COMPLETADO"));
        assertEquals("Entregado",        EstadoFormatterUtil.formatearEstadoPedido("ENTREGADO"));
        assertEquals("Cancelado",        EstadoFormatterUtil.formatearEstadoPedido("CANCELADO"));
    }

    @Test
    @DisplayName("tambien normaliza la entrada del pedido")
    void normalizaElEstadoDelPedido() {
        assertEquals("En preparación", EstadoFormatterUtil.formatearEstadoPedido("en preparacion"));
        assertEquals("En preparación", EstadoFormatterUtil.formatearEstadoPedido("  EN-PREPARACION "));
    }

    @Test
    @DisplayName("sin estado de pedido tambien muestra Desconocido")
    void sinEstadoDePedidoMuestraDesconocido() {
        assertEquals("Desconocido", EstadoFormatterUtil.formatearEstadoPedido(null));
        assertEquals("Desconocido", EstadoFormatterUtil.formatearEstadoPedido(""));
        assertEquals("Desconocido", EstadoFormatterUtil.formatearEstadoPedido("  "));
    }

    @Test
    @DisplayName("un estado de pedido desconocido se devuelve tal cual")
    void estadoDePedidoDesconocidoSeDevuelveIgual() {
        // A diferencia del de mesa, este no lo pasa a minuscula.
        assertEquals("PAGADO", EstadoFormatterUtil.formatearEstadoPedido("PAGADO"));
        assertEquals("Lo Que Sea", EstadoFormatterUtil.formatearEstadoPedido("Lo Que Sea"));
    }

    @Test
    @DisplayName("ninguna traduccion devuelve nulo")
    void ningunaTraduccionDevuelveNulo() {
        String[] entradas = {null, "", "  ", "DISPONIBLE", "OCUPADA", "RARO", "PENDIENTE", "EN_PREPARACION"};
        for (String e : entradas) {
            assertNotNull(EstadoFormatterUtil.formatearEstadoMesa(e), "mesa con entrada " + e);
            assertNotNull(EstadoFormatterUtil.formatearEstadoPedido(e), "pedido con entrada " + e);
        }
    }
}
