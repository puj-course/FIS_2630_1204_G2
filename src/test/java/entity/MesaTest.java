package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MesaTest {
    @Test
    void conservaCapacidad() throws Exception {
        var registro = new Mesa();
        registro.setCapacidad(6);
        assertEquals(6, registro.getCapacidad());
    }
}

