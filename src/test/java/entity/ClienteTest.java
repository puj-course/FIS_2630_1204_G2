package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {
    @Test
    void conservaDocumento() throws Exception {
        var registro = new Cliente();
        registro.setDocumento("00122");
        assertEquals("00122", registro.getDocumento());
    }
}

