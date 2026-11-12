package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PedidoTest {
    @Test
    void permiteMarcarInventarioDescontado() throws Exception {
        var pedido = new Pedido();
        assertFalse(pedido.isInventarioDescontado());
        pedido.setInventarioDescontado(true);
        assertTrue(pedido.isInventarioDescontado());
    }
}

