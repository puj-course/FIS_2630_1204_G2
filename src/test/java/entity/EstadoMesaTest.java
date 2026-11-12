package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoMesaTest {
    @Test
    void conservaCodigoEstado() throws Exception {
        var registro = new EstadoMesa();
        registro.setCodigoEstado("RESERVADA");
        assertEquals("RESERVADA", registro.getCodigoEstado());
    }
}

