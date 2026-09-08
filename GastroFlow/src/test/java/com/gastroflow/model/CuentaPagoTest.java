package com.gastroflow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("HU-35 — cálculo del total de la cuenta")
class CuentaPagoTest {

    private CuentaPago cuenta(String subtotal, String impuestos, String descuento, String propina) {
        return new CuentaPago(
                1L, "PED-001", 5,
                new BigDecimal(subtotal),
                new BigDecimal(impuestos),
                new BigDecimal(descuento),
                new BigDecimal(propina),
                false);
    }

    @Test
    @DisplayName("total = subtotal + impuestos + propina − descuento")
    void formulaCompleta() {
        // El mismo caso que valida ck_pagos_total_coherente en la base.
        CuentaPago c = cuenta("50000", "9500", "5000", "6000");
        assertEquals(0, new BigDecimal("60500").compareTo(c.calcularTotal()));
    }

    @Test
    @DisplayName("sin propina ni descuento el total es subtotal + impuestos")
    void sinPropinaNiDescuento() {
        CuentaPago c = cuenta("50000", "9500", "0", "0");
        assertEquals(0, new BigDecimal("59500").compareTo(c.calcularTotal()));
    }

    @Test
    @DisplayName("la propina suma después de aplicar el descuento")
    void propinaNoSeDescuenta() {
        CuentaPago c = cuenta("100000", "0", "10000", "10000");
        assertEquals(0, new BigDecimal("100000").compareTo(c.calcularTotal()));
    }

    @Test
    @DisplayName("un descuento igual al subtotal deja solo impuestos y propina")
    void descuentoIgualAlSubtotal() {
        CuentaPago c = cuenta("30000", "5700", "30000", "3000");
        assertEquals(0, new BigDecimal("8700").compareTo(c.calcularTotal()));
    }

    @Test
    @DisplayName("los decimales no se pierden")
    void conservaDecimales() {
        CuentaPago c = cuenta("12345.67", "2345.68", "1000.01", "500.02");
        assertEquals(0, new BigDecimal("14191.36").compareTo(c.calcularTotal()));
    }

    @Test
    @DisplayName("un valor nulo se trata como cero y no lanza excepción")
    void valoresNulos() {
        CuentaPago c = new CuentaPago(1L, "PED-002", null,
                new BigDecimal("10000"), null, null, null, false);
        assertEquals(0, new BigDecimal("10000").compareTo(c.calcularTotal()));
    }
}
