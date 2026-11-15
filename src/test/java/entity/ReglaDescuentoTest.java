package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReglaDescuento es la regla que configura el administrador: cuanto se
 * descuenta y si ese numero es un porcentaje o son pesos.
 */
class ReglaDescuentoTest {

    @Test
    void conservaElValorYElTipo() throws Exception {
        var r = new ReglaDescuento("modificacion", 15.0, true);
        assertEquals(15.0, r.getValor());
        assertTrue(r.isEsPorcentaje());
    }

    @Test
    @DisplayName("una regla en porcentaje se marca como porcentaje")
    void laRegaEnPorcentajeSeMarca() {
        assertTrue(new ReglaDescuento("modificacion", 10.0, true).isEsPorcentaje());
    }

    @Test
    @DisplayName("una regla en pesos no se marca como porcentaje")
    void laReglaEnPesosNoSeMarca() {
        // El mismo numero significa cosas distintas segun el indicador: 5000
        // como porcentaje no tendria sentido.
        assertFalse(new ReglaDescuento("adicion", 5000.0, false).isEsPorcentaje());
    }

    @Test
    @DisplayName("guarda si la regla modifica una existente o agrega una nueva")
    void guardaElTipoDeCambio() {
        assertEquals("modificacion", new ReglaDescuento("modificacion", 10.0, true).getTipoCambio());
        assertEquals("adicion", new ReglaDescuento("adicion", 10.0, true).getTipoCambio());
    }

    @Test
    @DisplayName("el constructor vacio deja la regla sin configurar")
    void elConstructorVacioDejaTodoSinConfigurar() {
        var r = new ReglaDescuento();
        assertEquals(0, r.getId());
        assertEquals(0.0, r.getValor());
        assertFalse(r.isEsPorcentaje());
        assertNull(r.getTipoCambio());
    }

    @Test
    @DisplayName("se puede cambiar el valor sin crear otra regla")
    void seCambiaElValor() {
        var r = new ReglaDescuento("modificacion", 10.0, true);
        r.setValor(20.0);
        assertEquals(20.0, r.getValor());
    }

    @Test
    @DisplayName("se puede pasar de porcentaje a pesos")
    void seCambiaDePorcentajeAPesos() {
        var r = new ReglaDescuento("modificacion", 10.0, true);
        r.setEsPorcentaje(false);
        assertFalse(r.isEsPorcentaje());
    }

    @Test
    @DisplayName("no valida el valor: acepta negativos y porcentajes mayores a 100")
    void noValidaElValor() {
        // Queda documentado. Quien valida es ReglaDescuentoService.
        assertEquals(-5.0, new ReglaDescuento("modificacion", -5.0, true).getValor());
        assertEquals(150.0, new ReglaDescuento("modificacion", 150.0, true).getValor());
    }
}
