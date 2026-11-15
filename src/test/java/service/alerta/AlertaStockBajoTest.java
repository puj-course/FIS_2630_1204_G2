package service.alerta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertaStockBajoTest {
    @Test
    void presentaLaEtiquetaDeStock() throws Exception {
        assertEquals("STOCK BAJO", new AlertaStockBajo().getTexto());
    }
}

