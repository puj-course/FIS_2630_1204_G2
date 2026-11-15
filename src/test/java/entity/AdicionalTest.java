package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adicional es lo que se le agrega a un plato y se cobra aparte: queso extra,
 * porcion de arroz, salsa.
 */
class AdicionalTest {

    @Test
    void conservaElNombreYElPrecio() throws Exception {
        var a = new Adicional();
        a.setNombre("Queso extra");
        a.setPrecioAdicional(new BigDecimal("3000"));
        assertEquals("Queso extra", a.getNombre());
        assertEquals(new BigDecimal("3000"), a.getPrecioAdicional());
    }

    @Test
    @DisplayName("un adicional nuevo nace activo")
    void naceActivo() {
        // Asi el administrador no tiene que habilitarlo despues de crearlo.
        assertTrue(new Adicional().getIsActive());
    }

    @Test
    @DisplayName("se puede desactivar sin borrarlo de la carta")
    void seDesactivaSinBorrarlo() {
        var a = new Adicional();
        a.setIsActive(false);
        assertFalse(a.getIsActive());
        assertNull(a.getNombre());
    }

    @Test
    @DisplayName("el indicador de activo admite nulo porque la columna lo admite")
    void elIndicadorAdmiteNulo() {
        var a = new Adicional();
        a.setIsActive(null);
        assertNull(a.getIsActive());
    }

    @Test
    @DisplayName("un adicional recien creado todavia no tiene identificador")
    void naceSinIdentificador() {
        var a = new Adicional();
        assertNull(a.getId());
        assertNull(a.getPrecioAdicional());
    }

    @Test
    @DisplayName("al guardarlo la base le asigna el identificador")
    void alGuardarloRecibeIdentificador() {
        var a = new Adicional();
        a.setId(7L);
        assertEquals(7L, a.getId());
    }

    @Test
    @DisplayName("el precio no se redondea: lo que se guarda es lo que se cobra")
    void elPrecioNoSeRedondea() {
        var a = new Adicional();
        a.setPrecioAdicional(new BigDecimal("3500.50"));
        assertEquals(2, a.getPrecioAdicional().scale());
    }

    @Test
    @DisplayName("un adicional gratis vale cero, no nulo")
    void unAdicionalGratisValeCero() {
        var a = new Adicional();
        a.setNombre("Salsa de la casa");
        a.setPrecioAdicional(BigDecimal.ZERO);
        assertEquals(0, a.getPrecioAdicional().signum());
    }
}
