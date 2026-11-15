package service.alerta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertaNormalFactoryTest {
    @Test
    void creaLaAlertaCorrespondiente() throws Exception {
        assertInstanceOf(AlertaNormal.class, new AlertaNormalFactory().crearAlerta());
    }
}

