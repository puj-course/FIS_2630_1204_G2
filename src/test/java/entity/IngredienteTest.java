package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class IngredienteTest {

    private Ingrediente ingredienteCon(String stock, String minimo) {
        var i = new Ingrediente();
        i.setNombre("Aguacate");
        i.setStockActual(stock == null ? null : new BigDecimal(stock));
        i.setStockMinimo(minimo == null ? null : new BigDecimal(minimo));
        return i;
    }

    @Test
    void conservaElNombre() throws Exception {
        var registro = new Ingrediente();
        registro.setNombre("Carne de res");
        assertEquals("Carne de res", registro.getNombre());
    }

    // ------------------------------------------------------ stock bajo --

    @Test
    @DisplayName("hay stock bajo cuando el actual queda por debajo del minimo")
    void detectaStockPorDebajoDelMinimo() {
        assertTrue(ingredienteCon("2", "5").tieneStockBajo());
    }

    @Test
    @DisplayName("el minimo exacto tambien cuenta como stock bajo")
    void elMinimoExactoCuentaComoBajo() {
        // La comparacion es <= 0, asi que quedarse justo en el minimo ya avisa.
        assertTrue(ingredienteCon("5", "5").tieneStockBajo());
    }

    @Test
    @DisplayName("no hay stock bajo cuando sobra inventario")
    void noAvisaCuandoHaySuficiente() {
        assertFalse(ingredienteCon("20", "5").tieneStockBajo());
    }

    @Test
    @DisplayName("sin existencias tambien es stock bajo")
    void sinExistenciasEsStockBajo() {
        assertTrue(ingredienteCon("0", "5").tieneStockBajo());
    }

    @Test
    @DisplayName("compara por valor, no por la escala del numero")
    void comparaPorValorNoPorEscala() {
        // 5.00 y 5 son el mismo valor aunque BigDecimal.equals diria que no.
        assertTrue(ingredienteCon("5.00", "5").tieneStockBajo());
        assertFalse(ingredienteCon("5.01", "5.00").tieneStockBajo());
    }

    @Test
    @DisplayName("sin datos de stock no se inventa una alerta")
    void sinDatosNoAvisa() {
        assertFalse(ingredienteCon(null, "5").tieneStockBajo());
        assertFalse(ingredienteCon("2", null).tieneStockBajo());
        assertFalse(ingredienteCon(null, null).tieneStockBajo());
    }

    @Test
    @DisplayName("un ingrediente recien creado arranca con el stock en cero")
    void arrancaConStockEnCero() {
        // Importa para la prueba anterior: los campos no nacen nulos, nacen en
        // cero, asi que un ingrediente nuevo ya cuenta como stock bajo.
        var nuevo = new Ingrediente();
        assertEquals(0, nuevo.getStockActual().compareTo(BigDecimal.ZERO));
        assertEquals(0, nuevo.getStockMinimo().compareTo(BigDecimal.ZERO));
        assertTrue(nuevo.tieneStockBajo());
    }

    // ---------------------------------------------------------- estado --

    @Test
    @DisplayName("estaActivo no distingue mayusculas de minusculas")
    void estaActivoIgnoraMayusculas() {
        var i = new Ingrediente();
        i.setEstado("ACTIVO");
        assertTrue(i.estaActivo());
        i.setEstado("activo");
        assertTrue(i.estaActivo());
        i.setEstado("Activo");
        assertTrue(i.estaActivo());
    }

    @Test
    @DisplayName("cualquier otro estado no cuenta como activo")
    void otroEstadoNoEsActivo() {
        var i = new Ingrediente();
        i.setEstado("INACTIVO");
        assertFalse(i.estaActivo());
        i.setEstado("");
        assertFalse(i.estaActivo());
        i.setEstado(null);
        assertFalse(i.estaActivo());
    }

    @Test
    @DisplayName("isActivo si distingue mayusculas, a diferencia de estaActivo")
    void isActivoEsSensibleAMayusculas() {
        // Las dos conviven en la clase y no se comportan igual. Queda
        // documentado para que nadie las use como si fueran intercambiables.
        var i = new Ingrediente();
        i.setEstado("activo");
        assertTrue(i.estaActivo());
        assertFalse(i.isActivo());
    }

    @Test
    @DisplayName("se muestra con su nombre en las listas de la interfaz")
    void seMuestraConSuNombre() {
        var i = new Ingrediente();
        i.setNombre("Tomate chonto");
        assertEquals("Tomate chonto", i.toString());
    }
}
