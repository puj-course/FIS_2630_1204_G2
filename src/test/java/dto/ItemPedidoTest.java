package dto;

import entity.Producto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HU-28 y HU-39 — líneas del pedido y estado del producto")
class ItemPedidoTest {

    private Producto producto(String precio, String estado) {
        return new Producto(1L, "H1", "Hamburguesa", "Con queso",
                "Platos fuertes", new BigDecimal(precio), estado);
    }

    @Test
    @DisplayName("una línea nueva arranca en una unidad")
    void arrancaEnUno() {
        ItemPedido item = new ItemPedido(producto("25000", "DISPONIBLE"));
        assertEquals(1, item.getCantidad());
        assertEquals(0, new BigDecimal("25000").compareTo(item.getSubtotal()));
    }

    @Test
    @DisplayName("el subtotal es precio por cantidad")
    void subtotalPorCantidad() {
        ItemPedido item = new ItemPedido(producto("25000", "DISPONIBLE"));
        item.aumentarCantidad();
        item.aumentarCantidad();
        assertEquals(3, item.getCantidad());
        assertEquals(0, new BigDecimal("75000").compareTo(item.getSubtotal()));
    }

    @Test
    @DisplayName("la cantidad nunca baja de uno")
    void nuncaBajaDeUno() {
        // Quitar la última unidad se maneja eliminando la línea, no bajando a cero.
        ItemPedido item = new ItemPedido(producto("25000", "DISPONIBLE"));
        item.disminuirCantidad();
        item.disminuirCantidad();
        assertEquals(1, item.getCantidad());
    }

    @Test
    @DisplayName("solo DISPONIBLE cuenta como disponible")
    void estadoDisponible() {
        assertTrue(producto("25000", "DISPONIBLE").isDisponible());
        assertTrue(producto("25000", "disponible").isDisponible());
        assertFalse(producto("25000", "AGOTADO").isDisponible());
        assertFalse(producto("25000", "INACTIVO").isDisponible());
    }
}
