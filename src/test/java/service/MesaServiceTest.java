package service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las validaciones de MesaService. Todas se detienen antes de
 * llegar al repositorio, asi que no necesitan base de datos.
 */
class MesaServiceTest {

    private final MesaService servicio = new MesaService();

    @Test
    void rechazaNumerosYCapacidadesInvalidos() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(0, 4));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(10000, 4));
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarCapacidad(1, 0));
    }

    // --------------------------------------------------- numero de la mesa --

    @Test
    @DisplayName("el numero de mesa no puede ser cero ni negativo")
    void rechazaNumeroMenorQueUno() {
        var error = assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(0, 4));
        assertEquals("El número de mesa debe estar entre 1 y 9999.", error.getMessage());

        assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(-1, 4));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(-9999, 4));
    }

    @Test
    @DisplayName("el numero de mesa no puede pasar de 9999")
    void rechazaNumeroMayorQueElLimite() {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(10000, 4));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(99999, 4));
    }

    // ------------------------------------------------------------ capacidad --

    @Test
    @DisplayName("la capacidad debe ser mayor que cero al registrar")
    void rechazaCapacidadInvalidaAlRegistrar() {
        var error = assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(5, 0));
        assertEquals("La capacidad debe ser mayor que cero.", error.getMessage());

        assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(5, -2));
    }

    @Test
    @DisplayName("la capacidad debe ser mayor que cero al cambiarla")
    void rechazaCapacidadInvalidaAlCambiarla() {
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarCapacidad(1, 0));
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarCapacidad(1, -5));
    }

    @Test
    @DisplayName("el numero se valida antes que la capacidad")
    void elNumeroSeValidaPrimero() {
        // Con los dos datos malos, el mensaje que llega es el del numero:
        // el orden de las validaciones es parte del contrato.
        var error = assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(0, 0));
        assertEquals("El número de mesa debe estar entre 1 y 9999.", error.getMessage());
    }

    @Test
    @DisplayName("los dos mensajes de error explican que esta mal")
    void losMensajesExplicanElProblema() {
        var porNumero = assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(0, 4));
        var porCapacidad = assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(5, 0));

        assertNotNull(porNumero.getMessage());
        assertNotNull(porCapacidad.getMessage());
        assertFalse(porNumero.getMessage().isBlank());
        assertFalse(porCapacidad.getMessage().isBlank());
        assertNotEquals(porNumero.getMessage(), porCapacidad.getMessage());
    }
}
