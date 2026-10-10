package repository;

import ConexionBD.ConexionBD;
import entity.Cliente;
import entity.PlatoInsumo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.DisponibilidadService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-115. Tres consultas apuntaban a tablas que no existen: "cliente" en vez de
 * "clientes", "insumo" en vez de "ingredientes" y "producto_menu" en vez de
 * "productos". Estas pruebas corren las consultas corregidas contra la base real
 * para que el error no vuelva sin que nadie se entere.
 *
 * Son pruebas de integracion: necesitan PostgreSQL con el esquema creado por
 * scripts/setup.sh. Si no hay base, la clase entera se omite en lugar de fallar.
 */
@DisplayName("HU-115 - consultas contra las tablas correctas")
class ChoquesNombreTablaTest {

    private static final String DOCUMENTO = "HU115-900900900";

    private static long idProducto;
    private static long idIngrediente;

    @BeforeAll
    static void comprobarBase() {
        try (Connection cn = ConexionBD.getConnection();
             Statement st = cn.createStatement()) {

            ResultSet rs = st.executeQuery(
                    "SELECT producto_id FROM productos ORDER BY producto_id LIMIT 1");
            Assumptions.assumeTrue(rs.next(), "No hay productos en la base: se omite.");
            idProducto = rs.getLong(1);

            rs = st.executeQuery(
                    "SELECT ingrediente_id FROM ingredientes ORDER BY ingrediente_id LIMIT 1");
            Assumptions.assumeTrue(rs.next(), "No hay ingredientes en la base: se omite.");
            idIngrediente = rs.getLong(1);

            rs = st.executeQuery("SELECT 1 FROM tipos_documento WHERE codigo = 'CC'");
            Assumptions.assumeTrue(rs.next(), "Falta el tipo de documento CC: se omite.");

        } catch (SQLException e) {
            Assumptions.abort("No hay base de datos disponible: " + e.getMessage());
        }
    }

    @BeforeEach
    void limpiar() throws SQLException {
        try (Connection cn = ConexionBD.getConnection();
             Statement st = cn.createStatement()) {
            st.executeUpdate("DELETE FROM clientes WHERE numero_documento LIKE 'HU115-%'");
            st.executeUpdate("DELETE FROM plato_insumo WHERE plato_id = " + idProducto);
            st.executeUpdate("UPDATE productos SET estado = 'DISPONIBLE' "
                           + "WHERE producto_id = " + idProducto);
        }
    }

    @AfterAll
    static void borrarRastros() throws SQLException {
        try (Connection cn = ConexionBD.getConnection();
             Statement st = cn.createStatement()) {
            st.executeUpdate("DELETE FROM clientes WHERE numero_documento LIKE 'HU115-%'");
            st.executeUpdate("DELETE FROM plato_insumo WHERE plato_id = " + idProducto);
        } catch (SQLException ignorada) {
            // Si la base se cayo despues de las pruebas no hay nada que limpiar.
        }
    }

    @Test
    @DisplayName("guardar un cliente escribe en la tabla clientes")
    void guardarCliente() throws SQLException {
        new ClienteRepository().guardar(
                new Cliente("CC", DOCUMENTO, "Ana", "Perez",
                            "hu115.ana@prueba.com", "3001112233"));

        try (Connection cn = ConexionBD.getConnection();
             PreparedStatement st = cn.prepareStatement(
                     "SELECT nombre, apellido, telefono, id_tipo_documento "
                   + "FROM clientes WHERE numero_documento = ?")) {
            st.setString(1, DOCUMENTO);
            ResultSet rs = st.executeQuery();

            assertTrue(rs.next(), "El cliente no quedo guardado.");
            assertEquals("Ana", rs.getString("nombre"));
            assertEquals("Perez", rs.getString("apellido"));
            assertEquals("3001112233", rs.getString("telefono"));
            assertTrue(rs.getInt("id_tipo_documento") > 0,
                    "El tipo de documento no se resolvio por su codigo.");
        }
    }

    @Test
    @DisplayName("un tipo de documento inexistente avisa en vez de no guardar en silencio")
    void tipoDocumentoInexistente() {
        SQLException e = assertThrows(SQLException.class, () ->
                new ClienteRepository().guardar(
                        new Cliente("NO-EXISTE", DOCUMENTO, "Ana", "Perez",
                                    "hu115.ana@prueba.com", "3001112233")));

        assertTrue(e.getMessage().contains("no existe"),
                "El mensaje deberia decir que el tipo de documento no existe: " + e.getMessage());
    }

    @Test
    @DisplayName("con stock suficiente el producto queda DISPONIBLE")
    void hayStock() throws SQLException {
        darReceta(5);
        fijarStock(100);

        DisponibilidadService servicio = new DisponibilidadService();
        assertTrue(servicio.tieneInsumosDisponibles((int) idProducto));

        servicio.actualizarDisponibilidad((int) idProducto);
        assertEquals("DISPONIBLE", estadoProducto());
    }

    @Test
    @DisplayName("sin stock suficiente el producto queda AGOTADO")
    void noHayStock() throws SQLException {
        darReceta(50);
        fijarStock(1);

        DisponibilidadService servicio = new DisponibilidadService();
        assertFalse(servicio.tieneInsumosDisponibles((int) idProducto));

        servicio.actualizarDisponibilidad((int) idProducto);
        assertEquals("AGOTADO", estadoProducto());
    }

    @Test
    @DisplayName("un producto INACTIVO no se reactiva solo porque haya insumos")
    void noReactivaInactivo() throws SQLException {
        darReceta(5);
        fijarStock(100);

        try (Connection cn = ConexionBD.getConnection();
             Statement st = cn.createStatement()) {
            st.executeUpdate("UPDATE productos SET estado = 'INACTIVO' "
                           + "WHERE producto_id = " + idProducto);
        }

        new DisponibilidadService().actualizarDisponibilidad((int) idProducto);
        assertEquals("INACTIVO", estadoProducto());
    }

    // ------------------------------------------------------------- auxiliares --

    private void darReceta(int cantidad) throws SQLException {
        PlatoInsumo pi = new PlatoInsumo();
        pi.setPlatoId((int) idProducto);
        pi.setInsumoId((int) idIngrediente);
        pi.setCantidadNecesaria(cantidad);
        new PlatoInsumoRepository().guardar(pi);
    }

    private void fijarStock(int cantidad) throws SQLException {
        try (Connection cn = ConexionBD.getConnection();
             Statement st = cn.createStatement()) {
            st.executeUpdate("UPDATE ingredientes SET stock_actual = " + cantidad
                           + " WHERE ingrediente_id = " + idIngrediente);
        }
    }

    private String estadoProducto() throws SQLException {
        try (Connection cn = ConexionBD.getConnection();
             PreparedStatement st = cn.prepareStatement(
                     "SELECT estado FROM productos WHERE producto_id = ?")) {
            st.setLong(1, idProducto);
            ResultSet rs = st.executeQuery();
            rs.next();
            return rs.getString(1);
        }
    }
}
