package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HistorialEstadoMesaTest {
    @Test
    void conservaEstadoAnteriorId() throws Exception {
        var registro = new HistorialEstadoMesa();
        registro.setEstadoAnteriorId(3L);
        assertEquals(3L, registro.getEstadoAnteriorId());
    }
}

