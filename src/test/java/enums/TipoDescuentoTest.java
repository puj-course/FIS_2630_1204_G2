package enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TipoDescuento es el Strategy que usa la caja: cada constante calcula el
 * descuento a su manera y las dos devuelven pesos con dos decimales.
 */
class TipoDescuentoTest {

    @Test
    void tieneLosDosTipos() throws Exception {
        assertEquals(2, TipoDescuento.values().length);
    }

    // ----------------------------------------------------- monto fijo --

    @Test
    @DisplayName("el monto fijo descuenta lo que escribio el cajero")
    void elMontoFijoDescuentaLoEscrito() {
        assertEquals(new BigDecimal("5000.00"),
                TipoDescuento.MONTO_FIJO.calcular(new BigDecimal("32000"), new BigDecimal("5000")));
    }

    @Test
    @DisplayName("el monto fijo no mira el subtotal")
    void elMontoFijoIgnoraElSubtotal() {
        var conSubtotalAlto = TipoDescuento.MONTO_FIJO.calcular(new BigDecimal("999999"), new BigDecimal("3000"));
        var conSubtotalBajo = TipoDescuento.MONTO_FIJO.calcular(new BigDecimal("10"), new BigDecimal("3000"));
        assertEquals(conSubtotalAlto, conSubtotalBajo);
    }

    @Test
    @DisplayName("el monto fijo redondea a dos decimales")
    void elMontoFijoRedondeaADosDecimales() {
        assertEquals(new BigDecimal("1234.57"),
                TipoDescuento.MONTO_FIJO.calcular(new BigDecimal("50000"), new BigDecimal("1234.567")));
        assertEquals(2,
                TipoDescuento.MONTO_FIJO.calcular(BigDecimal.TEN, new BigDecimal("5")).scale());
    }

    // ----------------------------------------------------- porcentaje --

    @Test
    @DisplayName("el porcentaje se calcula sobre el subtotal")
    void elPorcentajeSeCalculaSobreElSubtotal() {
        assertEquals(new BigDecimal("3200.00"),
                TipoDescuento.PORCENTAJE.calcular(new BigDecimal("32000"), new BigDecimal("10")));
    }

    @Test
    @DisplayName("el quince por ciento de una cuenta de 28000")
    void calculaElQuincePorCiento() {
        assertEquals(new BigDecimal("4200.00"),
                TipoDescuento.PORCENTAJE.calcular(new BigDecimal("28000"), new BigDecimal("15")));
    }

    @Test
    @DisplayName("el cero por ciento no descuenta nada")
    void elCeroPorCientoNoDescuenta() {
        assertEquals(0, TipoDescuento.PORCENTAJE
                .calcular(new BigDecimal("50000"), BigDecimal.ZERO).compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("el cien por ciento descuenta la cuenta entera")
    void elCienPorCientoDescuentaTodo() {
        assertEquals(new BigDecimal("18000.00"),
                TipoDescuento.PORCENTAJE.calcular(new BigDecimal("18000"), new BigDecimal("100")));
    }

    @Test
    @DisplayName("el porcentaje redondea a dos decimales hacia arriba desde la mitad")
    void elPorcentajeRedondeaHalfUp() {
        // 3333 x 15 / 100 = 499.95
        assertEquals(new BigDecimal("499.95"),
                TipoDescuento.PORCENTAJE.calcular(new BigDecimal("3333"), new BigDecimal("15")));
        // 1 x 33.333 / 100 = 0.33333 -> 0.33
        assertEquals(new BigDecimal("0.33"),
                TipoDescuento.PORCENTAJE.calcular(BigDecimal.ONE, new BigDecimal("33.333")));
    }

    // ---------------------------------------------------- presentacion --

    @Test
    @DisplayName("el desplegable muestra un texto legible, no el nombre de la constante")
    void elDesplegableMuestraTextoLegible() {
        assertEquals("Monto fijo ($)", TipoDescuento.MONTO_FIJO.toString());
        assertEquals("Porcentaje (%)", TipoDescuento.PORCENTAJE.toString());
    }

    @Test
    @DisplayName("ninguna constante se muestra con su nombre interno")
    void ningunaSeMuestraConSuNombreInterno() {
        for (TipoDescuento t : TipoDescuento.values()) {
            assertNotEquals(t.name(), t.toString());
            assertFalse(t.toString().isBlank());
        }
    }

    @Test
    @DisplayName("las dos constantes responden al mismo metodo sin que el cajero sepa cual es")
    void lasDosRespondenAlMismoMetodo() {
        // Es lo que hace que sea un Strategy: el que llama no distingue.
        for (TipoDescuento t : TipoDescuento.values()) {
            var resultado = t.calcular(new BigDecimal("10000"), new BigDecimal("10"));
            assertNotNull(resultado);
            assertEquals(2, resultado.scale());
            assertTrue(resultado.signum() >= 0);
        }
    }
}
