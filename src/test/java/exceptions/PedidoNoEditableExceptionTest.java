package exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PedidoNoEditableExceptionTest {
    @Test
    void conservaElMensajeDelDominio() throws Exception {
        assertEquals("Operación rechazada", new PedidoNoEditableException("Operación rechazada").getMessage());
    }
}

