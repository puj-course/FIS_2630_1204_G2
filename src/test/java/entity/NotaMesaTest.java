package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotaMesaTest {
    @Test
    void conservaTextoNota() throws Exception {
        var registro = new NotaMesa();
        registro.setTextoNota("Sin gluten");
        assertEquals("Sin gluten", registro.getTextoNota());
    }
}

