package service;

import entity.EntradaInventario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EntradaInventarioService recibe la mercancia que llega. Las validaciones de
 * aqui abajo se resuelven antes de tocar la base de datos.
 */
class EntradaInventarioServiceTest {

    private final EntradaInventarioService servicio = new EntradaInventarioService();

    private EntradaInventario entradaDe(double cantidad) {
        return new EntradaInventario(1L, cantidad, "Compra");
    }

    @Test
    void rechazaEntradasSinCantidadPositiva() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(entradaDe(0)));
    }

    @Test
    @DisplayName("una entrada de cero unidades no tiene sentido y se rechaza")
    void rechazaLaEntradaDeCero() {
        var error = assertThrows(IllegalArgumentException.class, () -> servicio.registrar(entradaDe(0)));
        assertEquals("La cantidad debe ser mayor a cero", error.getMessage());
    }

    @Test
    @DisplayName("una entrada negativa tambien se rechaza")
    void rechazaLaEntradaNegativa() {
        // Para sacar mercancia esta el movimiento de salida, no una entrada
        // con el numero en negativo.
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(entradaDe(-1)));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(entradaDe(-100.5)));
    }

    @Test
    @DisplayName("el mensaje de error explica cual es el problema")
    void elMensajeExplicaElProblema() {
        var error = assertThrows(IllegalArgumentException.class, () -> servicio.registrar(entradaDe(-5)));
        assertNotNull(error.getMessage());
        assertTrue(error.getMessage().toLowerCase().contains("cantidad"));
    }

    @Test
    @DisplayName("la validacion corre antes de ir a la base de datos")
    void validaAntesDeIrALaBase() {
        // Si llegara a la base, el error seria SQLException y no
        // IllegalArgumentException. Esta prueba es lo que garantiza que se
        // pueda correr sin base de datos levantada.
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(entradaDe(0)));
    }

    @Test
    @DisplayName("una cantidad muy pequena pero positiva si pasa la validacion")
    void unaCantidadPequenaPasaLaValidacion() {
        // No llega a guardar porque no hay base, pero ya no falla por la
        // validacion: el error que sale es otro.
        var error = assertThrows(Exception.class, () -> servicio.registrar(entradaDe(0.001)));
        assertFalse(error instanceof IllegalArgumentException,
                "no deberia ser un problema de validacion: " + error);
    }

    @Test
    @DisplayName("una entrada sin ingrediente tampoco se puede guardar")
    void sinIngredienteNoSePuedeGuardar() {
        var sinIngrediente = new EntradaInventario(null, 5.0, "Compra");
        assertThrows(Exception.class, () -> servicio.registrar(sinIngrediente));
    }
}
