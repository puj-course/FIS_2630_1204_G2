package exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AdicionalNoDisponibleException avisa que el mesero pide un adicional que ya no se esta ofreciendo.
 */
class AdicionalNoDisponibleExceptionTest {

    private static final String MENSAJE = "El adicional 'Queso extra' no esta disponible";

    @Test
    void conservaElMensajeDelDominio() throws Exception {
        assertEquals(MENSAJE, new AdicionalNoDisponibleException(MENSAJE).getMessage());
    }

    @Test
    @DisplayName("no obliga a declararla: es una excepcion sin revisar")
    void esSinRevisar() {
        // Importa porque los servicios la dejan subir sin ponerla en el throws
        // y el rollback la atrapa junto con las demas RuntimeException.
        var error = new AdicionalNoDisponibleException(MENSAJE);
        assertInstanceOf(RuntimeException.class, error);
        assertFalse(java.sql.SQLException.class.isInstance(error));
    }

    @Test
    @DisplayName("se lanza y se atrapa por su propio tipo")
    void seAtrapaPorSuTipo() {
        var atrapada = assertThrows(AdicionalNoDisponibleException.class, () -> {
            throw new AdicionalNoDisponibleException(MENSAJE);
        });
        assertEquals(MENSAJE, atrapada.getMessage());
    }

    @Test
    @DisplayName("no se confunde con las otras excepciones del dominio")
    void noSeConfundeConOtra() {
        var error = new AdicionalNoDisponibleException(MENSAJE);
        assertFalse(StockInsuficienteException.class.isInstance(error));
    }

    @Test
    @DisplayName("nace sin causa: el mensaje es toda la explicacion")
    void naceSinCausa() {
        assertNull(new AdicionalNoDisponibleException(MENSAJE).getCause());
    }

    @Test
    @DisplayName("tambien acepta quedarse sin mensaje, aunque no ayude")
    void aceptaMensajeNulo() {
        // Queda documentado: no valida la entrada, asi que el mensaje nulo
        // llega hasta la pantalla del usuario.
        assertNull(new AdicionalNoDisponibleException(null).getMessage());
    }
}
