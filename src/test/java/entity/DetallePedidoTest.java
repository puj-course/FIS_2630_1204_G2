package entity;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DetallePedidoTest {
    @Test
    void recalculaSubtotalAlCambiarCantidadYPrecio() throws Exception {
        var detalle = new DetallePedido();
        detalle.setPrecioUnitario(new BigDecimal("7.50"));
        detalle.setCantidad(3);
        assertEquals(new BigDecimal("22.50"), detalle.getSubtotal());
        detalle.setPrecioUnitario(new BigDecimal("10.00"));
        assertEquals(new BigDecimal("30.00"), detalle.getSubtotal());
    }
}

