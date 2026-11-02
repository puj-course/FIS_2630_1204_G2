package repository;

import ConexionBD.ConexionBD;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HU-134 - disponibilidad basada en plato_insumo")
class DisponibilidadHu134Test {

    private Connection cn;
    private long productoId;
    private long ingredienteId;

    @BeforeEach
    void prepararDatos() throws SQLException {
        cn = ConexionBD.getConnection();
        cn.setAutoCommit(false);

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT p.producto_id,
                            i.ingrediente_id
                     FROM productos p
                     CROSS JOIN ingredientes i
                     WHERE p.estado <> 'INACTIVO'
                       AND i.estado = 'ACTIVO'
                     ORDER BY p.producto_id, i.ingrediente_id
                     LIMIT 1
                     """)) {

            Assumptions.assumeTrue(
                    rs.next(),
                    "Se necesita al menos un producto activo y un ingrediente activo."
            );

            productoId = rs.getLong("producto_id");
            ingredienteId = rs.getLong("ingrediente_id");
        } catch (SQLException | RuntimeException e) {
            cn.rollback();
            cn.close();
            throw e;
        }
    }

    @AfterEach
    void restaurarDatos() throws SQLException {
        if (cn != null && !cn.isClosed()) {
            try {
                cn.rollback();
            } finally {
                cn.close();
            }
        }
    }

    @Test
    @DisplayName("Hay disponibilidad cuando el stock del ingrediente es suficiente")
    void stockSuficiente() throws SQLException {
        prepararRecetaYStock(100, 5);

        assertTrue(disponible(1),
                "El producto debería estar disponible con stock suficiente.");
    }

    @Test
    @DisplayName("No hay disponibilidad cuando el stock del ingrediente es insuficiente")
    void stockInsuficiente() throws SQLException {
        prepararRecetaYStock(1, 5);

        assertFalse(disponible(1),
                "El producto no debería estar disponible si faltan ingredientes.");
    }

    @Test
    @DisplayName("Un producto sin receta se evalúa por su propio stock")
    void productoSinReceta() throws SQLException {
        eliminarReceta();
        actualizarStockProducto(3);

        assertTrue(disponible(2),
                "Debe estar disponible si su stock alcanza para dos unidades.");

        assertFalse(disponible(4),
                "No debe estar disponible si se solicitan más unidades que su stock.");
    }

    @Test
    @DisplayName("Un producto INACTIVO nunca se considera disponible")
    void productoInactivo() throws SQLException {
        eliminarReceta();
        actualizarEstado("INACTIVO");

        assertFalse(disponible(1),
                "Un producto INACTIVO no debe considerarse disponible.");
    }

    private void prepararRecetaYStock(
            int stockIngrediente,
            int cantidadNecesaria
    ) throws SQLException {

        eliminarReceta();

        try (PreparedStatement ps = cn.prepareStatement("""
                INSERT INTO plato_insumo
                    (plato_id, insumo_id, cantidad_necesaria)
                VALUES (?, ?, ?)
                """)) {
            ps.setLong(1, productoId);
            ps.setLong(2, ingredienteId);
            ps.setInt(3, cantidadNecesaria);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE ingredientes
                   SET stock_actual = ?
                 WHERE ingrediente_id = ?
                """)) {
            ps.setInt(1, stockIngrediente);
            ps.setLong(2, ingredienteId);
            ps.executeUpdate();
        }

        actualizarEstado("DISPONIBLE");
    }

    private void eliminarReceta() throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "DELETE FROM plato_insumo WHERE plato_id = ?")) {
            ps.setLong(1, productoId);
            ps.executeUpdate();
        }
    }

    private void actualizarStockProducto(int stock) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE productos
                   SET stock_actual = ?
                 WHERE producto_id = ?
                """)) {
            ps.setInt(1, stock);
            ps.setLong(2, productoId);
            ps.executeUpdate();
        }

        actualizarEstado("DISPONIBLE");
    }

    private void actualizarEstado(String estado) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE productos
                   SET estado = ?
                 WHERE producto_id = ?
                """)) {
            ps.setString(1, estado);
            ps.setLong(2, productoId);
            ps.executeUpdate();
        }
    }

    private boolean disponible(int cantidad) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT fn_producto_disponible_cantidad(?, ?)")) {
            ps.setLong(1, productoId);
            ps.setInt(2, cantidad);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }
}