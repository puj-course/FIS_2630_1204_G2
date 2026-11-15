package dto;

import entity.Producto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ItemPedido es cada linea del carrito del mesero: un producto y cuantos van.
 */
class ItemPedidoTest {

    private static final Producto BANDEJA = new Producto(
            1L, "PLA-001", "Bandeja paisa", "Con chicharron",
            "Plato fuerte", new BigDecimal("32000"), "DISPONIBLE");

    @Test
    void arrancaConUnaUnidad() throws Exception {
        assertEquals(1, new ItemPedido(BANDEJA).getCantidad());
    }

    @Test
    @DisplayName("guarda el producto que le pasaron")
    void guardaElProducto() {
        var item = new ItemPedido(BANDEJA);
        assertSame(BANDEJA, item.getProducto());
        assertEquals("Bandeja paisa", item.getProducto().getNombre());
    }

    @Test
    @DisplayName("se puede crear con la cantidad que ya pidio el cliente")
    void seCreaConLaCantidadDada() {
        assertEquals(3, new ItemPedido(BANDEJA, 3).getCantidad());
    }

    // --------------------------------------------------------- cantidades --

    @Test
    @DisplayName("el boton de mas sube de uno en uno")
    void elBotonDeMasSubeDeUnoEnUno() {
        var item = new ItemPedido(BANDEJA);
        item.aumentarCantidad();
        assertEquals(2, item.getCantidad());
        item.aumentarCantidad();
        assertEquals(3, item.getCantidad());
    }

    @Test
    @DisplayName("el boton de menos baja de uno en uno")
    void elBotonDeMenosBajaDeUnoEnUno() {
        var item = new ItemPedido(BANDEJA, 3);
        item.disminuirCantidad();
        assertEquals(2, item.getCantidad());
    }

    @Test
    @DisplayName("el boton de menos no baja de uno")
    void elBotonDeMenosNoBajaDeUno() {
        // Para quitar la linea del carrito el mesero usa el boton de borrar,
        // no el de menos: por eso se queda en uno.
        var item = new ItemPedido(BANDEJA);
        item.disminuirCantidad();
        item.disminuirCantidad();
        assertEquals(1, item.getCantidad());
    }

    @Test
    @DisplayName("escribir la cantidad directamente la cambia")
    void escribirLaCantidadLaCambia() {
        var item = new ItemPedido(BANDEJA);
        item.establecerCantidad(7);
        assertEquals(7, item.getCantidad());
    }

    @Test
    @DisplayName("escribir cero o un numero negativo no cambia nada")
    void cantidadInvalidaNoCambiaNada() {
        var item = new ItemPedido(BANDEJA, 4);
        item.establecerCantidad(0);
        assertEquals(4, item.getCantidad());
        item.establecerCantidad(-2);
        assertEquals(4, item.getCantidad());
    }

    // ----------------------------------------------------------- subtotal --

    @Test
    @DisplayName("el subtotal es el precio por la cantidad")
    void elSubtotalEsPrecioPorCantidad() {
        assertEquals(new BigDecimal("96000"), new ItemPedido(BANDEJA, 3).getSubtotal());
    }

    @Test
    @DisplayName("con una unidad el subtotal es el precio del producto")
    void conUnaUnidadElSubtotalEsElPrecio() {
        assertEquals(0, new ItemPedido(BANDEJA).getSubtotal()
                .compareTo(BANDEJA.getPrecioVenta()));
    }

    @Test
    @DisplayName("el subtotal se mueve cuando cambia la cantidad")
    void elSubtotalSigueALaCantidad() {
        var item = new ItemPedido(BANDEJA);
        var conUna = item.getSubtotal();
        item.aumentarCantidad();
        assertEquals(0, item.getSubtotal().compareTo(conUna.multiply(BigDecimal.valueOf(2))));
    }

    @Test
    @DisplayName("un producto con precio con centavos no pierde los centavos")
    void respetaLosCentavosDelPrecio() {
        var conCentavos = new Producto(2L, "BEB-001", "Jugo", "De mora",
                "Bebida", new BigDecimal("7500.50"), "DISPONIBLE");
        assertEquals(new BigDecimal("15001.00"), new ItemPedido(conCentavos, 2).getSubtotal());
    }
}
