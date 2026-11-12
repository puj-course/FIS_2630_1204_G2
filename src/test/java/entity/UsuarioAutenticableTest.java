package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioAutenticableTest {
    @Test
    void conservaRolEIdentificador() throws Exception {
        var usuario = new UsuarioAutenticable(21, "M-21", "Ana", "MESERO", "hash", true);
        assertEquals(21, usuario.idUsuario());
        assertEquals("MESERO", usuario.nombre_rol());
        assertTrue(usuario.activo());
    }
}

