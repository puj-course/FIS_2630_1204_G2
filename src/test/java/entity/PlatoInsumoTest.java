package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlatoInsumo es un renglon de la receta: cuanto de un ingrediente lleva un
 * plato. Es lo que mira el sistema para saber si el plato se puede preparar.
 */
class PlatoInsumoTest {

    @Test
    void conservaLaRelacionPlatoInsumo() throws Exception {
        var r = new PlatoInsumo(3, 10, 2);
        assertEquals(3, r.getPlatoId());
        assertEquals(10, r.getInsumoId());
    }

    @Test
    @DisplayName("guarda cuanto se necesita del ingrediente")
    void guardaCuantoSeNecesita() {
        assertEquals(2, new PlatoInsumo(3, 10, 2).getCantidadNecesaria());
    }

    @Test
    @DisplayName("el constructor vacio deja todo en cero")
    void elConstructorVacioDejaTodoEnCero() {
        var r = new PlatoInsumo();
        assertEquals(0, r.getId());
        assertEquals(0, r.getPlatoId());
        assertEquals(0, r.getInsumoId());
        assertEquals(0, r.getCantidadNecesaria());
    }

    @Test
    @DisplayName("el constructor con datos no asigna identificador: eso lo hace la base")
    void elConstructorNoAsignaIdentificador() {
        assertEquals(0, new PlatoInsumo(3, 10, 2).getId());
    }

    @Test
    @DisplayName("se puede cambiar la cantidad cuando se ajusta la receta")
    void seAjustaLaCantidad() {
        var r = new PlatoInsumo(3, 10, 2);
        r.setCantidadNecesaria(3);
        assertEquals(3, r.getCantidadNecesaria());
    }

    @Test
    @DisplayName("un plato lleva varios ingredientes: un renglon por cada uno")
    void unPlatoLlevaVariosIngredientes() {
        var carne = new PlatoInsumo(3, 10, 1);
        var arroz = new PlatoInsumo(3, 11, 2);
        assertEquals(carne.getPlatoId(), arroz.getPlatoId());
        assertNotEquals(carne.getInsumoId(), arroz.getInsumoId());
    }

    @Test
    @DisplayName("no valida la cantidad: acepta cero y negativos")
    void noValidaLaCantidad() {
        // Queda documentado. La validacion vive en el servicio, no aqui.
        assertEquals(0, new PlatoInsumo(3, 10, 0).getCantidadNecesaria());
        assertEquals(-1, new PlatoInsumo(3, 10, -1).getCantidadNecesaria());
    }
}
