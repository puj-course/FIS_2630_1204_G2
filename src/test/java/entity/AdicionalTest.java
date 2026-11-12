package entity;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdicionalTest {
    @Test
    void conservaPrecioAdicional() throws Exception {
        var registro = new Adicional();
        registro.setPrecioAdicional(new BigDecimal("2.50"));
        assertEquals(new BigDecimal("2.50"), registro.getPrecioAdicional());
    }
}

