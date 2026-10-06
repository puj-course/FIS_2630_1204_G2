package repository;

import ConexionDB.ConexionBD;
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
            "JOIN roles r ON r.id_rol = u.id_rol " +
            "WHERE LOWER(u.codigo_empleado) = LOWER(?) OR LOWER(u.correo) = LOWER(?)";

    /**
     * Busca al usuario por código de empleado o por correo. Se aceptan los dos
     * porque la tabla los tiene marcados como únicos y el usuario puede recordar
     * cualquiera de ellos.
     *
     * @return el usuario, o vacío si no existe ninguno con ese identificador
     */
    public Optional<UsuarioAutenticable> buscarPorIdentificador(String identificador) throws SQLException {
        if (identificador == null || identificador.isBlank()) {
            return Optional.empty();
        }

        String valor = identificador.trim();

        try (Connection conn = ConexionBD.getConnection();
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
                        rs.getInt("is_active") == 1));
            }
        }
    }
}
