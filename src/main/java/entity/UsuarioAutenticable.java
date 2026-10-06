package entity;

/**
 * Los datos de un usuario que hacen falta para iniciar sesión. Es solo lo
 * necesario para autenticar: no reemplaza a una entidad Usuario completa.
 *
 * @param idUsuario      identificador en la tabla usuarios
 * @param codigoEmpleado código con el que el usuario se identifica al entrar
 * @param nombreCompleto nombre y apellido, para mostrarlo en la interfaz
 * @param rol            nombre del rol, tal como está en la tabla roles
 * @param passwordHash   contraseña almacenada, en formato sha256$salt$hash
 * @param activo         false si el usuario fue dado de baja
 */
public record UsuarioAutenticable(int idUsuario,
                                  String codigoEmpleado,
                                  String nombreCompleto,
                                  String rol,
                                  String passwordHash,
                                  boolean activo) {
}
