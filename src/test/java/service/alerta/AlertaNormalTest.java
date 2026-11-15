package service.alerta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertaNormalTest {
    @Test
    void presentaLaEtiquetaDeStock() throws Exception {
        assertEquals("NORMAL", new AlertaNormal().getTexto());
    }
}

