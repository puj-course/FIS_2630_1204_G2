package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReglaDescuentoTest {
    @Test
    void conservaValor() throws Exception {
        var registro = new ReglaDescuento();
        registro.setValor(12.5);
        assertEquals(12.5, registro.getValor());
    }
}

