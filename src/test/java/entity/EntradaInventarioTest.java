package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EntradaInventario es cuando llega mercancia y se suma al inventario. El
 * constructor con datos ya la marca como entrada, sin que haya que decirlo.
 */
class EntradaInventarioTest {

    @Test
    void conservaElIngredienteYLaCantidad() throws Exception {
        var e = new EntradaInventario(10L, 25.5, "Compra al proveedor");
        assertEquals(10L, e.getIngredienteId());
        assertEquals(25.5, e.getCantidad());
    }

    @Test
    @DisplayName("el constructor con datos la marca como entrada")
    void elConstructorLaMarcaComoEntrada() {
        // Es la razon de ser de la clase: nunca deberia quedar como salida.
        assertEquals("entrada", new EntradaInventario(10L, 25.5, "Compra").getTipoMovimiento());
    }

    @Test
    @DisplayName("guarda por que entro la mercancia")
    void guardaElMotivo() {
        assertEquals("Compra al proveedor",
                new EntradaInventario(10L, 25.5, "Compra al proveedor").getMotivo());
    }

    @Test
    @DisplayName("el constructor vacio no marca el tipo: hay que ponerlo")
    void elConstructorVacioNoMarcaElTipo() {
        // Queda documentado: los dos constructores no dejan el objeto igual.
        var e = new EntradaInventario();
        assertNull(e.getTipoMovimiento());
        assertNull(e.getIngredienteId());
        assertEquals(0.0, e.getCantidad());
    }

    @Test
    @DisplayName("el tipo se puede cambiar para registrar una salida")
    void elTipoSePuedeCambiar() {
        var e = new EntradaInventario(10L, 5.0, "Merma");
        e.setTipoMovimiento("salida");
        assertEquals("salida", e.getTipoMovimiento());
    }

    @Test
    @DisplayName("acepta cantidades con decimales, que es como se mide el inventario")
    void aceptaDecimales() {
        assertEquals(0.75, new EntradaInventario(10L, 0.75, "Compra").getCantidad());
    }

    @Test
    @DisplayName("no valida la cantidad: acepta cero y negativos")
    void noValidaLaCantidad() {
        // La validacion esta en el servicio, no en la entidad.
        assertEquals(0.0, new EntradaInventario(10L, 0.0, "x").getCantidad());
        assertEquals(-3.0, new EntradaInventario(10L, -3.0, "x").getCantidad());
    }

    @Test
    @DisplayName("al guardarla la base le asigna el identificador")
    void alGuardarlaRecibeIdentificador() {
        var e = new EntradaInventario(10L, 25.5, "Compra");
        e.setIdMovimiento(40);
        assertEquals(40, e.getIdMovimiento());
    }
}
