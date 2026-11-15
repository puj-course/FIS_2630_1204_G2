package service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ListaEsperaService anota los grupos que esperan mesa. La validacion de la
 * cantidad de personas se resuelve antes de tocar la base de datos.
 */
class ListaEsperaServiceTest {

    private final ListaEsperaService servicio = new ListaEsperaService();

    @Test
    void rechazaGruposVacios() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarEnEspera("Ana", 0));
    }

    @Test
    @DisplayName("un grupo de cero personas se rechaza con un mensaje claro")
    void rechazaElGrupoDeCero() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarEnEspera("Ana", 0));
        assertEquals("La cantidad de personas debe ser mayor a 0.", error.getMessage());
    }

    @Test
    @DisplayName("una cantidad negativa tambien se rechaza")
    void rechazaLaCantidadNegativa() {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarEnEspera("Ana", -1));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarEnEspera("Ana", -50));
    }

    @Test
    @DisplayName("el nombre en blanco no se valida: lo que se valida es la cantidad")
    void elNombreNoSeValida() {
        // Queda documentado. Si se quisiera exigir el nombre habria que
        // agregarlo aqui, porque hoy no se revisa.
        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarEnEspera("", 0));
        assertTrue(error.getMessage().contains("personas"));
    }

    @Test
    @DisplayName("la validacion corre antes de ir a la base de datos")
    void validaAntesDeIrALaBase() {
        // Si llegara a la base el error seria SQLException. Es lo que permite
        // correr esta prueba sin base levantada.
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarEnEspera("Ana", 0));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarEnEspera(null, 0));
    }

    @Test
    @DisplayName("un grupo de una persona pasa la validacion y queda armado")
    void unGrupoDeUnaPersonaPasa() throws Exception {
        var anotado = servicio.registrarEnEspera("Ana", 1);
        assertNotNull(anotado);
        assertEquals("Ana", anotado.getNombreCliente());
        assertEquals(1, anotado.getCantidadPersonas());
    }

    @Test
    @DisplayName("el grupo anotado queda en espera y con la hora de llegada")
    void elGrupoQuedaEnEsperaConSuHora() throws Exception {
        var antes = java.time.LocalDateTime.now().minusSeconds(5);
        var anotado = servicio.registrarEnEspera("Familia Lopez", 4);

        assertEquals(enums.EstadoListaEspera.EN_ESPERA, anotado.getEstado());
        assertNotNull(anotado.getHoraIngreso());
        assertTrue(anotado.getHoraIngreso().isAfter(antes));
    }

    @Test
    @DisplayName("un grupo grande tambien pasa: no hay tope de personas")
    void noHayTopeDePersonas() throws Exception {
        assertEquals(40, servicio.registrarEnEspera("Grupo grande", 40).getCantidadPersonas());
    }

    @Test
    @DisplayName("el grupo anotado todavia no tiene identificador")
    void elGrupoAnotadoNoTieneIdentificador() throws Exception {
        // ListaEsperaRepository.save() todavia no escribe en la base: devuelve
        // el mismo objeto sin tocarlo. Queda documentado aqui para que el dia
        // que se implemente esta prueba falle y se actualice a proposito.
        assertNull(servicio.registrarEnEspera("Ana", 2).getId());
    }
}
