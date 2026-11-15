package service.alerta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertaAgotadoFactoryTest {
    @Test
    void creaLaAlertaCorrespondiente() throws Exception {
        assertInstanceOf(AlertaAgotado.class, new AlertaAgotadoFactory().crearAlerta());
    }
}

