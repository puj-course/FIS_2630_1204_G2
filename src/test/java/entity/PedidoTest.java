package entity;

import enums.EstadoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pedido es la comanda. Nace en PENDIENTE, con los totales en cero y el
 * inventario sin descontar, que es lo que espera el resto del sistema.
 */
class PedidoTest {

    @Test
    void conservaElNumeroDePedido() throws Exception {
        var pedido = new Pedido();
        pedido.setNumeroPedido("PED-0007");
        assertEquals("PED-0007", pedido.getNumeroPedido());
    }

    // --------------------------------------------------- como nace --

    @Test
    @DisplayName("un pedido nuevo arranca en PENDIENTE")
    void arrancaEnPendiente() {
        assertEquals(EstadoPedido.PENDIENTE, new Pedido().getEstado());
    }

    @Test
    @DisplayName("un pedido nuevo arranca con los totales en cero")
    void arrancaConTotalesEnCero() {
        var pedido = new Pedido();
        assertEquals(BigDecimal.ZERO, pedido.getSubtotal());
        assertEquals(BigDecimal.ZERO, pedido.getImpuestos());
        assertEquals(BigDecimal.ZERO, pedido.getTotal());
    }

    @Test
    @DisplayName("un pedido nuevo arranca con la lista de productos vacia, no nula")
    void arrancaConLaListaVacia() {
        // Es un JSON que viaja a la columna productos: si fuera nulo la
        // restriccion ck_pedido_productos_json lo rechazaria.
        assertEquals("[]", new Pedido().getProductos());
    }

    @Test
    @DisplayName("un pedido nuevo todavia no descuenta inventario")
    void arrancaSinDescontarInventario() {
        assertFalse(new Pedido().isInventarioDescontado());
    }

    @Test
    @DisplayName("un pedido nuevo se marca con la fecha del momento")
    void arrancaConLaFechaDelMomento() {
        var antes = LocalDateTime.now().minusSeconds(5);
        var pedido = new Pedido();
        assertNotNull(pedido.getFechaPedido());
        assertTrue(pedido.getFechaPedido().isAfter(antes));
    }

    // -------------------------------------------------------- impuestos --

    @Test
    @DisplayName("poner los impuestos en nulo los deja en cero")
    void impuestosNulosQuedanEnCero() {
        // Es el unico campo con esa proteccion, y existe porque la columna
        // admite nulos y el calculo del total no los soporta.
        var pedido = new Pedido();
        pedido.setImpuestos(new BigDecimal("5700"));
        pedido.setImpuestos(null);
        assertEquals(BigDecimal.ZERO, pedido.getImpuestos());
    }

    @Test
    @DisplayName("los impuestos normales se guardan tal cual")
    void losImpuestosNormalesSeGuardan() {
        var pedido = new Pedido();
        pedido.setImpuestos(new BigDecimal("5700.00"));
        assertEquals(new BigDecimal("5700.00"), pedido.getImpuestos());
    }

    @Test
    @DisplayName("el subtotal y el total si aceptan nulo, a diferencia de los impuestos")
    void subtotalYTotalSiAceptanNulo() {
        // Queda documentado para que nadie suponga que los tres se comportan
        // igual.
        var pedido = new Pedido();
        pedido.setSubtotal(null);
        pedido.setTotal(null);
        assertNull(pedido.getSubtotal());
        assertNull(pedido.getTotal());
    }

    // ----------------------------------------------------------- avance --

    @Test
    @DisplayName("el pedido avanza de estado cuando cocina lo toma")
    void avanzaDeEstado() {
        var pedido = new Pedido();
        pedido.setEstado(EstadoPedido.EN_PREPARACION);
        assertEquals(EstadoPedido.EN_PREPARACION, pedido.getEstado());

        pedido.setEstado(EstadoPedido.ENTREGADO);
        assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
    }

    @Test
    @DisplayName("se marca una sola vez que el inventario ya se descontó")
    void seMarcaElInventarioDescontado() {
        var pedido = new Pedido();
        pedido.setInventarioDescontado(true);
        assertTrue(pedido.isInventarioDescontado());
    }

    @Test
    @DisplayName("guarda a que mesa y a que mesero pertenece")
    void guardaMesaYMesero() {
        var pedido = new Pedido();
        pedido.setMesaId(4L);
        pedido.setUsuarioId(9L);
        assertEquals(4L, pedido.getMesaId());
        assertEquals(9L, pedido.getUsuarioId());
    }

    @Test
    @DisplayName("un pedido para llevar puede no tener mesa")
    void puedeNoTenerMesa() {
        var pedido = new Pedido();
        pedido.setMesaId(null);
        assertNull(pedido.getMesaId());
    }

    @Test
    @DisplayName("guarda el JSON de productos que se escribe en la base")
    void guardaElJsonDeProductos() {
        var pedido = new Pedido();
        pedido.setProductos("[{\"codigo\":\"PLA-001\",\"cantidad\":2}]");
        assertTrue(pedido.getProductos().contains("PLA-001"));
    }
}
