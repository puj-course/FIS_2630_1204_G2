package entity;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertaInventarioTest {
    @Test
    void conservaCantidadSugerida() throws Exception {
        var registro = new AlertaInventario();
        registro.setCantidadSugerida(new BigDecimal("7.5"));
        assertEquals(new BigDecimal("7.5"), registro.getCantidadSugerida());
    }
}

