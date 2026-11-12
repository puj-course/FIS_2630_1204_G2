package dto;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngredienteStockDTOTest {
    @Test
    void conservaStockActualYMinimo() throws Exception {
        var dto = new IngredienteStockDTO(1L, "Arroz", BigDecimal.ONE, BigDecimal.TEN, true, "ACTIVO");
        assertEquals(BigDecimal.ONE, dto.getStockActual());
        assertEquals(BigDecimal.TEN, dto.getStockMinimo());
    }
}

