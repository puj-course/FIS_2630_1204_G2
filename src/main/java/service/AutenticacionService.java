package service;

import entity.UsuarioAutenticable;
import repository.UsuarioRepository;
import session.SesionUsuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Verifica las credenciales contra la tabla usuarios y abre la sesión.
 *
 * Las contraseñas se guardan como  sha256$&lt;salt&gt;$&lt;hash&gt;,  donde el hash es
 * SHA-256 de (salt + contraseña) en hexadecimal. El salt es distinto por
 * usuario, así que dos personas con la misma contraseña no comparten hash.
 *
 * SHA-256 con salt es suficiente para el alcance del proyecto, pero no es un
 * algoritmo pensado para contraseñas: no tiene costo configurable. Si el
 * sistema llegara a usarse de verdad, conviene cambiarlo por bcrypt o Argon2.
 * El formato lleva el prefijo del algoritmo justamente para poder migrar sin
 * tocar las filas existentes.
 */
public class AutenticacionService {

    /** Resultado de un intento de acceso. */
    public enum Resultado {
        OK,
        CREDENCIALES_INVALIDAS,
        USUARIO_INACTIVO,
        CAMPOS_VACIOS,
        ERROR_DE_CONEXION
    }

    private final UsuarioRepository usuarioRepository;

    public AutenticacionService() {
        this(new UsuarioRepository());
    }

    public AutenticacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Comprueba las credenciales y, si son correctas, deja la sesión abierta.
     * No distingue entre usuario inexistente y contraseña equivocada: las dos
     * devuelven CREDENCIALES_INVALIDAS, para no confirmarle a nadie qué códigos
     * de empleado existen.
     */
    public Resultado iniciarSesion(String identificador, String contrasena) {
        if (identificador == null || identificador.isBlank()
                || contrasena == null || contrasena.isEmpty()) {
            return Resultado.CAMPOS_VACIOS;
        }

        try {
            Optional<UsuarioAutenticable> encontrado =
                    usuarioRepository.buscarPorIdentificador(identificador);

            if (encontrado.isEmpty()) {
                return Resultado.CREDENCIALES_INVALIDAS;
            }

            UsuarioAutenticable usuario = encontrado.get();

            if (!contrasenaCoincide(contrasena, usuario.passwordHash())) {
                return Resultado.CREDENCIALES_INVALIDAS;
            }

            if (!usuario.activo()) {
                return Resultado.USUARIO_INACTIVO;
            }

            SesionUsuario.iniciarSesion(usuario.idUsuario(),
                    usuario.nombreCompleto(),
                    usuario.rol());
            return Resultado.OK;

        } catch (SQLException e) {
            System.err.println("No se pudo consultar la tabla usuarios: " + e.getMessage());
            return Resultado.ERROR_DE_CONEXION;
        }
    }

    /** Mensaje para mostrarle al usuario, según el resultado. */
    public static String mensajeDe(Resultado resultado) {
        return switch (resultado) {
            case CAMPOS_VACIOS -> "Escriba su usuario y su contraseña.";
            case CREDENCIALES_INVALIDAS -> "Usuario o contraseña incorrectos.";
            case USUARIO_INACTIVO -> "Este usuario está inactivo. Comuníquese con el administrador.";
            case ERROR_DE_CONEXION -> "No se pudo conectar con la base de datos. Intente de nuevo.";
            case OK -> "";
        };
    }

    // ------------------------------------------------------------------

    /**
     * Compara en tiempo constante para no filtrar información por lo que tarda
     * la comparación.
     */
    static boolean contrasenaCoincide(String contrasena, String almacenado) {
        if (almacenado == null) {
            return false;
        }

        String[] partes = almacenado.split("\\$");
        if (partes.length != 3 || !"sha256".equals(partes[0])) {
            return false;
        }

        String calculado = hash(partes[1], contrasena);
        return MessageDigest.isEqual(
                calculado.getBytes(StandardCharsets.UTF_8),
                partes[2].getBytes(StandardCharsets.UTF_8));
    }

    /** Genera el valor que va en password_hash para una contraseña nueva. */
    public static String generarHash(String salt, String contrasena) {
        return "sha256$" + salt + "$" + hash(salt, contrasena);
    }

    private static String hash(String salt, String contrasena) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] resumen = sha.digest((salt + contrasena).getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(resumen.length * 2);
            for (byte b : resumen) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda implementación de Java.
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
