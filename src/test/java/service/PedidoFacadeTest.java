package service;

import dto.ItemPedido;
import entity.Producto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PedidoFacade es la fachada que usa la pantalla del mesero. Aqui se prueba el
 * calculo del subtotal, que es la parte que no necesita base de datos.
 */
class PedidoFacadeTest {

    private final PedidoFacade facade = new PedidoFacade();

    private Producto productoDe(long id, String precio) {
        return new Producto(id, "P" + id, "Producto " + id, "", "Entrada",
                new BigDecimal(precio), "DISPONIBLE");
    }

    @Test
    void sumaElSubtotalDeCadaLinea() throws Exception {
        var producto = productoDe(1, "5.50");
        var lineas = List.of(new ItemPedido(producto, 2), new ItemPedido(producto, 3));
        assertEquals(new BigDecimal("27.50"), facade.calcularSubtotal(lineas));
    }

    @Test
    @DisplayName("un pedido vacio suma cero, no revienta")
    void elPedidoVacioSumaCero() {
        assertEquals(BigDecimal.ZERO, facade.calcularSubtotal(List.of()));
    }

    @Test
    @DisplayName("con una sola linea el subtotal es el de esa linea")
    void conUnaLineaEsElDeEsaLinea() {
        var item = new ItemPedido(productoDe(1, "32000"), 2);
        assertEquals(item.getSubtotal(), facade.calcularSubtotal(List.of(item)));
    }

    @Test
    @DisplayName("suma productos de precios distintos")
    void sumaProductosDistintos() {
        // 32000x1 + 7500x2 = 47000
        var lineas = List.of(
                new ItemPedido(productoDe(1, "32000"), 1),
                new ItemPedido(productoDe(2, "7500"), 2));
        assertEquals(new BigDecimal("47000"), facade.calcularSubtotal(lineas));
    }

    @Test
    @DisplayName("respeta los centavos de cada linea")
    void respetaLosCentavos() {
        var lineas = List.of(
                new ItemPedido(productoDe(1, "5.55"), 3),
                new ItemPedido(productoDe(2, "0.45"), 1));
        assertEquals(new BigDecimal("17.10"), facade.calcularSubtotal(lineas));
    }

    @Test
    @DisplayName("el mismo producto repetido en dos lineas se suma dos veces")
    void elMismoProductoEnDosLineasSeSumaDosVeces() {
        // La fachada no agrupa: suma lo que le pasen tal como se lo pasan.
        var producto = productoDe(1, "10000");
        var lineas = List.of(new ItemPedido(producto, 1), new ItemPedido(producto, 1));
        assertEquals(new BigDecimal("20000"), facade.calcularSubtotal(lineas));
    }

    @Test
    @DisplayName("un producto gratis no cambia el subtotal")
    void unProductoGratisNoCambiaNada() {
        var cobrado = new ItemPedido(productoDe(1, "10000"), 1);
        var gratis = new ItemPedido(productoDe(2, "0"), 1);
        assertEquals(0, facade.calcularSubtotal(List.of(cobrado, gratis))
                .compareTo(facade.calcularSubtotal(List.of(cobrado))));
    }

    @Test
    @DisplayName("el orden de las lineas no cambia el total")
    void elOrdenNoCambiaElTotal() {
        var uno = new ItemPedido(productoDe(1, "32000"), 1);
        var dos = new ItemPedido(productoDe(2, "7500"), 2);
        assertEquals(0, facade.calcularSubtotal(List.of(uno, dos))
                .compareTo(facade.calcularSubtotal(List.of(dos, uno))));
    }

    @Test
    @DisplayName("el subtotal nunca es negativo si los precios no lo son")
    void elSubtotalNoEsNegativo() {
        var lineas = List.of(
                new ItemPedido(productoDe(1, "32000"), 1),
                new ItemPedido(productoDe(2, "7500"), 4),
                new ItemPedido(productoDe(3, "3000"), 2));
        assertTrue(facade.calcularSubtotal(lineas).signum() > 0);
    }
}
