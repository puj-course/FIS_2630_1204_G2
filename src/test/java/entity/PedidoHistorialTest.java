package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PedidoHistorialTest {
    @Test
    void conservaTipoEvento() throws Exception {
        var registro = new PedidoHistorial();
        registro.setTipoEvento("CREADO");
        assertEquals("CREADO", registro.getTipoEvento());
    }
}

