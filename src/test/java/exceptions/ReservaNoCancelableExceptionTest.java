package exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReservaNoCancelableException avisa que se intenta cancelar una reserva que ya paso o ya se cumplio.
 */
class ReservaNoCancelableExceptionTest {

    private static final String MENSAJE = "La reserva del 12 de noviembre ya fue completada";

    @Test
    void conservaElMensajeDelDominio() throws Exception {
        assertEquals(MENSAJE, new ReservaNoCancelableException(MENSAJE).getMessage());
    }

    @Test
    @DisplayName("no obliga a declararla: es una excepcion sin revisar")
    void esSinRevisar() {
        // Importa porque los servicios la dejan subir sin ponerla en el throws
        // y el rollback la atrapa junto con las demas RuntimeException.
        var error = new ReservaNoCancelableException(MENSAJE);
        assertInstanceOf(RuntimeException.class, error);
        assertFalse(java.sql.SQLException.class.isInstance(error));
    }

    @Test
    @DisplayName("se lanza y se atrapa por su propio tipo")
    void seAtrapaPorSuTipo() {
        var atrapada = assertThrows(ReservaNoCancelableException.class, () -> {
            throw new ReservaNoCancelableException(MENSAJE);
        });
        assertEquals(MENSAJE, atrapada.getMessage());
    }

    @Test
    @DisplayName("no se confunde con las otras excepciones del dominio")
    void noSeConfundeConOtra() {
        var error = new ReservaNoCancelableException(MENSAJE);
        assertFalse(ReservaNotFoundException.class.isInstance(error));
    }

    @Test
    @DisplayName("nace sin causa: el mensaje es toda la explicacion")
    void naceSinCausa() {
        assertNull(new ReservaNoCancelableException(MENSAJE).getCause());
    }

    @Test
    @DisplayName("tambien acepta quedarse sin mensaje, aunque no ayude")
    void aceptaMensajeNulo() {
        // Queda documentado: no valida la entrada, asi que el mensaje nulo
        // llega hasta la pantalla del usuario.
        assertNull(new ReservaNoCancelableException(null).getMessage());
    }
}
