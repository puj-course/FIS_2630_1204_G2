package repository;

import ConexionDB.ConexionBD;
import entity.EstadoMesa;

import java.sql.*;
import java.util.Optional;

public class EstadoMesaRepository {

    public Optional<EstadoMesa> findById(long id) throws SQLException {
        String sql = "SELECT id_estado_mesa, codigo_estado, descripcion FROM estados_mesa WHERE id_estado_mesa = ?";
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

    public Optional<EstadoMesa> findByCodigoEstado(String codigo) throws SQLException {
        String sql = "SELECT id_estado_mesa, codigo_estado, descripcion FROM estados_mesa WHERE codigo_estado = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    private EstadoMesa map(ResultSet rs) throws SQLException {
        EstadoMesa e = new EstadoMesa();
        e.setId(rs.getLong("id_estado_mesa"));
        e.setCodigoEstado(rs.getString("codigo_estado"));
        e.setDescripcion(rs.getString("descripcion"));
        return e;
    }
}