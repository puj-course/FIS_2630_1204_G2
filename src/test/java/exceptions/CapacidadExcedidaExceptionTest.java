package exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CapacidadExcedidaException avisa que se quiso sentar mas gente de la que
 * cabe en la mesa. Es la unica del dominio que trae el mensaje adentro, sin
 * recibirlo por constructor.
 */
class CapacidadExcedidaExceptionTest {

    @Test
    void traeSuPropioMensaje() throws Exception {
        assertNotNull(new CapacidadExcedidaException().getMessage());
    }

    @Test
    @DisplayName("el mensaje explica que el problema es la capacidad de la mesa")
    void elMensajeExplicaElProblema() {
        var mensaje = new CapacidadExcedidaException().getMessage();
        assertTrue(mensaje.contains("comensales"), "deberia nombrar a los comensales: " + mensaje);
        assertTrue(mensaje.contains("capacidad"), "deberia nombrar la capacidad: " + mensaje);
    }

    @Test
    @DisplayName("todas las instancias dicen lo mismo, no depende de quien la lance")
    void todasDicenLoMismo() {
        assertEquals(new CapacidadExcedidaException().getMessage(),
                new CapacidadExcedidaException().getMessage());
    }

    @Test
    @DisplayName("no obliga a declararla: es una excepcion sin revisar")
    void esSinRevisar() {
        assertInstanceOf(RuntimeException.class, new CapacidadExcedidaException());
    }

    @Test
    @DisplayName("se lanza y se atrapa por su propio tipo")
    void seAtrapaPorSuTipo() {
        var atrapada = assertThrows(CapacidadExcedidaException.class, () -> {
            throw new CapacidadExcedidaException();
        });
        assertNotNull(atrapada.getMessage());
    }

    @Test
    @DisplayName("no se confunde con la de mesa ocupada")
    void noSeConfundeConMesaOcupada() {
        // Son dos problemas distintos: una mesa libre tambien puede quedarse
        // corta de sillas.
        assertFalse(MesaOcupadaException.class.isInstance(new CapacidadExcedidaException()));
    }

    @Test
    @DisplayName("nace sin causa")
    void naceSinCausa() {
        assertNull(new CapacidadExcedidaException().getCause());
    }
}
