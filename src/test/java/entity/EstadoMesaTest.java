package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EstadoMesa es la fila de la tabla de estados: el codigo que se usa en el
 * codigo y la descripcion que se le muestra al usuario.
 *
 * Ojo: no confundir con el enum enums.EstadoMesa, que es otra cosa.
 */
class EstadoMesaTest {

    private EstadoMesa estadoCon(String codigo, String descripcion) {
        var e = new EstadoMesa();
        e.setCodigoEstado(codigo);
        e.setDescripcion(descripcion);
        return e;
    }

    @Test
    void conservaElCodigoDelEstado() throws Exception {
        assertEquals("DISPONIBLE", estadoCon("DISPONIBLE", "Mesa libre").getCodigoEstado());
    }

    @Test
    @DisplayName("separa el codigo interno de lo que ve el usuario")
    void separaCodigoDeDescripcion() {
        var e = estadoCon("PENDIENTE_PAGO", "Pendiente de pago");
        assertEquals("PENDIENTE_PAGO", e.getCodigoEstado());
        assertEquals("Pendiente de pago", e.getDescripcion());
        assertNotEquals(e.getCodigoEstado(), e.getDescripcion());
    }

    @Test
    @DisplayName("un estado nuevo todavia no tiene identificador")
    void naceSinIdentificador() {
        assertNull(new EstadoMesa().getId());
    }

    @Test
    @DisplayName("al leerlo de la base trae el identificador que referencian las mesas")
    void traeElIdentificadorQueReferencianLasMesas() {
        var e = estadoCon("OCUPADA", "Mesa ocupada");
        e.setId(2L);
        assertEquals(2L, e.getId());
    }

    @Test
    @DisplayName("guarda los estados que usa el mapa del salon")
    void guardaLosEstadosDelMapa() {
        for (String codigo : new String[]{"DISPONIBLE", "OCUPADA", "RESERVADA", "MANTENIMIENTO"}) {
            assertEquals(codigo, estadoCon(codigo, "x").getCodigoEstado());
        }
    }

    @Test
    @DisplayName("no es el mismo tipo que el enum de estados")
    void noEsElEnum() {
        // Son dos clases con el mismo nombre en paquetes distintos, y vale la
        // pena dejarlo escrito para que nadie las mezcle.
        assertFalse(enums.EstadoMesa.class.isInstance(new EstadoMesa()));
    }
}
