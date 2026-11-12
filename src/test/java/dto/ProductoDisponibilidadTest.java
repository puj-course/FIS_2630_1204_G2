package dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductoDisponibilidadTest {
    @Test
    void conservaElMotivoDeIndisponibilidad() throws Exception {
        var dto = new ProductoDisponibilidad(4, "P-4", "Sopa", "Entrada", "AGOTADO", "Sin insumos");
        assertEquals(4L, dto.getProductoId());
        assertEquals("AGOTADO", dto.getEstado());
        assertEquals("Sin insumos", dto.getMotivo());
    }
}

