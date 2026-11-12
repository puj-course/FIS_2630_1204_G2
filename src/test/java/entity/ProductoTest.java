package entity;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductoTest {
    @Test
    void reconoceDisponibilidadSinDistinguirMayusculas() throws Exception {
        assertTrue(new Producto(1, "A", "Sopa", "", "Entrada", BigDecimal.TEN, "disponible").isDisponible());
        assertFalse(new Producto(1, "A", "Sopa", "", "Entrada", BigDecimal.TEN, null).isDisponible());
    }
}

