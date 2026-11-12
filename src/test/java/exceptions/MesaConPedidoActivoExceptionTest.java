package exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MesaConPedidoActivoExceptionTest {
    @Test
    void conservaElMensajeDelDominio() throws Exception {
        assertEquals("Operación rechazada", new MesaConPedidoActivoException("Operación rechazada").getMessage());
    }
}

