package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsignacionMesaTest {
    @Test
    void conservaUsuarioId() throws Exception {
        var registro = new AsignacionMesa();
        registro.setUsuarioId(15L);
        assertEquals(15L, registro.getUsuarioId());
    }
}

