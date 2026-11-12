package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatoInsumoTest {
    @Test
    void conservaCantidadNecesaria() throws Exception {
        var registro = new PlatoInsumo();
        registro.setCantidadNecesaria(4);
        assertEquals(4, registro.getCantidadNecesaria());
    }
}

