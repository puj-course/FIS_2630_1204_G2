package service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutenticacionServiceTest {
    @Test
    void rechazaCredencialesInvalidasYAbreSesionConLasCorrectas() throws Exception {
        var repositorio = mock(repository.UsuarioRepository.class);
        var servicio = new AutenticacionService(repositorio);
        var hash = org.mindrot.jbcrypt.BCrypt.hashpw("clave-de-prueba", org.mindrot.jbcrypt.BCrypt.gensalt(4));
        var usuario = new entity.UsuarioAutenticable(7, "M7", "Ana", "MESERO", hash, true);
        when(repositorio.buscarPorIdentificador("M7")).thenReturn(java.util.Optional.of(usuario));
        try {
            assertEquals(AutenticacionService.Resultado.CREDENCIALES_INVALIDAS, servicio.iniciarSesion("M7", "incorrecta"));
            assertFalse(session.SesionUsuario.estaAutenticado());
            assertEquals(AutenticacionService.Resultado.OK, servicio.iniciarSesion("M7", "clave-de-prueba"));
            assertEquals(7, session.SesionUsuario.getUsuarioId());
        } finally {
            session.SesionUsuario.getInstance().cerrarSesion();
        }
    }
}

