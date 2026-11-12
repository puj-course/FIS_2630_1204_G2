package entity;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngredienteTest {
    @Test
    void detectaStockBajoEnElUmbral() throws Exception {
        var ingrediente = new Ingrediente();
        ingrediente.setStockMinimo(BigDecimal.TEN);
        ingrediente.setStockActual(BigDecimal.TEN);
        assertTrue(ingrediente.tieneStockBajo());
        ingrediente.setStockActual(BigDecimal.valueOf(11));
        assertFalse(ingrediente.tieneStockBajo());
    }
}

