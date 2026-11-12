package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plato es el producto visto desde la administracion de la carta: ademas del
 * precio de venta guarda el costo y los topes de stock.
 */
class PlatoTest {

    private Plato bandeja() {
        return new Plato("PLA-001", "Bandeja paisa", "Con chicharron",
                "Plato fuerte", 32000.0, 18000.0);
    }

    @Test
    void conservaElCodigoYElNombre() throws Exception {
        var p = bandeja();
        assertEquals("PLA-001", p.getCodigo());
        assertEquals("Bandeja paisa", p.getNombre());
    }

    @Test
    @DisplayName("guarda el precio de venta y el costo por separado")
    void guardaPrecioYCosto() {
        // Es lo que permite calcular el margen en los reportes.
        var p = bandeja();
        assertEquals(32000.0, p.getPrecioVenta());
        assertEquals(18000.0, p.getCosto());
        assertTrue(p.getPrecioVenta() > p.getCosto());
    }

    @Test
    @DisplayName("guarda la descripcion y la categoria con las que se arma la carta")
    void guardaDescripcionYCategoria() {
        var p = bandeja();
        assertEquals("Con chicharron", p.getDescripcion());
        assertEquals("Plato fuerte", p.getCategoria());
    }

    @Test
    @DisplayName("el constructor con datos no toca el stock ni el estado")
    void elConstructorNoTocaElStock() {
        // Quedan en cero y en nulo: los llena la consulta a la base.
        var p = bandeja();
        assertEquals(0.0, p.getStockActual());
        assertEquals(0.0, p.getStockMinimo());
        assertEquals(0.0, p.getStockMaximo());
        assertNull(p.getEstado());
    }

    @Test
    @DisplayName("el constructor vacio deja todo sin llenar")
    void elConstructorVacioDejaTodoSinLlenar() {
        var p = new Plato();
        assertEquals(0L, p.getProductoId());
        assertNull(p.getCodigo());
        assertNull(p.getNombre());
        assertEquals(0.0, p.getPrecioVenta());
    }

    @Test
    @DisplayName("guarda los topes de stock que usan las alertas")
    void guardaLosTopesDeStock() {
        var p = bandeja();
        p.setStockActual(12.0);
        p.setStockMinimo(5.0);
        p.setStockMaximo(50.0);
        assertTrue(p.getStockActual() > p.getStockMinimo());
        assertTrue(p.getStockActual() < p.getStockMaximo());
    }

    @Test
    @DisplayName("el estado se escribe con los codigos que acepta la base")
    void elEstadoUsaLosCodigosDeLaBase() {
        var p = bandeja();
        p.setEstado("DISPONIBLE");
        assertEquals("DISPONIBLE", p.getEstado());
        p.setEstado("AGOTADO");
        assertEquals("AGOTADO", p.getEstado());
    }

    @Test
    @DisplayName("se le puede cambiar el precio sin crear otro plato")
    void seLeCambiaElPrecio() {
        var p = bandeja();
        p.setPrecioVenta(35000.0);
        assertEquals(35000.0, p.getPrecioVenta());
    }

    @Test
    @DisplayName("al leerlo de la base trae su identificador de producto")
    void traeSuIdentificador() {
        var p = bandeja();
        p.setProductoId(1L);
        assertEquals(1L, p.getProductoId());
    }

    @Test
    @DisplayName("usa double, no BigDecimal, a diferencia de Producto")
    void usaDouble() {
        // Son dos representaciones del mismo concepto con tipos distintos.
        // Queda escrito para que nadie espere centavos exactos aqui.
        var p = bandeja();
        p.setPrecioVenta(0.1 + 0.2);
        assertNotEquals(0.3, p.getPrecioVenta());
        assertEquals(0.3, p.getPrecioVenta(), 0.0000001);
    }
}
