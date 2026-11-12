package exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StockInsuficienteExceptionTest {
    @Test
    void conservaElMensajeDelDominio() throws Exception {
        assertEquals("Operación rechazada", new StockInsuficienteException("Operación rechazada").getMessage());
    }
}

