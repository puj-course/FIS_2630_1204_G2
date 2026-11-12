package service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la parte de AutenticacionService que no toca la base de datos:
 * la comprobacion de contrasenas y los mensajes que ve el usuario.
 *
 * Las pruebas del inicio de sesion completo, que si necesita el repositorio,
 * estan en AutenticacionServiceTest.
 */
class AutenticacionServicePuraTest {

    // ------------------------------------------------- mensajes al usuario --

    @Test
    @DisplayName("cada resultado tiene su mensaje y solo OK va sin texto")
    void cadaResultadoTieneSuMensaje() {
        assertEquals("Escriba su usuario y su contraseña.",
                AutenticacionService.mensajeDe(AutenticacionService.Resultado.CAMPOS_VACIOS));

        assertEquals("Usuario o contraseña incorrectos.",
                AutenticacionService.mensajeDe(AutenticacionService.Resultado.CREDENCIALES_INVALIDAS));

        assertEquals("Este usuario está inactivo. Comuníquese con el administrador.",
                AutenticacionService.mensajeDe(AutenticacionService.Resultado.USUARIO_INACTIVO));

        assertEquals("No se pudo conectar con la base de datos. Intente de nuevo.",
                AutenticacionService.mensajeDe(AutenticacionService.Resultado.ERROR_DE_CONEXION));

        assertEquals("", AutenticacionService.mensajeDe(AutenticacionService.Resultado.OK));
    }

    @Test
    @DisplayName("ningun mensaje de error queda vacio")
    void ningunMensajeDeErrorQuedaVacio() {
        for (AutenticacionService.Resultado r : AutenticacionService.Resultado.values()) {
            String mensaje = AutenticacionService.mensajeDe(r);
            assertNotNull(mensaje, "el resultado " + r + " no devolvio mensaje");
            if (r != AutenticacionService.Resultado.OK) {
                assertFalse(mensaje.isBlank(), "el resultado " + r + " deberia explicar que paso");
            }
        }
    }

    // ------------------------------------------- hash en el formato antiguo --
    //
    // generarHash(sal, clave) esta marcado como obsoleto a proposito: las
    // contrasenas nuevas usan BCrypt. Se sigue probando porque en la base
    // todavia hay usuarios guardados con el formato viejo y tienen que poder
    // entrar. Por eso estas pruebas silencian el aviso de obsoleto.

    @Test
    @DisplayName("el hash antiguo tiene el formato sha256$salt$resumen")
    @SuppressWarnings("deprecation")
    void elHashAntiguoTieneSuFormato() {
        String generado = AutenticacionService.generarHash("sal123", "miClave");

        String[] partes = generado.split("\\$");
        assertEquals(3, partes.length);
        assertEquals("sha256", partes[0]);
        assertEquals("sal123", partes[1]);
        assertEquals(64, partes[2].length(), "SHA-256 en hexadecimal son 64 caracteres");
        assertTrue(partes[2].matches("[0-9a-f]{64}"), "el resumen debe ser hexadecimal en minuscula");
    }

    @Test
    @DisplayName("la misma sal y la misma clave dan siempre el mismo hash")
    @SuppressWarnings("deprecation")
    void elHashEsDeterminista() {
        assertEquals(AutenticacionService.generarHash("sal", "clave"),
                     AutenticacionService.generarHash("sal", "clave"));
    }

    @Test
    @DisplayName("cambiar la sal cambia el hash aunque la clave sea la misma")
    @SuppressWarnings("deprecation")
    void salDistintaCambiaElHash() {
        assertNotEquals(AutenticacionService.generarHash("salA", "clave"),
                        AutenticacionService.generarHash("salB", "clave"));
    }

    @Test
    @DisplayName("cambiar la clave cambia el hash aunque la sal sea la misma")
    @SuppressWarnings("deprecation")
    void claveDistintaCambiaElHash() {
        assertNotEquals(AutenticacionService.generarHash("sal", "claveA"),
                        AutenticacionService.generarHash("sal", "claveB"));
    }

    // ---------------------------------------- comprobacion de la contrasena --

    @Test
    @DisplayName("acepta la clave correcta guardada en el formato antiguo")
    @SuppressWarnings("deprecation")
    void aceptaLaClaveCorrectaEnFormatoAntiguo() {
        String almacenado = AutenticacionService.generarHash("gfmes001", "mesero123");
        assertTrue(AutenticacionService.contrasenaCoincide("mesero123", almacenado));
    }

    @Test
    @DisplayName("rechaza una clave equivocada contra el mismo hash")
    @SuppressWarnings("deprecation")
    void rechazaLaClaveEquivocada() {
        String almacenado = AutenticacionService.generarHash("gfmes001", "mesero123");
        assertFalse(AutenticacionService.contrasenaCoincide("mesero124", almacenado));
        assertFalse(AutenticacionService.contrasenaCoincide("MESERO123", almacenado));
        assertFalse(AutenticacionService.contrasenaCoincide("", almacenado));
    }

    @Test
    @DisplayName("rechaza cuando no hay hash guardado")
    void rechazaSinHashGuardado() {
        assertFalse(AutenticacionService.contrasenaCoincide("loQueSea", null));
        assertFalse(AutenticacionService.contrasenaCoincide("loQueSea", ""));
        assertFalse(AutenticacionService.contrasenaCoincide("loQueSea", "   "));
    }

    @Test
    @DisplayName("rechaza un hash con un formato que no reconoce")
    void rechazaFormatoDesconocido() {
        assertFalse(AutenticacionService.contrasenaCoincide("clave", "md5$sal$abc"));
        assertFalse(AutenticacionService.contrasenaCoincide("clave", "textoPlano"));
        assertFalse(AutenticacionService.contrasenaCoincide("clave", "sha512$sal$abc"));
    }

    @Test
    @DisplayName("rechaza un hash sha256 mal formado en vez de reventar")
    void rechazaHashAntiguoMalFormado() {
        assertFalse(AutenticacionService.contrasenaCoincide("clave", "sha256$soloDosPartes"));
        assertFalse(AutenticacionService.contrasenaCoincide("clave", "sha256$sal$resumen$sobra"));
        assertFalse(AutenticacionService.contrasenaCoincide("clave", "sha256$"));
    }
}
