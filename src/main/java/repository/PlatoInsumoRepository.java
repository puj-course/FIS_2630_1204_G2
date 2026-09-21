package repository;

import java.sql.*;
import java.util.*;
import conf.ConexionDB;
import entity.PlatoInsumo;

public class PlatoInsumoRepository {

    public void guardar(PlatoInsumo pi) throws SQLException {
        String sql = "INSERT INTO plato_insumo (plato_id, insumo_id, cantidad_necesaria) VALUES (?, ?, ?)";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, pi.getPlatoId());
            stmt.setInt(2, pi.getInsumoId());
            stmt.setInt(3, pi.getCantidadNecesaria());
            stmt.executeUpdate();
        }
    }

    // Trae todos los insumos que necesita un plato específico
    public List<PlatoInsumo> obtenerPorPlato(int platoId) throws SQLException {
        List<PlatoInsumo> lista = new ArrayList<>();
        String sql = "SELECT id, plato_id, insumo_id, cantidad_necesaria FROM plato_insumo WHERE plato_id = ?";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, platoId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                PlatoInsumo pi = new PlatoInsumo();
                pi.setId(rs.getInt("id"));
                pi.setPlatoId(rs.getInt("plato_id"));
                pi.setInsumoId(rs.getInt("insumo_id"));
                pi.setCantidadNecesaria(rs.getInt("cantidad_necesaria"));
                lista.add(pi);
            }
        }
        return lista;
    }
}
