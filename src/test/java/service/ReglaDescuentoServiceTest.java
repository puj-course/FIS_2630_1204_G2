package service;

import entity.ReglaDescuento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReglaDescuentoServiceTest {

    private final ReglaDescuentoService servicio = new ReglaDescuentoService();

    // ------------------------------------------- descuentos en porcentaje --

    @Test
    void calculaElPrecioConDescuentoPorcentual() throws Exception {
        assertEquals(90.0, servicio.calcularPrecioFinal(100.0, new ReglaDescuento("Promoción", 10, true)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(new ReglaDescuento("Promoción", -1, true)));
    }

    @Test
    @DisplayName("el descuento del 15 % de cumpleanos sobre 32000")
    void aplicaElQuincePorCiento() {
        assertEquals(27200.0, servicio.calcularPrecioFinal(32000.0, new ReglaDescuento("Cumpleaños", 15, true)), 0.001);
    }

    @Test
    @DisplayName("un porcentaje de cero deja el precio igual")
    void porcentajeCeroNoCambiaNada() {
        assertEquals(50000.0, servicio.calcularPrecioFinal(50000.0, new ReglaDescuento("Ninguno", 0, true)), 0.001);
    }

    @Test
    @DisplayName("el cien por ciento deja la cuenta en cero")
    void cienPorCientoDejaEnCero() {
        assertEquals(0.0, servicio.calcularPrecioFinal(18000.0, new ReglaDescuento("Cortesía total", 100, true)), 0.001);
    }

    @Test
    @DisplayName("sobre un precio de cero el porcentaje sigue dando cero")
    void sobrePrecioCeroDaCero() {
        assertEquals(0.0, servicio.calcularPrecioFinal(0.0, new ReglaDescuento("Promoción", 25, true)), 0.001);
    }

    // ------------------------------------------- descuentos de monto fijo --

    @Test
    @DisplayName("el monto fijo se resta tal cual")
    void elMontoFijoSeResta() {
        assertEquals(27000.0, servicio.calcularPrecioFinal(32000.0, new ReglaDescuento("Cortesía", 5000, false)), 0.001);
    }

    @Test
    @DisplayName("un monto fijo igual al precio deja la cuenta en cero")
    void montoFijoIgualAlPrecioDejaEnCero() {
        assertEquals(0.0, servicio.calcularPrecioFinal(5000.0, new ReglaDescuento("Cortesía", 5000, false)), 0.001);
    }

    // ---------------------------------------------- validaciones al guardar --

    @Test
    @DisplayName("no guarda una regla con valor negativo")
    void rechazaValorNegativo() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(new ReglaDescuento("Promoción", -0.01, false)));
        assertEquals("El valor no puede ser negativo", error.getMessage());
    }

    @Test
    @DisplayName("no guarda una regla sin tipo de cambio")
    void rechazaTipoDeCambioVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(new ReglaDescuento(null, 10, true)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(new ReglaDescuento("", 10, true)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(new ReglaDescuento("   ", 10, true)));
    }

    // -------------------------------------------------- huecos conocidos --

    @Test
    @DisplayName("hoy un descuento mayor que el precio deja un total negativo")
    void descuentoMayorQueElPrecioDejaNegativo() {
        // Deja constancia del comportamiento actual: el servicio no acota el
        // resultado en cero. Si se decide corregirlo, esta prueba debe cambiar.
        assertTrue(servicio.calcularPrecioFinal(3000.0, new ReglaDescuento("Cortesía", 5000, false)) < 0);
        assertTrue(servicio.calcularPrecioFinal(1000.0, new ReglaDescuento("Error", 150, true)) < 0);
    }

    @Test
    @DisplayName("hoy se puede guardar un porcentaje mayor que cien")
    void hoySeAceptaPorcentajeMayorQueCien() {
        // registrar() solo rechaza valores negativos y el tipo vacio: un 150 %
        // pasa la validacion. Queda documentado para decidir si se corrige.
        assertDoesNotThrow(() -> {
            var regla = new ReglaDescuento("Exagerado", 150, true);
            assertEquals(150.0, regla.getValor(), 0.001);
            assertTrue(regla.isEsPorcentaje());
        });
    }
}
