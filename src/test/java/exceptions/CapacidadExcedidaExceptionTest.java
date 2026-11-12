package exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CapacidadExcedidaExceptionTest {
    @Test
    void explicaPorQueSeRechazaLaAsignacion() throws Exception {
        assertTrue(new CapacidadExcedidaException().getMessage().contains("capacidad máxima"));
    }
}

