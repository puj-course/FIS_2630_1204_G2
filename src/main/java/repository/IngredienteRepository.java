package repository;

import database.ConexionBD;
import entity.Ingrediente;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IngredienteRepository {

    /**
     * Busca un ingrediente por su identificador.
     */
    public Optional<Ingrediente> findById(long ingredienteId)
            throws SQLException {

        String sql = """
                SELECT ingrediente_id,
                       nombre,
                       stock_actual,
                       stock_minimo,
                       estado
                FROM ingredientes
                WHERE ingrediente_id = ?
                """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, ingredienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Obtiene todos los ingredientes activos.
     */
    public List<Ingrediente> findAllActivos()
            throws SQLException {

        String sql = """
                SELECT ingrediente_id,
                       nombre,
                       stock_actual,
                       stock_minimo,
                       estado
                FROM ingredientes
                WHERE estado = 'ACTIVO'
                ORDER BY nombre
                """;

        List<Ingrediente> ingredientes = new ArrayList<>();

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ingredientes.add(map(rs));
            }
        }

        return ingredientes;
    }

    /**
     * Busca ingredientes activos cuyo nombre contenga
     * el texto indicado.
     */
    public List<Ingrediente> buscarActivosPorNombre(String nombre)
            throws SQLException {

        String sql = """
                SELECT ingrediente_id,
                       nombre,
                       stock_actual,
                       stock_minimo,
                       estado
                FROM ingredientes
                WHERE estado = 'ACTIVO'
                  AND LOWER(nombre) LIKE LOWER(?)
                ORDER BY nombre
                """;

        List<Ingrediente> ingredientes = new ArrayList<>();

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + nombre + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ingredientes.add(map(rs));
                }
            }
        }

        return ingredientes;
    }

    /**
     * Actualiza únicamente el stock actual de un ingrediente.
     */
    public void actualizarStock(
            long ingredienteId,
            BigDecimal nuevoStock
    ) throws SQLException {

        String sql = """
                UPDATE ingredientes
                SET stock_actual = ?,
                    fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE ingrediente_id = ?
                """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, nuevoStock);
            stmt.setLong(2, ingredienteId);

            stmt.executeUpdate();
        }
    }

    /**
     * Convierte una fila de la base de datos
     * en un objeto Ingrediente.
     */
    private Ingrediente map(ResultSet rs) throws SQLException {

        Ingrediente ingrediente = new Ingrediente();

        ingrediente.setIngredienteId(
                rs.getLong("ingrediente_id")
        );

        ingrediente.setNombre(
                rs.getString("nombre")
        );

        ingrediente.setStockActual(
                rs.getBigDecimal("stock_actual")
        );

        ingrediente.setStockMinimo(
                rs.getBigDecimal("stock_minimo")
        );

        ingrediente.setEstado(
                rs.getString("estado")
        );

        return ingrediente;
    }
}