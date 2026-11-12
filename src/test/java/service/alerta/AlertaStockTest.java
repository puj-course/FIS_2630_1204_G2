package service.alerta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertaStockTest {
    @Test
    void exponeTextoYEstilosParaLaVista() throws Exception {
        AlertaStock alerta = new AlertaAgotadoFactory().crearAlerta();
        assertEquals("AGOTADO", alerta.getTexto());
        assertFalse(alerta.getEstiloCelda().isBlank());
        assertFalse(alerta.getEstiloFila().isBlank());
    }
}

