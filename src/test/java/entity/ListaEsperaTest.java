package entity;

import enums.EstadoListaEspera;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ListaEspera es cada grupo anotado esperando mesa: quien es, cuantos son y
 * desde que hora espera.
 */
class ListaEsperaTest {

    private ListaEspera grupo(String nombre, int personas, EstadoListaEspera estado) {
        var l = new ListaEspera();
        l.setNombreCliente(nombre);
        l.setCantidadPersonas(personas);
        l.setEstado(estado);
        l.setHoraIngreso(LocalDateTime.now());
        return l;
    }

    @Test
    void conservaElClienteYLaCantidad() throws Exception {
        var l = grupo("Familia Lopez", 4, EstadoListaEspera.EN_ESPERA);
        assertEquals("Familia Lopez", l.getNombreCliente());
        assertEquals(4, l.getCantidadPersonas());
    }

    @Test
    @DisplayName("un grupo recien anotado queda en espera")
    void unGrupoAnotadoQuedaEnEspera() {
        assertEquals(EstadoListaEspera.EN_ESPERA,
                grupo("Familia Lopez", 4, EstadoListaEspera.EN_ESPERA).getEstado());
    }

    @Test
    @DisplayName("al sentarlo pasa a asignado y sale de la lista activa")
    void alSentarloPasaAAsignado() {
        var l = grupo("Familia Lopez", 4, EstadoListaEspera.EN_ESPERA);
        l.setEstado(EstadoListaEspera.ASIGNADO);
        assertEquals(EstadoListaEspera.ASIGNADO, l.getEstado());
        assertNotEquals(EstadoListaEspera.EN_ESPERA, l.getEstado());
    }

    @Test
    @DisplayName("si el grupo se cansa de esperar queda cancelado")
    void siSeVaQuedaCancelado() {
        var l = grupo("Familia Lopez", 4, EstadoListaEspera.EN_ESPERA);
        l.setEstado(EstadoListaEspera.CANCELADO);
        assertEquals(EstadoListaEspera.CANCELADO, l.getEstado());
    }

    @Test
    @DisplayName("guarda la hora de llegada para respetar el turno")
    void guardaLaHoraDeLlegada() {
        var temprano = LocalDateTime.of(2026, 11, 10, 19, 0);
        var tarde = LocalDateTime.of(2026, 11, 10, 19, 30);

        var primero = grupo("Familia Lopez", 4, EstadoListaEspera.EN_ESPERA);
        primero.setHoraIngreso(temprano);
        var segundo = grupo("Pareja Ruiz", 2, EstadoListaEspera.EN_ESPERA);
        segundo.setHoraIngreso(tarde);

        assertTrue(primero.getHoraIngreso().isBefore(segundo.getHoraIngreso()));
    }

    @Test
    @DisplayName("un registro nuevo nace vacio, sin estado")
    void naceVacio() {
        var l = new ListaEspera();
        assertNull(l.getId());
        assertNull(l.getEstado());
        assertNull(l.getNombreCliente());
        assertNull(l.getHoraIngreso());
        assertEquals(0, l.getCantidadPersonas());
    }

    @Test
    @DisplayName("al guardarlo la base le asigna el identificador")
    void alGuardarloRecibeIdentificador() {
        var l = grupo("Familia Lopez", 4, EstadoListaEspera.EN_ESPERA);
        l.setId(12L);
        assertEquals(12L, l.getId());
    }
}
