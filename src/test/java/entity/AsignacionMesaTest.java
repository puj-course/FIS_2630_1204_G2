package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsignacionMesa dice que mesero quedo a cargo de que mesa y desde cuando.
 * Mientras siga activa, esa mesa no se le puede asignar a otro.
 */
class AsignacionMesaTest {

    @Test
    void conservaLaMesaYElMesero() throws Exception {
        var a = new AsignacionMesa();
        a.setMesaId(4L);
        a.setUsuarioId(9L);
        assertEquals(4L, a.getMesaId());
        assertEquals(9L, a.getUsuarioId());
    }

    @Test
    @DisplayName("una asignacion nueva nace activa")
    void naceActiva() {
        assertTrue(new AsignacionMesa().getIsActive());
    }

    @Test
    @DisplayName("una asignacion nueva se marca con la fecha del momento")
    void naceConLaFechaDelMomento() {
        var antes = LocalDateTime.now().minusSeconds(5);
        var a = new AsignacionMesa();
        assertNotNull(a.getFechaAsignacion());
        assertTrue(a.getFechaAsignacion().isAfter(antes));
    }

    @Test
    @DisplayName("cerrar la asignacion la deja inactiva")
    void cerrarlaLaDejaInactiva() {
        // Es lo que libera la mesa para que otro mesero la pueda tomar.
        var a = new AsignacionMesa();
        a.setIsActive(false);
        assertFalse(a.getIsActive());
    }

    @Test
    @DisplayName("se puede fijar la fecha a mano al leerla de la base")
    void seFijaLaFechaAlLeerla() {
        var cuando = LocalDateTime.of(2026, 11, 10, 19, 30);
        var a = new AsignacionMesa();
        a.setFechaAsignacion(cuando);
        assertEquals(cuando, a.getFechaAsignacion());
    }

    @Test
    @DisplayName("una asignacion nueva todavia no tiene identificador")
    void naceSinIdentificador() {
        var a = new AsignacionMesa();
        assertNull(a.getId());
        assertNull(a.getMesaId());
        assertNull(a.getUsuarioId());
    }

    @Test
    @DisplayName("el indicador de activo admite nulo")
    void elIndicadorAdmiteNulo() {
        var a = new AsignacionMesa();
        a.setIsActive(null);
        assertNull(a.getIsActive());
    }
}
