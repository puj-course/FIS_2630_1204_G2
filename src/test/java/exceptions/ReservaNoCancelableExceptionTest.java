package exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReservaNoCancelableExceptionTest {
    @Test
    void conservaElMensajeDelDominio() throws Exception {
        assertEquals("Operación rechazada", new ReservaNoCancelableException("Operación rechazada").getMessage());
    }
}

