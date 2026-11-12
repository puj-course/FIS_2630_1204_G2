package exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdicionalNoDisponibleExceptionTest {
    @Test
    void conservaElMensajeDelDominio() throws Exception {
        assertEquals("Operación rechazada", new AdicionalNoDisponibleException("Operación rechazada").getMessage());
    }
}

