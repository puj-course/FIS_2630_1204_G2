package enums;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TipoDescuentoTest {
    @Test
    void calculaYRedondeaAmbosTipos() throws Exception {
        assertEquals(new BigDecimal("1.01"), TipoDescuento.MONTO_FIJO.calcular(BigDecimal.TEN, new BigDecimal("1.005")));
        assertEquals(new BigDecimal("1.25"), TipoDescuento.PORCENTAJE.calcular(BigDecimal.TEN, new BigDecimal("12.5")));
    }
}

