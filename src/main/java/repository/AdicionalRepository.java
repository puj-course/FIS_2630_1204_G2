package repository;

import ConexionDB.ConexionBD;
import entity.Adicional;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdicionalRepository {

    public Optional<Adicional> findById(long id) throws SQLException {
        String sql = "SELECT id, nombre, precio_adicional, is_active FROM adicional WHERE id = ?";
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

    public List<Adicional> findAllActivos() throws SQLException {
        String sql = "SELECT id, nombre, precio_adicional, is_active FROM adicional WHERE is_active = TRUE";
        List<Adicional> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(map(rs));
            }
        }
        return lista;
    }

    public Adicional save(Adicional a) throws SQLException {
        if (a.getId() == null) {
            String sql = "INSERT INTO adicional (nombre, precio_adicional, is_active) VALUES (?, ?, ?) RETURNING id";
            try (Connection conn = ConexionBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, a.getNombre());
                stmt.setBigDecimal(2, a.getPrecioAdicional());
                stmt.setBoolean(3, a.getIsActive() != null ? a.getIsActive() : true);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        a.setId(rs.getLong(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE adicional SET nombre = ?, precio_adicional = ?, is_active = ? WHERE id = ?";
            try (Connection conn = ConexionBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, a.getNombre());
                stmt.setBigDecimal(2, a.getPrecioAdicional());
                stmt.setBoolean(3, a.getIsActive() != null ? a.getIsActive() : true);
                stmt.setLong(4, a.getId());
                stmt.executeUpdate();
            }
        }
        return a;
    }

    private Adicional map(ResultSet rs) throws SQLException {
        Adicional a = new Adicional();
        a.setId(rs.getLong("id"));
        a.setNombre(rs.getString("nombre"));
        a.setPrecioAdicional(rs.getBigDecimal("precio_adicional"));
        a.setIsActive(rs.getBoolean("is_active"));
        return a;
    }
}