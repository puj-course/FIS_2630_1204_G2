package service.descuento;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DescuentoMontoFijoTest {
    @Test
    void redondeaElMontoYRechazaSuperarSubtotal() throws Exception {
        var estrategia = new DescuentoMontoFijo();
        assertEquals(new BigDecimal("1.01"), estrategia.calcular(BigDecimal.TEN, new BigDecimal("1.005")));
        assertThrows(IllegalArgumentException.class, () -> estrategia.calcular(BigDecimal.TEN, BigDecimal.valueOf(11)));
    }
}

