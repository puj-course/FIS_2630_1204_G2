package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Producto es lo que se vende en la carta. Una vez creado no cambia: todos sus
 * campos son final, porque lo arma la consulta a la base y de ahi va directo a
 * la pantalla.
 */
class ProductoTest {

    private Producto productoEn(String estado) {
        return new Producto(1L, "PLA-001", "Bandeja paisa", "Con chicharron",
                "Plato fuerte", new BigDecimal("32000"), estado);
    }

    @Test
    void conservaLosDatosDeLaCarta() throws Exception {
        var p = productoEn("DISPONIBLE");
        assertEquals(1L, p.getProductoId());
        assertEquals("PLA-001", p.getCodigo());
        assertEquals("Bandeja paisa", p.getNombre());
    }

    @Test
    @DisplayName("guarda la descripcion y la categoria con las que se agrupa la carta")
    void guardaDescripcionYCategoria() {
        var p = productoEn("DISPONIBLE");
        assertEquals("Con chicharron", p.getDescripcion());
        assertEquals("Plato fuerte", p.getCategoria());
    }

    @Test
    @DisplayName("guarda el precio sin redondearlo")
    void guardaElPrecioSinRedondear() {
        var conCentavos = new Producto(2L, "BEB-001", "Jugo", "De mora",
                "Bebida", new BigDecimal("7500.50"), "DISPONIBLE");
        assertEquals(new BigDecimal("7500.50"), conCentavos.getPrecioVenta());
    }

    // ------------------------------------------------------ disponible --

    @Test
    @DisplayName("un producto DISPONIBLE se puede vender")
    void elDisponibleSePuedeVender() {
        assertTrue(productoEn("DISPONIBLE").isDisponible());
    }

    @Test
    @DisplayName("un producto AGOTADO no se puede vender")
    void elAgotadoNoSePuedeVender() {
        assertFalse(productoEn("AGOTADO").isDisponible());
    }

    @Test
    @DisplayName("no le importan las mayusculas del estado")
    void noLeImportanLasMayusculas() {
        assertTrue(productoEn("disponible").isDisponible());
        assertTrue(productoEn("Disponible").isDisponible());
    }

    @Test
    @DisplayName("sin estado no se puede vender, y no revienta")
    void sinEstadoNoSePuedeVender() {
        assertFalse(productoEn(null).isDisponible());
        assertFalse(productoEn("").isDisponible());
    }

    @Test
    @DisplayName("cualquier otro estado tampoco habilita la venta")
    void cualquierOtroEstadoNoHabilita() {
        assertFalse(productoEn("INACTIVO").isDisponible());
        assertFalse(productoEn("EN_REVISION").isDisponible());
    }

    @Test
    @DisplayName("el estado se puede leer tal cual, ademas de preguntarle si esta disponible")
    void elEstadoSeLeeTalCual() {
        assertEquals("AGOTADO", productoEn("AGOTADO").getEstado());
    }

    @Test
    @DisplayName("responder dos veces lo mismo: el producto no cambia")
    void elProductoNoCambia() {
        var p = productoEn("DISPONIBLE");
        assertTrue(p.isDisponible());
        assertTrue(p.isDisponible());
        assertEquals("DISPONIBLE", p.getEstado());
    }
}
