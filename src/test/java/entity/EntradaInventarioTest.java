package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntradaInventarioTest {
    @Test
    void clasificaElMovimientoComoEntrada() throws Exception {
        var entrada = new EntradaInventario(5L, 2.5, "Compra");
        assertEquals("entrada", entrada.getTipoMovimiento());
        assertEquals(5L, entrada.getIngredienteId());
        assertEquals(2.5, entrada.getCantidad());
    }
}


