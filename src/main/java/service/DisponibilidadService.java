package service;

import java.sql.*;
import java.util.*;
import ConexionBD.ConexionBD;
import entity.PlatoInsumo;
import repository.PlatoInsumoRepository;

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
        // La tabla se llama "ingredientes" y su llave es "ingrediente_id".
        // Antes decia "FROM insumo WHERE id = ?", una tabla que no existe (HU-115).
        String sql = "SELECT stock_actual FROM ingredientes WHERE ingrediente_id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, insumoId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("stock_actual");
            }
            return 0;
        }
    }

    // Actualiza la disponibilidad del plato en la base de datos.
    public void actualizarDisponibilidad(int platoId) throws SQLException {
        boolean disponible = tieneInsumosDisponibles(platoId);

        // La tabla se llama "productos" y su llave es "producto_id". No tiene una
        // columna booleana "disponible": guarda un "estado" de tres valores
        // (DISPONIBLE, AGOTADO, INACTIVO). Antes decia
        // "UPDATE producto_menu SET disponible = ? WHERE id = ?" (HU-115).
        //
        // El filtro por estado protege a los productos INACTIVO: un plato que el
        // administrador saco del menu no vuelve solo porque haya insumos.
        String sql = "UPDATE productos SET estado = ? "
                   + "WHERE producto_id = ? AND estado <> 'INACTIVO'";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, disponible ? "DISPONIBLE" : "AGOTADO");
            stmt.setInt(2, platoId);
            stmt.executeUpdate();
        }
    }
}
