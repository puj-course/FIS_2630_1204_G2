package service;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdicionalServiceTest {
    @Test
    void multiplicaSobrecostoPorCantidad() throws Exception {
        var adicional = new entity.Adicional();
        var servicio = new AdicionalService();
        assertEquals(BigDecimal.ZERO, servicio.calcularSobrecosto(adicional, 3));
        adicional.setPrecioAdicional(new BigDecimal("2.50"));
        assertEquals(new BigDecimal("7.50"), servicio.calcularSobrecosto(adicional, 3));
    }
}

