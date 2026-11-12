package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DetallePedido es cada renglon de un pedido ya guardado. Recalcula el
 * subtotal solo, cada vez que cambia la cantidad o el precio.
 */
class DetallePedidoTest {

    private DetallePedido renglonDe(int cantidad, String precio) {
        var d = new DetallePedido();
        d.setPrecioUnitario(new BigDecimal(precio));
        d.setCantidad(cantidad);
        return d;
    }

    @Test
    void conservaLosIdentificadores() throws Exception {
        var d = new DetallePedido();
        d.setId(1L);
        d.setPedidoId(10L);
        d.setProductoId(20L);
        assertEquals(1L, d.getId());
        assertEquals(10L, d.getPedidoId());
        assertEquals(20L, d.getProductoId());
    }

    // ----------------------------------------------- subtotal automatico --

    @Test
    @DisplayName("el subtotal se calcula solo al poner la cantidad")
    void elSubtotalSeCalculaSolo() {
        assertEquals(new BigDecimal("96000"), renglonDe(3, "32000").getSubtotal());
    }

    @Test
    @DisplayName("cambiar la cantidad vuelve a calcular el subtotal")
    void cambiarLaCantidadRecalcula() {
        var d = renglonDe(2, "10000");
        assertEquals(new BigDecimal("20000"), d.getSubtotal());

        d.setCantidad(5);
        assertEquals(new BigDecimal("50000"), d.getSubtotal());
    }

    @Test
    @DisplayName("cambiar el precio tambien vuelve a calcular el subtotal")
    void cambiarElPrecioRecalcula() {
        var d = renglonDe(4, "5000");
        assertEquals(new BigDecimal("20000"), d.getSubtotal());

        d.setPrecioUnitario(new BigDecimal("6000"));
        assertEquals(new BigDecimal("24000"), d.getSubtotal());
    }

    @Test
    @DisplayName("el orden en que se ponen precio y cantidad no cambia el resultado")
    void elOrdenNoImporta() {
        var primeroElPrecio = new DetallePedido();
        primeroElPrecio.setPrecioUnitario(new BigDecimal("8000"));
        primeroElPrecio.setCantidad(3);

        var primeroLaCantidad = new DetallePedido();
        primeroLaCantidad.setCantidad(3);
        primeroLaCantidad.setPrecioUnitario(new BigDecimal("8000"));

        assertEquals(primeroElPrecio.getSubtotal(), primeroLaCantidad.getSubtotal());
    }

    @Test
    @DisplayName("el subtotal respeta los centavos del precio")
    void respetaLosCentavos() {
        assertEquals(new BigDecimal("15001.00"), renglonDe(2, "7500.50").getSubtotal());
    }

    // -------------------------------------------------- casos incompletos --

    @Test
    @DisplayName("sin precio el subtotal queda en cero, no en nulo")
    void sinPrecioElSubtotalEsCero() {
        var d = new DetallePedido();
        d.setCantidad(3);
        assertEquals(BigDecimal.ZERO, d.getSubtotal());
    }

    @Test
    @DisplayName("con cantidad cero el subtotal tambien queda en cero")
    void conCantidadCeroElSubtotalEsCero() {
        var d = new DetallePedido();
        d.setPrecioUnitario(new BigDecimal("32000"));
        assertEquals(BigDecimal.ZERO, d.getSubtotal());
    }

    @Test
    @DisplayName("bajar la cantidad a cero vuelve el subtotal a cero")
    void bajarLaCantidadACeroLimpiaElSubtotal() {
        var d = renglonDe(3, "32000");
        d.setCantidad(0);
        assertEquals(BigDecimal.ZERO, d.getSubtotal());
    }

    @Test
    @DisplayName("una cantidad negativa deja el subtotal en cero")
    void cantidadNegativaDejaCero() {
        // Queda documentado: la entidad no rechaza el numero negativo, solo
        // se niega a calcular con el.
        var d = renglonDe(3, "32000");
        d.setCantidad(-2);
        assertEquals(-2, d.getCantidad());
        assertEquals(BigDecimal.ZERO, d.getSubtotal());
    }

    @Test
    @DisplayName("quitar el precio deja el subtotal en cero")
    void quitarElPrecioLimpiaElSubtotal() {
        var d = renglonDe(3, "32000");
        d.setPrecioUnitario(null);
        assertNull(d.getPrecioUnitario());
        assertEquals(BigDecimal.ZERO, d.getSubtotal());
    }

    @Test
    @DisplayName("el subtotal tambien se puede escribir a mano, por si la base trae otro")
    void elSubtotalSePuedeEscribirAMano() {
        var d = renglonDe(3, "32000");
        d.setSubtotal(new BigDecimal("90000"));
        assertEquals(new BigDecimal("90000"), d.getSubtotal());
    }
}
