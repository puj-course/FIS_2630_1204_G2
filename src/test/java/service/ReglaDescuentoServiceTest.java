package service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReglaDescuentoServiceTest {
    @Test
    void calculaElPrecioConDescuentoPorcentual() throws Exception {
        var servicio = new ReglaDescuentoService();
        assertEquals(90.0, servicio.calcularPrecioFinal(100.0, new entity.ReglaDescuento("Promoción", 10, true)));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(new entity.ReglaDescuento("Promoción", -1, true)));
    }
}

