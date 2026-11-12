package service;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PedidoFacadeTest {
    @Test
    void sumaElSubtotalDeCadaLinea() throws Exception {
        var producto = new entity.Producto(1, "P1", "Sopa", "", "Entrada", new BigDecimal("5.50"), "DISPONIBLE");
        var lineas = java.util.List.of(new dto.ItemPedido(producto, 2), new dto.ItemPedido(producto, 3));
        assertEquals(new BigDecimal("27.50"), new PedidoFacade().calcularSubtotal(lineas));
    }
}

