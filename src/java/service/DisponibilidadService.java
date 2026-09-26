package service;

import java.sql.*;
import java.util.*;
import conf.ConexionDB;

public class DisponibilidadService {
    private PlatoInsumoRepository platoInsumoRepo = new PlatoInsumoRepository();

    public boolean tieneInsumosDisponibles(int platoId) throws SQLException {
        List<PlatoInsumo> requeridos = platoInsumoRepo.obtenerPorPlato(platoId);

        for (PlatoInsumo pi : requeridos) {
            int stockActual = obtenerStockInsumo(pi.getInsumoId());
            if (stockActual < pi.getCantidadNecesaria()) {
                return false; // falta este insumo → el plato no se puede preparar
            }
        }
        return true; // todos los insumos alcanzan
    }

    private int obtenerStockInsumo(int insumoId) throws SQLException {
        String sql = "SELECT stock_actual FROM insumo WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, insumoId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("stock_actual");
            }
            return 0;
        }
    }

    // Actualiza el campo "disponible" del plato en la base de datos
    public void actualizarDisponibilidad(int platoId) throws SQLException {
        boolean disponible = tieneInsumosDisponibles(platoId);

        String sql = "UPDATE producto_menu SET disponible = ? WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, disponible);
            stmt.setInt(2, platoId);
            stmt.executeUpdate();
        }
    }
}
