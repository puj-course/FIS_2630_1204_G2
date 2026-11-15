package service.cocina;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComandaCocinaTest {
    @Test
    void exponeLaIdentidadYColumnaDelPedido() throws Exception {
        var pedido = new entity.Pedido();
        pedido.setNumeroPedido("P-8");
        pedido.setMesaId(5L);
        pedido.setEstado(enums.EstadoPedido.EN_PREPARACION);
        ComandaCocina comanda = new PedidoComandaAdapter(pedido);
        assertEquals("P-8", comanda.getNumero());
        assertEquals(5L, comanda.getMesaId());
        assertEquals(ComandaCocina.Columna.PREPARACION, comanda.getColumna());
    }
}

