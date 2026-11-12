package service;

import entity.Adicional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AdicionalServiceTest {

    private final AdicionalService servicio = new AdicionalService();

    private Adicional adicionalDe(String precio) {
        var adicional = new Adicional();
        adicional.setNombre("Queso extra");
        if (precio != null) {
            adicional.setPrecioAdicional(new BigDecimal(precio));
        }
        return adicional;
    }

    @Test
    void multiplicaSobrecostoPorCantidad() throws Exception {
        var adicional = new Adicional();
        assertEquals(BigDecimal.ZERO, servicio.calcularSobrecosto(adicional, 3));
        adicional.setPrecioAdicional(new BigDecimal("2.50"));
        assertEquals(new BigDecimal("7.50"), servicio.calcularSobrecosto(adicional, 3));
    }

    @Test
    @DisplayName("una sola unidad cuesta el precio del adicional")
    void unaUnidadCuestaElPrecio() {
        assertEquals(new BigDecimal("3000"), servicio.calcularSobrecosto(adicionalDe("3000"), 1));
    }

    @Test
    @DisplayName("cantidad cero no suma nada")
    void cantidadCeroNoSuma() {
        assertEquals(0, servicio.calcularSobrecosto(adicionalDe("3000"), 0).compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("sin precio registrado el sobrecosto es cero, no un error")
    void sinPrecioDevuelveCero() {
        assertEquals(BigDecimal.ZERO, servicio.calcularSobrecosto(adicionalDe(null), 10));
    }

    @Test
    @DisplayName("conserva los decimales del precio")
    void conservaLosDecimales() {
        assertEquals(new BigDecimal("4500.75"), servicio.calcularSobrecosto(adicionalDe("1500.25"), 3));
    }

    @Test
    @DisplayName("la escala del resultado es la del precio mas la de la cantidad")
    void respetaLaEscalaDeBigDecimal() {
        // 2.50 x 2 = 5.00, no 5.0 ni 5: con BigDecimal la escala importa al comparar
        var resultado = servicio.calcularSobrecosto(adicionalDe("2.50"), 2);
        assertEquals(new BigDecimal("5.00"), resultado);
        assertEquals(2, resultado.scale());
    }

    @Test
    @DisplayName("una cantidad alta no pierde precision")
    void cantidadAltaNoPierdePrecision() {
        assertEquals(new BigDecimal("3000000"), servicio.calcularSobrecosto(adicionalDe("3000"), 1000));
    }

    @Test
    @DisplayName("hoy una cantidad negativa devuelve un sobrecosto negativo")
    void cantidadNegativaDaNegativo() {
        // El servicio no valida la cantidad. Queda documentado por si se decide
        // rechazarla en vez de devolver un numero negativo.
        assertTrue(servicio.calcularSobrecosto(adicionalDe("3000"), -2).signum() < 0);
    }
}
