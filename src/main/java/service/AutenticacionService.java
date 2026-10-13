package service;

import entity.UsuarioAutenticable;
import org.mindrot.jbcrypt.BCrypt;
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
 * Soporta dos formatos de contraseña durante la migración:
 *
 * SHA-256:
 * sha256$<salt>$<hash>
 *
 * BCrypt:
 * $2a$... / $2b$... / $2y$...
 *
 * Las contraseñas SHA-256 existentes se migran automáticamente a BCrypt
 * después de un inicio de sesión correcto.
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
     *
     * Si la contraseña almacenada todavía utiliza SHA-256, se migra
     * automáticamente a BCrypt después de verificarla correctamente.
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

            String passwordHash = usuario.passwordHash();

            if (!contrasenaCoincide(contrasena, passwordHash)) {
                return Resultado.CREDENCIALES_INVALIDAS;
            }

            if (!usuario.activo()) {
                return Resultado.USUARIO_INACTIVO;
            }

            /*
             * Si todavía utiliza SHA-256, aprovechamos este inicio de sesión
             * correcto para migrar la contraseña a BCrypt.
             */
            if (esHashSha256(passwordHash)) {
                String nuevoHash = BCrypt.hashpw(
                        contrasena,
                        BCrypt.gensalt(12)
                );

                usuarioRepository.actualizarPasswordHash(
                        usuario.idUsuario(),
                        nuevoHash
                );
            }

            SesionUsuario.iniciarSesion(
                    usuario.idUsuario(),
                    usuario.nombreCompleto(),
                    usuario.nombre_rol()
            );

            return Resultado.OK;

        } catch (SQLException e) {
            System.err.println(
                    "No se pudo consultar o actualizar la tabla usuarios: "
                            + e.getMessage()
            );
            return Resultado.ERROR_DE_CONEXION;

        } catch (IllegalArgumentException e) {
            System.err.println(
                    "El hash de contraseña almacenado no es válido: "
                            + e.getMessage()
            );
            return Resultado.CREDENCIALES_INVALIDAS;
        }
    }

    /** Mensaje para mostrarle al usuario, según el resultado. */
    public static String mensajeDe(Resultado resultado) {
        return switch (resultado) {
            case CAMPOS_VACIOS ->
                    "Escriba su usuario y su contraseña.";

            case CREDENCIALES_INVALIDAS ->
                    "Usuario o contraseña incorrectos.";

            case USUARIO_INACTIVO ->
                    "Este usuario está inactivo. Comuníquese con el administrador.";

            case ERROR_DE_CONEXION ->
                    "No se pudo conectar con la base de datos. Intente de nuevo.";

            case OK -> "";
        };
    }

    // ------------------------------------------------------------------

    /**
     * Comprueba una contraseña contra un hash SHA-256 o BCrypt.
     */
    static boolean contrasenaCoincide(String contrasena, String almacenado) {
        if (almacenado == null || almacenado.isBlank()) {
            return false;
        }

        /*
         * Contraseña almacenada con BCrypt.
         */
        if (esHashBcrypt(almacenado)) {
            try {
                return BCrypt.checkpw(contrasena, almacenado);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        /*
         * Contraseña almacenada con el formato antiguo SHA-256.
         */
        if (esHashSha256(almacenado)) {
            String[] partes = almacenado.split("\\$");

            if (partes.length != 3) {
                return false;
            }

            String calculado = hash(partes[1], contrasena);

            return MessageDigest.isEqual(
                    calculado.getBytes(StandardCharsets.UTF_8),
                    partes[2].getBytes(StandardCharsets.UTF_8)
            );
        }

        /*
         * No conocemos el formato del hash almacenado.
         */
        return false;
    }

    /**
     * Determina si el hash utiliza BCrypt.
     */
    private static boolean esHashBcrypt(String almacenado) {
        return almacenado.startsWith("$2a$")
                || almacenado.startsWith("$2b$")
                || almacenado.startsWith("$2y$");
    }

    /**
     * Determina si el hash utiliza el formato SHA-256 antiguo.
     */
    private static boolean esHashSha256(String almacenado) {
        return almacenado.startsWith("sha256$");
    }

    /**
     * Genera un hash BCrypt para una contraseña nueva.
     */
    public static String generarHash(String contrasena) {
        return BCrypt.hashpw(
                contrasena,
                BCrypt.gensalt(12)
        );
    }

    /**
     * Método conservado para compatibilidad con código antiguo.
     *
     * @deprecated Las contraseñas nuevas deben utilizar {@link #generarHash(String)}.
     */
    @Deprecated
    public static String generarHash(String salt, String contrasena) {
        return "sha256$" + salt + "$" + hash(salt, contrasena);
    }

    private static String hash(String salt, String contrasena) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");

            byte[] resumen = sha.digest(
                    (salt + contrasena).getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex = new StringBuilder(resumen.length * 2);

            for (byte b : resumen) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 no disponible",
                    e
            );
        }
    }
}