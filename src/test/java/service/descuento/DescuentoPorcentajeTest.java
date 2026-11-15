package service.descuento;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DescuentoPorcentajeTest {
    @Test
    void calculaPorcentajeYRechazaMasDelCienPorCiento() throws Exception {
        var estrategia = new DescuentoPorcentaje();
        assertEquals(new BigDecimal("12.50"), estrategia.calcular(BigDecimal.valueOf(100), new BigDecimal("12.5")));
        assertThrows(IllegalArgumentException.class, () -> estrategia.calcular(BigDecimal.TEN, BigDecimal.valueOf(101)));
    }
}

