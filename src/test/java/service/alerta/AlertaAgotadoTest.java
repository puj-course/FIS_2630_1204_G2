package service.alerta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertaAgotadoTest {
    @Test
    void presentaLaEtiquetaDeStock() throws Exception {
        assertEquals("AGOTADO", new AlertaAgotado().getTexto());
    }
}

