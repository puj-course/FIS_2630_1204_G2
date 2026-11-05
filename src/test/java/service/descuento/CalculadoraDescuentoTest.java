package service.descuento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Servicio 1 de 3 — Descuentos (patrón Strategy).
 *
 * Métodos bajo prueba:
 *   1. DescuentoPorcentaje.calcular
 *   2. DescuentoMontoFijo.calcular
 *   3. CalculadoraDescuento.calcular (delegación del contexto)
 *   4. CalculadoraDescuento.setEstrategia (cambio en caliente)
 *   5. CalculadoraDescuento() (estrategia por defecto)
 *
 * Todas las pruebas siguen Arrange - Act - Assert y no tocan la base de datos.
 */
@DisplayName("Servicio de descuentos (Strategy)")
class CalculadoraDescuentoTest {

    private static final BigDecimal SUBTOTAL = new BigDecimal("100000");

    // ---------------------------------------------------------------
    // Método 1: DescuentoPorcentaje.calcular
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP01 (normal) — 10% sobre $100.000 descuenta $10.000")
    void porcentajeNormal() {
        // Arrange
        DescuentoStrategy estrategia = new DescuentoPorcentaje();

        // Act
        BigDecimal descuento = estrategia.calcular(SUBTOTAL, new BigDecimal("10"));

        // Assert
        assertEquals(new BigDecimal("10000.00"), descuento);
    }

    @Test
    @DisplayName("CP02 (borde) — 100% descuenta la cuenta completa")
    void porcentajeEnElLimiteSuperior() {
        // Arrange
        DescuentoStrategy estrategia = new DescuentoPorcentaje();

        // Act
        BigDecimal descuento = estrategia.calcular(SUBTOTAL, new BigDecimal("100"));

        // Assert
        assertEquals(new BigDecimal("100000.00"), descuento);
    }

    @Test
    @DisplayName("CP03 (negativa) — un porcentaje mayor a 100 se rechaza")
    void porcentajePorEncimaDelLimite() {
        // Arrange
        DescuentoStrategy estrategia = new DescuentoPorcentaje();

        // Act + Assert
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> estrategia.calcular(SUBTOTAL, new BigDecimal("101")));
        assertEquals("El porcentaje no puede superar el 100%", error.getMessage());
    }

    // ---------------------------------------------------------------
    // Método 2: DescuentoMontoFijo.calcular
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP04 (borde) — un monto igual al subtotal se acepta")
    void montoFijoIgualAlSubtotal() {
        // Arrange
        DescuentoStrategy estrategia = new DescuentoMontoFijo();

        // Act
        BigDecimal descuento = estrategia.calcular(SUBTOTAL, new BigDecimal("100000"));

        // Assert
        assertEquals(new BigDecimal("100000.00"), descuento);
    }

    @Test
    @DisplayName("CP05 (negativa) — un monto mayor que la cuenta se rechaza")
    void montoFijoMayorQueElSubtotal() {
        // Arrange
        DescuentoStrategy estrategia = new DescuentoMontoFijo();

        // Act + Assert
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> estrategia.calcular(new BigDecimal("20000"), new BigDecimal("90000")));
        assertEquals("El descuento no puede superar el subtotal de la cuenta",
                error.getMessage());
    }

    @Test
    @DisplayName("CP06 (negativa) — un subtotal nulo se rechaza en vez de reventar")
    void montoFijoConSubtotalNulo() {
        // Arrange
        DescuentoStrategy estrategia = new DescuentoMontoFijo();

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> estrategia.calcular(null, new BigDecimal("5000")));
    }

    // ---------------------------------------------------------------
    // Método 3: CalculadoraDescuento.calcular (el contexto delega)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP07 (normal) — el contexto devuelve lo mismo que su estrategia")
    void contextoDelegaEnLaEstrategia() {
        // Arrange
        DescuentoStrategy estrategia = new DescuentoPorcentaje();
        CalculadoraDescuento calculadora = new CalculadoraDescuento(estrategia);

        // Act
        BigDecimal porElContexto = calculadora.calcular(SUBTOTAL, new BigDecimal("15"));
        BigDecimal porLaEstrategia = estrategia.calcular(SUBTOTAL, new BigDecimal("15"));

        // Assert
        assertEquals(porLaEstrategia, porElContexto);
    }

    @Test
    @DisplayName("CP08 (borde) — un descuento de 0% deja la cuenta intacta")
    void contextoConDescuentoCero() {
        // Arrange
        CalculadoraDescuento calculadora = new CalculadoraDescuento(new DescuentoPorcentaje());

        // Act
        BigDecimal descuento = calculadora.calcular(SUBTOTAL, BigDecimal.ZERO);

        // Assert
        assertEquals(new BigDecimal("0.00"), descuento);
    }

    // ---------------------------------------------------------------
    // Método 4: CalculadoraDescuento.setEstrategia
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP09 (negocio) — cambiar de estrategia cambia el resultado")
    void cambioDeEstrategiaEnCaliente() {
        // Arrange
        CalculadoraDescuento calculadora = new CalculadoraDescuento(new DescuentoMontoFijo());
        BigDecimal comoMontoFijo = calculadora.calcular(SUBTOTAL, new BigDecimal("10"));

        // Act
        calculadora.setEstrategia(new DescuentoPorcentaje());
        BigDecimal comoPorcentaje = calculadora.calcular(SUBTOTAL, new BigDecimal("10"));

        // Assert
        assertEquals(new BigDecimal("10.00"), comoMontoFijo);
        assertEquals(new BigDecimal("10000.00"), comoPorcentaje);
    }

    @Test
    @DisplayName("CP10 (negativa) — asignar una estrategia nula se rechaza al momento")
    void estrategiaNulaSeRechaza() {
        // Arrange
        CalculadoraDescuento calculadora = new CalculadoraDescuento();

        // Act + Assert
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> calculadora.setEstrategia(null));
        assertEquals("La estrategia de descuento es obligatoria", error.getMessage());
    }

    // ---------------------------------------------------------------
    // Método 5: constructor por defecto
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP11 (normal) — sin estrategia explícita la calculadora usa monto fijo")
    void estrategiaPorDefecto() {
        // Arrange
        DescuentoStrategy esperada = new DescuentoMontoFijo();

        // Act
        CalculadoraDescuento calculadora = new CalculadoraDescuento();

        // Assert
        assertInstanceOf(esperada.getClass(), calculadora.getEstrategia());
        assertEquals(esperada.getNombre(), calculadora.getEstrategia().getNombre());
    }

    // ---------------------------------------------------------------
    // Validaciones del servicio que no tenían prueba detrás
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP34 (negativa) — el porcentaje no se calcula sobre una cuenta sin subtotal")
    void porcentajeConSubtotalNulo() {
        // Arrange
        DescuentoStrategy estrategia = new DescuentoPorcentaje();

        // Act + Assert
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> estrategia.calcular(null, new BigDecimal("10")));
        assertEquals("El subtotal y el porcentaje de descuento son obligatorios",
                error.getMessage());
    }

    @Test
    @DisplayName("CP35 (negativa) — un porcentaje negativo se rechaza")
    void porcentajeNegativo() {
        // Arrange
        DescuentoStrategy estrategia = new DescuentoPorcentaje();

        // Act + Assert
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> estrategia.calcular(SUBTOTAL, new BigDecimal("-5")));
        assertEquals("El porcentaje no puede ser negativo", error.getMessage());
    }

    @Test
    @DisplayName("CP36 (negativa) — un subtotal negativo se rechaza en el porcentaje")
    void porcentajeConSubtotalNegativo() {
        // Arrange
        DescuentoStrategy estrategia = new DescuentoPorcentaje();

        // Act + Assert
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> estrategia.calcular(new BigDecimal("-100"), new BigDecimal("10")));
        assertEquals("El subtotal no puede ser negativo", error.getMessage());
    }

    @Test
    @DisplayName("CP37 (negativa) — un monto fijo negativo se rechaza")
    void montoFijoNegativo() {
        // Arrange
        DescuentoStrategy estrategia = new DescuentoMontoFijo();

        // Act + Assert
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> estrategia.calcular(SUBTOTAL, new BigDecimal("-1000")));
        assertEquals("El descuento no puede ser negativo", error.getMessage());
    }

    @Test
    @DisplayName("CP38 (negativa) — un subtotal negativo se rechaza en el monto fijo")
    void montoFijoConSubtotalNegativo() {
        // Arrange
        DescuentoStrategy estrategia = new DescuentoMontoFijo();

        // Act + Assert
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> estrategia.calcular(new BigDecimal("-100"), new BigDecimal("10")));
        assertEquals("El subtotal no puede ser negativo", error.getMessage());
    }

    @Test
    @DisplayName("CP39 (negativa) — construir la calculadora con estrategia nula se rechaza")
    void constructorConEstrategiaNula() {
        // Arrange
        DescuentoStrategy ninguna = null;

        // Act + Assert
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new CalculadoraDescuento(ninguna));
        assertEquals("La estrategia de descuento es obligatoria", error.getMessage());
    }
}
