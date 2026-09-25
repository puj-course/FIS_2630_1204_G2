package repository;

import ConexionDB.ConexionBD;
import entity.Zona;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ZonaRepository {

    public Optional<Zona> findById(long id) throws SQLException {
        String sql = "SELECT id_zona, nombre_zona, descripcion, is_active FROM zonas WHERE id_zona = ?";
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

    public List<Zona> findAllActivas() throws SQLException {
        String sql = "SELECT id_zona, nombre_zona, descripcion, is_active FROM zonas WHERE is_active = 1";
        List<Zona> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(map(rs));
            }
        }
        return lista;
    }

    private Zona map(ResultSet rs) throws SQLException {
        Zona z = new Zona();
        z.setId(rs.getLong("id_zona"));
        z.setNombreZona(rs.getString("nombre_zona"));
        z.setDescripcion(rs.getString("descripcion"));
        z.setIsActive(rs.getInt("is_active"));
        return z;
    }
}