package dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CuentaPago es lo que ve el cajero en la pantalla de pago: el subtotal, los
 * impuestos, el descuento y la propina, y el total que resulta de sumarlos.
 */
class CuentaPagoTest {

    private CuentaPago cuentaDe(String subtotal, String impuestos, String descuento, String propina) {
        return new CuentaPago(1L, "PED-0001", 5,
                subtotal == null ? null : new BigDecimal(subtotal),
                impuestos == null ? null : new BigDecimal(impuestos),
                descuento == null ? null : new BigDecimal(descuento),
                propina == null ? null : new BigDecimal(propina),
                false);
    }

    @Test
    void conservaLosDatosDelPedido() throws Exception {
        var cuenta = cuentaDe("30000", "5700", "0", "0");
        assertEquals(1L, cuenta.getPedidoId());
        assertEquals("PED-0001", cuenta.getNumeroPedido());
        assertEquals(5, cuenta.getNumeroMesa());
    }

    // -------------------------------------------------------------- total --

    @Test
    @DisplayName("el total suma impuestos y propina y resta el descuento")
    void elTotalSumaYResta() {
        // 30000 + 5700 + 3000 - 2000
        assertEquals(new BigDecimal("36700"), cuentaDe("30000", "5700", "2000", "3000").calcularTotal());
    }

    @Test
    @DisplayName("sin descuento ni propina el total es subtotal mas impuestos")
    void sinExtrasElTotalEsSubtotalMasImpuestos() {
        assertEquals(new BigDecimal("35700"), cuentaDe("30000", "5700", "0", "0").calcularTotal());
    }

    @Test
    @DisplayName("un descuento igual al subtotal deja solo los impuestos y la propina")
    void descuentoCompleto() {
        assertEquals(new BigDecimal("5700"), cuentaDe("30000", "5700", "30000", "0").calcularTotal());
    }

    @Test
    @DisplayName("el total se recalcula cuando el cajero cambia el descuento")
    void elTotalSeRecalculaAlCambiarElDescuento() {
        var cuenta = cuentaDe("30000", "5700", "0", "0");
        assertEquals(new BigDecimal("35700"), cuenta.calcularTotal());

        cuenta.setDescuento(new BigDecimal("5000"));
        assertEquals(new BigDecimal("30700"), cuenta.calcularTotal());
    }

    @Test
    @DisplayName("el total se recalcula cuando el cajero agrega propina")
    void elTotalSeRecalculaAlAgregarPropina() {
        var cuenta = cuentaDe("30000", "5700", "0", "0");
        cuenta.setPropina(new BigDecimal("3000"));
        assertEquals(new BigDecimal("38700"), cuenta.calcularTotal());
    }

    // -------------------------------------------------------- valor nulo --

    @Test
    @DisplayName("los valores que llegan nulos de la base se vuelven cero")
    void losNulosSeVuelvenCero() {
        // Sin esto el calculo del total reventaria con NullPointerException.
        var cuenta = cuentaDe("30000", null, null, null);
        assertEquals(BigDecimal.ZERO, cuenta.getImpuestos());
        assertEquals(BigDecimal.ZERO, cuenta.getDescuento());
        assertEquals(BigDecimal.ZERO, cuenta.getPropina());
        assertEquals(new BigDecimal("30000"), cuenta.calcularTotal());
    }

    @Test
    @DisplayName("un subtotal nulo tampoco rompe el calculo")
    void elSubtotalNuloTampocoRompe() {
        var cuenta = cuentaDe(null, "5700", "0", "0");
        assertEquals(BigDecimal.ZERO, cuenta.getSubtotal());
        assertEquals(new BigDecimal("5700"), cuenta.calcularTotal());
    }

    @Test
    @DisplayName("poner el descuento en nulo lo deja en cero, no lo deja nulo")
    void ponerDescuentoNuloLoDejaEnCero() {
        var cuenta = cuentaDe("30000", "5700", "2000", "0");
        cuenta.setDescuento(null);
        assertEquals(BigDecimal.ZERO, cuenta.getDescuento());
        assertEquals(new BigDecimal("35700"), cuenta.calcularTotal());
    }

    @Test
    @DisplayName("poner la propina en nulo la deja en cero")
    void ponerPropinaNulaLaDejaEnCero() {
        var cuenta = cuentaDe("30000", "5700", "0", "3000");
        cuenta.setPropina(null);
        assertEquals(BigDecimal.ZERO, cuenta.getPropina());
    }

    // ------------------------------------------------------------- estado --

    @Test
    @DisplayName("sabe si la cuenta ya se pago")
    void sabeSiYaSePago() {
        var pagada = new CuentaPago(2L, "PED-0002", 7, BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true);
        assertTrue(pagada.isPagada());
        assertFalse(cuentaDe("10", "0", "0", "0").isPagada());
    }

    @Test
    @DisplayName("una cuenta para llevar no tiene numero de mesa")
    void unaCuentaParaLlevarNoTieneMesa() {
        var sinMesa = new CuentaPago(3L, "PED-0003", null, new BigDecimal("12000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
        assertNull(sinMesa.getNumeroMesa());
        assertEquals(new BigDecimal("12000"), sinMesa.calcularTotal());
    }

    @Test
    @DisplayName("el descuento mayor que la cuenta deja un total negativo")
    void unDescuentoExageradoDejaTotalNegativo() {
        // Queda documentado: el DTO no valida el tope, y quien arma la
        // pantalla de pago es el que tiene que evitarlo.
        var cuenta = cuentaDe("10000", "0", "50000", "0");
        assertEquals(-1, cuenta.calcularTotal().signum());
    }
}
