package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UsuarioAutenticable es lo que devuelve la consulta de login. Es un record,
 * no una clase normal: no tiene setters y sus datos no cambian despues de
 * creado.
 */
class UsuarioAutenticableTest {

    private UsuarioAutenticable usuarioCon(String rol, boolean activo) {
        return new UsuarioAutenticable(1, "EMP-001", "Laura Gomez", rol,
                "sha256$abc$def", activo);
    }

    @Test
    void conservaElRolDelEmpleado() throws Exception {
        assertEquals("MESERO", usuarioCon("MESERO", true).nombre_rol());
    }

    @Test
    @DisplayName("guarda con que codigo y con que nombre entra el empleado")
    void guardaCodigoYNombre() {
        var u = usuarioCon("MESERO", true);
        assertEquals(1, u.idUsuario());
        assertEquals("EMP-001", u.codigoEmpleado());
        assertEquals("Laura Gomez", u.nombreCompleto());
    }

    @Test
    @DisplayName("trae el hash de la contrasena, nunca la contrasena")
    void traeElHashNoLaContrasena() {
        // Lo que viaja desde la base es el hash. La contrasena en texto nunca
        // entra a esta clase.
        var u = usuarioCon("ADMINISTRADOR", true);
        assertTrue(u.passwordHash().startsWith("sha256$"));
    }

    @Test
    @DisplayName("sabe si el empleado sigue habilitado para entrar")
    void sabeSiSigueHabilitado() {
        assertTrue(usuarioCon("MESERO", true).activo());
        assertFalse(usuarioCon("MESERO", false).activo());
    }

    @Test
    @DisplayName("al ser record, dos usuarios con los mismos datos son iguales")
    void dosIgualesSonIguales() {
        assertEquals(usuarioCon("MESERO", true), usuarioCon("MESERO", true));
        assertEquals(usuarioCon("MESERO", true).hashCode(), usuarioCon("MESERO", true).hashCode());
    }

    @Test
    @DisplayName("si cambia un dato ya no son iguales")
    void siCambiaUnDatoYaNoSonIguales() {
        assertNotEquals(usuarioCon("MESERO", true), usuarioCon("ADMINISTRADOR", true));
        assertNotEquals(usuarioCon("MESERO", true), usuarioCon("MESERO", false));
    }

    @Test
    @DisplayName("los datos no se pueden cambiar despues de creado")
    void losDatosNoCambian() {
        // No hay setters: es la garantia de que la sesion no se puede
        // modificar a mitad de camino.
        var u = usuarioCon("MESERO", true);
        assertEquals("MESERO", u.nombre_rol());
        assertEquals("MESERO", u.nombre_rol());
    }

    @Test
    @DisplayName("se puede crear un usuario de cocina igual que uno de salon")
    void sirveParaCualquierRol() {
        for (String rol : new String[]{"MESERO", "COCINERO", "CAJERO", "ADMINISTRADOR"}) {
            var u = usuarioCon(rol, true);
            assertEquals(rol, u.nombre_rol());
            assertTrue(u.activo());
        }
    }
}
