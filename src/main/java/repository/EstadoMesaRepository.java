package repository;

import ConexionBD.ConexionBD;
import entity.EstadoMesa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class EstadoMesaRepository {

    public Optional<EstadoMesa> findById(long id) throws SQLException {
        String sql = """
                SELECT id_estado_mesa
                FROM estado_mesa
                WHERE id_estado_mesa = ?
                """;

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<EstadoMesa> findByCodigoEstado(Connection conn, String codigo)
            throws SQLException {

        String sql = """
                SELECT id_estado_mesa
                FROM estado_mesa
                WHERE UPPER(id_estado_mesa) = UPPER(?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codigo.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    private EstadoMesa map(ResultSet rs) throws SQLException {
        EstadoMesa estado = new EstadoMesa();

        estado.setId(rs.getLong("id_estado_mesa"));
        String nombre = rs.getString("codigo_estado");

        estado.setCodigoEstado(nombre);
        estado.setDescripcion(nombre);
        return estado;
    }
}