package session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SesionUsuarioTest {
    @Test
    void iniciaYCierraSesion() throws Exception {
        var sesion = SesionUsuario.getInstance();
        try {
            sesion.iniciarSesion(7, "Ana", "CAJERO");
            assertTrue(SesionUsuario.estaAutenticado());
            assertEquals(7, SesionUsuario.getUsuarioId());
            assertTrue(sesion.esCajero());
        } finally {
            sesion.cerrarSesion();
        }
        assertFalse(SesionUsuario.estaAutenticado());
    }
}

