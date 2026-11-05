package repository;

import ConexionBD.ConexionBD;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.math.BigDecimal;
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
    @DisplayName("Un producto no está disponible si el stock de sus ingredientes no alcanza para la cantidad solicitada")
    void productoConStockInsuficienteDeIngredientes() throws SQLException {
        // Configuramos un stock insuficiente (por ejemplo, 0, o un valor menor al requerido por 4 unidades)
        actualizarStockIngrediente(0);

        assertTrue(disponible(2),
                "El producto debe estar disponible si el stock alcanza.");

        assertFalse(disponible(4),
                "No debe estar disponible si se solicitan más unidades de las que el stock de los ingredientes permite.");
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
                INSERT INTO producto_ingrediente
                    (producto_id, ingrediente_id, cantidad_necesaria)
                VALUES (?, ?, ?)
                """)) {
            ps.setLong(1, productoId);
            ps.setLong(2, ingredienteId);
            ps.setBigDecimal(3, BigDecimal.valueOf(cantidadNecesaria));
            ps.executeUpdate();
        }

        try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE ingredientes
                   SET stock_actual = ?
                 WHERE ingrediente_id = ?
                """)) {
            ps.setBigDecimal(1, BigDecimal.valueOf(stockIngrediente));
            ps.setLong(2, ingredienteId);
            ps.executeUpdate();
        }

        actualizarEstado("DISPONIBLE");
    }

    private void eliminarReceta() throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "DELETE FROM producto_ingrediente WHERE producto_id = ?")) {
            ps.setLong(1, productoId);
            ps.executeUpdate();
        }
    }

    private void actualizarStockIngrediente(int stock) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE ingredientes
                   SET stock_actual = ?
                 WHERE ingrediente_id = ?
                """)) {
            ps.setBigDecimal(1, BigDecimal.valueOf(stock));
            ps.setLong(2, ingredienteId);
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