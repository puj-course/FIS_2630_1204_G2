package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatoTest {
    @Test
    void conservaPrecioVenta() throws Exception {
        var registro = new Plato();
        registro.setPrecioVenta(12000.0);
        assertEquals(12000.0, registro.getPrecioVenta());
    }
}

