package service.alerta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertaStockBajoFactoryTest {
    @Test
    void creaLaAlertaCorrespondiente() throws Exception {
        assertInstanceOf(AlertaStockBajo.class, new AlertaStockBajoFactory().crearAlerta());
    }
}

