package service;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import ConexionBD.ConexionBD;
import entity.ProductoIngrediente;
import repository.ProductoIngredienteRepository;

public class DisponibilidadService {

    private final ProductoIngredienteRepository productoIngredienteRepo = new ProductoIngredienteRepository();

    /**
     * Verifica si todos los ingredientes necesarios para un producto tienen stock suficiente.
     */
    public boolean tieneInsumosDisponibles(long productoId) throws SQLException {
        List<ProductoIngrediente> requeridos = productoIngredienteRepo.obtenerPorProducto(productoId);

        for (ProductoIngrediente pi : requeridos) {
            BigDecimal stockActual = obtenerStockInsumo(pi.getIngredienteId());

            // Si el stock actual es menor que la cantidad necesaria (stockActual < cantidadNecesaria)
            if (stockActual.compareTo(pi.getCantidadNecesaria()) < 0) {
                return false; // Falta este insumo -> el producto no se puede preparar
            }
        }
        return true; // Todos los insumos alcanzan
    }

    /**
     * Obtiene el stock actual de un ingrediente en formato BigDecimal.
     */
    private BigDecimal obtenerStockInsumo(long ingredienteId) throws SQLException {
        String sql = "SELECT stock_actual FROM ingredientes WHERE ingrediente_id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, ingredienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal stock = rs.getBigDecimal("stock_actual");
                    return stock != null ? stock : BigDecimal.ZERO;
                }
                return BigDecimal.ZERO;
            }
        }
    }

    /**
     * Actualiza la disponibilidad del producto en la base de datos (DISPONIBLE o AGOTADO),
     * protegiendo los productos marcados como INACTIVO.
     */
    public void actualizarDisponibilidad(long productoId) throws SQLException {
        boolean disponible = tieneInsumosDisponibles(productoId);

        String sql = "UPDATE productos SET estado = ? "
                + "WHERE producto_id = ? AND estado <> 'INACTIVO'";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, disponible ? "DISPONIBLE" : "AGOTADO");
            stmt.setLong(2, productoId);
            stmt.executeUpdate();
        }
    }
}