package repository;

import database.ConexionBD;
import entity.UsuarioAutenticable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Consultas de la tabla usuarios necesarias para el inicio de sesión.
 */
public class UsuarioRepository {

    private static final String SELECT_ACCESO =
            "SELECT u.id_usuario, u.codigo_empleado, u.nombre, u.apellido, " +
                    "       u.password_hash, u.is_active, r.nombre_rol " +
                    "FROM usuarios u " +
                    "JOIN roles r ON r.rol_id = u.id_rol " +
                    "WHERE LOWER(u.codigo_empleado) = LOWER(?) OR LOWER(u.correo) = LOWER(?)";

    /**
     * Busca al usuario por código de empleado o por correo.
     *
     * @return el usuario, o vacío si no existe ninguno con ese identificador
     */
    public Optional<UsuarioAutenticable> buscarPorIdentificador(String identificador) throws SQLException {
        if (identificador == null || identificador.isBlank()) {
            return Optional.empty();
        }

        String valor = identificador.trim();

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ACCESO)) {

            stmt.setString(1, valor);
            stmt.setString(2, valor);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(new UsuarioAutenticable(
                        rs.getInt("id_usuario"),
                        rs.getString("codigo_empleado"),
                        (rs.getString("nombre") + " " + rs.getString("apellido")).trim(),
                        rs.getString("nombre_rol"),
                        rs.getString("password_hash"),
                        rs.getInt("is_active") == 1
                ));
            }
        }
    }

    /**
     * Actualiza el hash de contraseña de un usuario.
     *
     * Este método se utiliza durante la migración de SHA-256 a BCrypt.
     */
    public void actualizarPasswordHash(int idUsuario, String nuevoHash) throws SQLException {
        String sql = """
                UPDATE usuarios
                SET password_hash = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id_usuario = ?
                """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nuevoHash);
            stmt.setInt(2, idUsuario);

            stmt.executeUpdate();
        }
    }
}