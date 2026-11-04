package service.alerta;

import entity.Ingrediente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Servicio 2 de 3 — Alertas de inventario (patrón Factory Method).
 *
 * Métodos bajo prueba:
 *   1. AlertaStockFactory.para (elige la fábrica según el ingrediente)
 *   2. AlertaAgotadoFactory.crearAlerta
 *   3. AlertaStockBajoFactory.crearAlerta
 *   4. AlertaNormalFactory.crearAlerta
 *   5. Ingrediente.tieneStockBajo (el umbral que usa la fábrica)
 *
 * Todas las pruebas siguen Arrange - Act - Assert y no tocan la base de datos.
 */
@DisplayName("Alertas de inventario (Factory Method)")
class AlertaStockFactoryTest {

    /** Construye un ingrediente de prueba con stock actual y mínimo. */
    private Ingrediente ingrediente(String stockActual, String stockMinimo) {
        Ingrediente i = new Ingrediente();
        i.setNombre("Tomate");
        i.setStockActual(stockActual == null ? null : new BigDecimal(stockActual));
        i.setStockMinimo(stockMinimo == null ? null : new BigDecimal(stockMinimo));
        return i;
    }

    // ---------------------------------------------------------------
    // Método 1: AlertaStockFactory.para
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP12 (normal) — con stock holgado la alerta es NORMAL")
    void stockHolgadoEsNormal() {
        // Arrange
        Ingrediente tomate = ingrediente("50", "10");

        // Act
        AlertaStock alerta = AlertaStockFactory.para(tomate).crearAlerta();

        // Assert
        assertEquals("NORMAL", alerta.getTexto());
    }

    @Test
    @DisplayName("CP13 (borde) — stock exactamente en cero es AGOTADO, no STOCK BAJO")
    void stockEnCeroEsAgotado() {
        // Arrange
        Ingrediente tomate = ingrediente("0", "10");

        // Act
        AlertaStockFactory fabrica = AlertaStockFactory.para(tomate);

        // Assert
        assertInstanceOf(AlertaAgotadoFactory.class, fabrica);
        assertEquals("AGOTADO", fabrica.crearAlerta().getTexto());
    }

    @Test
    @DisplayName("CP14 (borde) — stock exactamente igual al mínimo ya es STOCK BAJO")
    void stockIgualAlMinimoEsStockBajo() {
        // Arrange
        Ingrediente tomate = ingrediente("10", "10");

        // Act
        AlertaStockFactory fabrica = AlertaStockFactory.para(tomate);

        // Assert
        assertInstanceOf(AlertaStockBajoFactory.class, fabrica);
        assertEquals("STOCK BAJO", fabrica.crearAlerta().getTexto());
    }

    @Test
    @DisplayName("CP15 (negativa) — un stock negativo se trata como AGOTADO")
    void stockNegativoEsAgotado() {
        // Arrange
        Ingrediente tomate = ingrediente("-5", "10");

        // Act
        AlertaStock alerta = AlertaStockFactory.para(tomate).crearAlerta();

        // Assert
        assertEquals("AGOTADO", alerta.getTexto());
    }

    @Test
    @DisplayName("CP16 (negativa) — un ingrediente nulo no rompe la tabla de inventario")
    void ingredienteNuloNoRompe() {
        // Arrange
        Ingrediente ninguno = null;

        // Act
        AlertaStockFactory fabrica = AlertaStockFactory.para(ninguno);

        // Assert
        assertInstanceOf(AlertaNormalFactory.class, fabrica);
        assertEquals("NORMAL", fabrica.crearAlerta().getTexto());
    }

    @Test
    @DisplayName("CP17 (negativa) — un ingrediente sin stock registrado no rompe")
    void stockNuloNoRompe() {
        // Arrange
        Ingrediente tomate = ingrediente(null, "10");

        // Act
        AlertaStock alerta = AlertaStockFactory.para(tomate).crearAlerta();

        // Assert
        assertEquals("NORMAL", alerta.getTexto());
    }

    // ---------------------------------------------------------------
    // Métodos 2, 3 y 4: los tres crearAlerta
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP18 (normal) — cada fábrica crea su alerta con su propio estilo")
    void cadaFabricaCreaSuAlerta() {
        // Arrange
        AlertaStockFactory agotado = new AlertaAgotadoFactory();
        AlertaStockFactory bajo = new AlertaStockBajoFactory();
        AlertaStockFactory normal = new AlertaNormalFactory();

        // Act
        AlertaStock alertaAgotado = agotado.crearAlerta();
        AlertaStock alertaBajo = bajo.crearAlerta();
        AlertaStock alertaNormal = normal.crearAlerta();

        // Assert
        assertInstanceOf(AlertaAgotado.class, alertaAgotado);
        assertInstanceOf(AlertaStockBajo.class, alertaBajo);
        assertInstanceOf(AlertaNormal.class, alertaNormal);
        assertFalse(alertaAgotado.getEstiloFila().isEmpty());
        assertFalse(alertaBajo.getEstiloFila().isEmpty());
        assertTrue(alertaNormal.getEstiloFila().isEmpty());
    }

    @Test
    @DisplayName("CP19 (normal) — la fábrica entrega una instancia nueva en cada llamada")
    void crearAlertaDevuelveInstanciasNuevas() {
        // Arrange
        AlertaStockFactory fabrica = new AlertaStockBajoFactory();

        // Act
        AlertaStock primera = fabrica.crearAlerta();
        AlertaStock segunda = fabrica.crearAlerta();

        // Assert
        assertNotSame(primera, segunda);
    }

    // ---------------------------------------------------------------
    // Método 5: Ingrediente.tieneStockBajo
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP20 (borde) — una unidad por encima del mínimo todavía no alerta")
    void unaUnidadPorEncimaDelMinimo() {
        // Arrange
        Ingrediente tomate = ingrediente("11", "10");

        // Act
        boolean bajo = tomate.tieneStockBajo();

        // Assert
        assertFalse(bajo);
    }

    @Test
    @DisplayName("CP21 (negativa) — sin mínimo definido el ingrediente nunca alerta")
    void sinMinimoDefinidoNuncaAlerta() {
        // Arrange: ingrediente al que nadie le configuró el stock mínimo
        Ingrediente tomate = ingrediente("1", null);

        // Act
        boolean bajo = tomate.tieneStockBajo();

        // Assert: comportamiento actual, documentado como hallazgo en el informe.
        // Un ingrediente con una sola unidad no genera ninguna alerta.
        assertFalse(bajo);
        assertEquals("NORMAL", AlertaStockFactory.para(tomate).crearAlerta().getTexto());
    }

    // ---------------------------------------------------------------
    // Caso de lógica de negocio del restaurante
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP33 (negocio) — la alerta escala a medida que se consume el ingrediente")
    void laAlertaEscalaDuranteElServicio() {
        // Arrange: un ingrediente que arranca el día con bodega suficiente.
        // Durante el servicio se va gastando y la tabla de inventario se
        // recalcula en cada refresco, así que la alerta tiene que ir subiendo
        // de nivel en el orden correcto y no saltarse ninguno.
        Ingrediente tomate = ingrediente("50", "10");

        // Act
        String alInicio = AlertaStockFactory.para(tomate).crearAlerta().getTexto();

        tomate.setStockActual(new BigDecimal("11"));
        String antesDelMinimo = AlertaStockFactory.para(tomate).crearAlerta().getTexto();

        tomate.setStockActual(new BigDecimal("10"));
        String alLlegarAlMinimo = AlertaStockFactory.para(tomate).crearAlerta().getTexto();

        tomate.setStockActual(new BigDecimal("3"));
        String bajoMinimo = AlertaStockFactory.para(tomate).crearAlerta().getTexto();

        tomate.setStockActual(BigDecimal.ZERO);
        String alAgotarse = AlertaStockFactory.para(tomate).crearAlerta().getTexto();

        // Assert
        assertEquals("NORMAL", alInicio);
        assertEquals("NORMAL", antesDelMinimo);
        assertEquals("STOCK BAJO", alLlegarAlMinimo);
        assertEquals("STOCK BAJO", bajoMinimo);
        assertEquals("AGOTADO", alAgotarse);
    }
}
