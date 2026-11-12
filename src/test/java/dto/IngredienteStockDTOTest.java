package dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IngredienteStockDTO es la fila que ve el administrador en la pantalla de
 * inventario: cuanto hay, cuanto es el minimo y si el ingrediente sigue en uso.
 */
class IngredienteStockDTOTest {

    private IngredienteStockDTO filaCon(String actual, String minimo, Boolean activo, String estado) {
        return new IngredienteStockDTO(10L, "Carne de res",
                actual == null ? null : new BigDecimal(actual),
                minimo == null ? null : new BigDecimal(minimo),
                activo, estado);
    }

    @Test
    void conservaLosDatosDelIngrediente() throws Exception {
        var fila = filaCon("12.5", "5", true, "ACTIVO");
        assertEquals(10L, fila.getId());
        assertEquals("Carne de res", fila.getNombre());
    }

    @Test
    @DisplayName("muestra el stock actual y el minimo tal como estan en la base")
    void muestraElStockComoEstaEnLaBase() {
        var fila = filaCon("12.50", "5.00", true, "ACTIVO");
        assertEquals(new BigDecimal("12.50"), fila.getStockActual());
        assertEquals(new BigDecimal("5.00"), fila.getStockMinimo());
    }

    @Test
    @DisplayName("no redondea ni cambia la escala de los numeros")
    void noCambiaLaEscala() {
        // Importa para la pantalla: 12.50 kg y 12.5 kg se tienen que ver
        // distinto si asi quedaron guardados.
        assertEquals(2, filaCon("12.50", "5.00", true, "ACTIVO").getStockActual().scale());
        assertEquals(0, filaCon("12", "5", true, "ACTIVO").getStockActual().scale());
    }

    @Test
    @DisplayName("deja comparar el actual contra el minimo para pintar la alerta")
    void permiteCompararParaLaAlerta() {
        var escaso = filaCon("2", "5", true, "ACTIVO");
        assertTrue(escaso.getStockActual().compareTo(escaso.getStockMinimo()) < 0);

        var sobrado = filaCon("20", "5", true, "ACTIVO");
        assertTrue(sobrado.getStockActual().compareTo(sobrado.getStockMinimo()) > 0);
    }

    @Test
    @DisplayName("un ingrediente que ya no se usa llega marcado como inactivo")
    void elInactivoLlegaMarcado() {
        var fila = filaCon("0", "5", false, "INACTIVO");
        assertFalse(fila.getIsActive());
        assertEquals("INACTIVO", fila.getEstado());
    }

    @Test
    @DisplayName("el indicador de activo puede llegar nulo desde la base")
    void elIndicadorPuedeLlegarNulo() {
        // Es Boolean y no boolean justamente por eso: la columna admite nulos
        // y la pantalla tiene que estar preparada.
        assertNull(filaCon("1", "1", null, "ACTIVO").getIsActive());
    }

    @Test
    @DisplayName("el stock puede llegar nulo y el DTO no lo convierte en cero")
    void elStockNuloSeRespeta() {
        // A diferencia de CuentaPago, este DTO no rellena los nulos: quien lo
        // use tiene que revisarlos.
        var fila = filaCon(null, null, true, "ACTIVO");
        assertNull(fila.getStockActual());
        assertNull(fila.getStockMinimo());
    }

    @Test
    @DisplayName("el estado y el indicador de activo son dos datos distintos")
    void estadoEIndicadorSonDistintos() {
        var raro = filaCon("3", "5", true, "INACTIVO");
        assertTrue(raro.getIsActive());
        assertEquals("INACTIVO", raro.getEstado());
    }
}
