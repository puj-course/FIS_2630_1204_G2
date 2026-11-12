package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZonaTest {
    @Test
    void conservaNombreZona() throws Exception {
        var registro = new Zona();
        registro.setNombreZona("Terraza");
        assertEquals("Terraza", registro.getNombreZona());
    }
}

