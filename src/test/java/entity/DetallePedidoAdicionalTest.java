package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DetallePedidoAdicionalTest {
    @Test
    void conservaCantidad() throws Exception {
        var registro = new DetallePedidoAdicional();
        registro.setCantidad(3);
        assertEquals(3, registro.getCantidad());
    }
}

