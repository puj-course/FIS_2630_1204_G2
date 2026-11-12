package exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsignacionActivaExceptionTest {
    @Test
    void conservaElMensajeDelDominio() throws Exception {
        assertEquals("Operación rechazada", new AsignacionActivaException("Operación rechazada").getMessage());
    }
}

