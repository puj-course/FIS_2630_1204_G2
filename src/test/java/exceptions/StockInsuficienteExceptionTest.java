package exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StockInsuficienteException avisa que no alcanza el inventario para preparar lo que se pidio.
 */
class StockInsuficienteExceptionTest {

    private static final String MENSAJE = "No hay suficiente 'Carne de res' para preparar el pedido";

    @Test
    void conservaElMensajeDelDominio() throws Exception {
        assertEquals(MENSAJE, new StockInsuficienteException(MENSAJE).getMessage());
    }

    @Test
    @DisplayName("no obliga a declararla: es una excepcion sin revisar")
    void esSinRevisar() {
        // Importa porque los servicios la dejan subir sin ponerla en el throws
        // y el rollback la atrapa junto con las demas RuntimeException.
        var error = new StockInsuficienteException(MENSAJE);
        assertInstanceOf(RuntimeException.class, error);
        assertFalse(java.sql.SQLException.class.isInstance(error));
    }

    @Test
    @DisplayName("se lanza y se atrapa por su propio tipo")
    void seAtrapaPorSuTipo() {
        var atrapada = assertThrows(StockInsuficienteException.class, () -> {
            throw new StockInsuficienteException(MENSAJE);
        });
        assertEquals(MENSAJE, atrapada.getMessage());
    }

    @Test
    @DisplayName("no se confunde con las otras excepciones del dominio")
    void noSeConfundeConOtra() {
        var error = new StockInsuficienteException(MENSAJE);
        assertFalse(AdicionalNoDisponibleException.class.isInstance(error));
    }

    @Test
    @DisplayName("nace sin causa: el mensaje es toda la explicacion")
    void naceSinCausa() {
        assertNull(new StockInsuficienteException(MENSAJE).getCause());
    }

    @Test
    @DisplayName("tambien acepta quedarse sin mensaje, aunque no ayude")
    void aceptaMensajeNulo() {
        // Queda documentado: no valida la entrada, asi que el mensaje nulo
        // llega hasta la pantalla del usuario.
        assertNull(new StockInsuficienteException(null).getMessage());
    }
}
