package repository;

import ConexionBD.ConexionBD;
import entity.Mesa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-087 · Pruebas funcionales del módulo de mesas.
 *
 * Son pruebas de integración: trabajan contra una base PostgreSQL real, porque
 * buena parte de las reglas del módulo (capacidad frente a comensales, catálogo
 * de estados) vive en el SQL y no en código Java, y probarlas con objetos
 * simulados no demostraría nada.
 *
 * Si no hay base disponible la clase entera se omite en vez de fallar, para que
 * quien compile sin base no vea el build en rojo.
 *
 * Los datos son propios: la prueba crea su zona y sus mesas con números a partir
 * de 9000, y los borra al terminar. No toca las mesas del restaurante.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("HU-087 · Módulo de mesas")
class MesaModuloFuncionalTest {

    private static final String ZONA_PRUEBA = "ZONA PRUEBAS HU-087";
    private static final int NUMERO_BASE = 9000;

    private static final MesaRepository repositorio = new MesaRepository();

    private static int idZona;
    /** Mesa de capacidad 4, la que más se usa. */
    private static int idMesaCuatro;
    /** Mesa de capacidad 2, para los casos de borde. */
    private static int idMesaDos;

    // ------------------------------------------------------------------
    // Preparación
    // ------------------------------------------------------------------

    @BeforeAll
    static void prepararDatos() {
        try (Connection conn = ConexionBD.getConnection()) {
            asegurarEstados(conn);
            idZona = crearZonaDePrueba(conn);
            idMesaCuatro = crearMesa(conn, NUMERO_BASE + 1, 4);
            idMesaDos = crearMesa(conn, NUMERO_BASE + 2, 2);
        } catch (SQLException e) {
            Assumptions.abort("No hay base de datos disponible, se omiten las pruebas "
                    + "del modulo de mesas: " + e.getMessage());
        }
    }

    @AfterAll
    static void limpiarDatos() {
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("DELETE FROM mesas WHERE numero_mesa >= " + NUMERO_BASE);
            stmt.executeUpdate("DELETE FROM zonas WHERE nombre_zona = '" + ZONA_PRUEBA + "'");

        } catch (SQLException e) {
            System.err.println("No se pudieron borrar los datos de prueba: " + e.getMessage());
        }
    }

    /** Cada caso empieza con las dos mesas libres y sin comensales. */
    @BeforeEach
    void reiniciarMesas() throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(
                    "UPDATE mesas SET cantidad_comensales = NULL, capacidad = 4, " +
                    "    id_estado_mesa = (SELECT id_estado_mesa FROM estados_mesa " +
                    "                      WHERE UPPER(codigo_estado) = 'LIBRE') " +
                    "WHERE id_mesa = " + idMesaCuatro);

            stmt.executeUpdate(
                    "UPDATE mesas SET cantidad_comensales = NULL, capacidad = 2, " +
                    "    id_estado_mesa = (SELECT id_estado_mesa FROM estados_mesa " +
                    "                      WHERE UPPER(codigo_estado) = 'LIBRE') " +
                    "WHERE id_mesa = " + idMesaDos);
        }
    }

    // ------------------------------------------------------------------
    // CP-01 a CP-02 · Visualización
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("CP-01 · Las mesas activas se cargan y se listan")
    void cargaDeMesas() throws SQLException {
        List<Mesa> mesas = repositorio.obtenerTodas();

        assertFalse(mesas.isEmpty(), "obtenerTodas no devolvio ninguna mesa");
        assertTrue(mesas.stream().anyMatch(m -> m.getIdMesa() == idMesaCuatro),
                "la mesa de prueba no aparece en el listado");

        long repetidas = mesas.size() - mesas.stream().map(Mesa::getIdMesa).distinct().count();
        assertEquals(0, repetidas, "obtenerTodas devolvio mesas repetidas");
    }

    @Test
    @Order(2)
    @DisplayName("CP-02 · Cada mesa trae numero, capacidad y estado")
    void datosVisiblesDeLaMesa() throws SQLException {
        Mesa mesa = buscar(idMesaCuatro);

        assertAll(
                () -> assertEquals(NUMERO_BASE + 1, mesa.getNumeroMesa(), "numero de mesa"),
                () -> assertEquals(4, mesa.getCapacidad(), "capacidad"),
                () -> assertNotNull(mesa.getCodigoEstado(), "codigo de estado"),
                () -> assertEquals("LIBRE", mesa.getCodigoEstado().toUpperCase(), "estado inicial"),
                () -> assertNotNull(mesa.getNombreZona(), "nombre de la zona"));
    }

    // ------------------------------------------------------------------
    // CP-03 a CP-07 · Asignación, estados y liberación
    // ------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("CP-03 · Se asigna una mesa disponible")
    void asignarMesaDisponible() throws SQLException {
        boolean asignada = repositorio.asignarComensales(idMesaCuatro, 3);

        assertTrue(asignada, "no se pudo asignar una mesa que estaba libre");
        assertEquals(3, comensalesEnBaseDeDatos(idMesaCuatro), "comensales guardados");
    }

    @Test
    @Order(4)
    @DisplayName("CP-06 · El estado pasa de LIBRE a OCUPADA al asignar")
    void elEstadoPasaAOcupada() throws SQLException {
        assertEquals("LIBRE", buscar(idMesaCuatro).getCodigoEstado().toUpperCase(),
                "la mesa no empezo libre");

        repositorio.asignarComensales(idMesaCuatro, 2);

        assertEquals("OCUPADA", buscar(idMesaCuatro).getCodigoEstado().toUpperCase(),
                "la mesa no quedo ocupada despues de asignarla");
    }

    @Test
    @Order(5)
    @DisplayName("CP-05 y CP-07 · Al liberar vuelve a LIBRE y sin comensales")
    void liberarMesa() throws SQLException {
        repositorio.asignarComensales(idMesaCuatro, 4);
        assertEquals("OCUPADA", buscar(idMesaCuatro).getCodigoEstado().toUpperCase());

        repositorio.liberarMesa(idMesaCuatro);

        Mesa mesa = buscar(idMesaCuatro);
        assertAll(
                () -> assertEquals("LIBRE", mesa.getCodigoEstado().toUpperCase(), "estado tras liberar"),
                () -> assertNull(comensalesEnBaseDeDatos(idMesaCuatro), "los comensales debian quedar vacios"));
    }

    @Test
    @Order(6)
    @DisplayName("CP-04 · Una mesa ocupada no se puede volver a asignar")
    void mesaOcupadaNoSeReasigna() throws SQLException {
        repositorio.asignarComensales(idMesaCuatro, 2);
        assertEquals("OCUPADA", buscar(idMesaCuatro).getCodigoEstado().toUpperCase());

        boolean segunda = repositorio.asignarComensales(idMesaCuatro, 3);

        assertFalse(segunda,
                "la mesa ya estaba OCUPADA y aun asi acepto una segunda asignacion");
        assertEquals(2, comensalesEnBaseDeDatos(idMesaCuatro),
                "la segunda asignacion sobrescribio los comensales de la primera");
    }

    // ------------------------------------------------------------------
    // CP-08 a CP-11 · Capacidad y valores límite
    // ------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("CP-10 · Comensales iguales a la capacidad: se asigna")
    void comensalesIgualesALaCapacidad() throws SQLException {
        assertTrue(repositorio.asignarComensales(idMesaDos, 2),
                "con comensales iguales a la capacidad la asignacion debia pasar");
        assertEquals(2, comensalesEnBaseDeDatos(idMesaDos));
    }

    @Test
    @Order(8)
    @DisplayName("CP-09 · Comensales por encima de la capacidad: no se asigna")
    void comensalesPorEncimaDeLaCapacidad() throws SQLException {
        boolean asignada = repositorio.asignarComensales(idMesaDos, 3);

        assertFalse(asignada, "se asignaron mas comensales que la capacidad de la mesa");
        assertNull(comensalesEnBaseDeDatos(idMesaDos), "no debio guardarse ninguna cantidad");
        assertEquals("LIBRE", buscar(idMesaDos).getCodigoEstado().toUpperCase(),
                "la mesa no debio quedar ocupada");
    }

    @Test
    @Order(9)
    @DisplayName("CP-11a · Cero comensales: se rechaza")
    void ceroComensales() {
        assertThrows(IllegalArgumentException.class,
                () -> repositorio.asignarComensales(idMesaCuatro, 0),
                "cero comensales debia rechazarse");
    }

    @Test
    @Order(10)
    @DisplayName("CP-11b · Comensales negativos: se rechaza")
    void comensalesNegativos() {
        assertThrows(IllegalArgumentException.class,
                () -> repositorio.asignarComensales(idMesaCuatro, -1),
                "una cantidad negativa debia rechazarse");
    }

    @Test
    @Order(11)
    @DisplayName("CP-11c · Capacidad mas uno: no se asigna")
    void capacidadMasUno() throws SQLException {
        assertFalse(repositorio.asignarComensales(idMesaCuatro, 5),
                "cinco comensales en una mesa de cuatro no debia pasar");
        assertNull(comensalesEnBaseDeDatos(idMesaCuatro));
    }

    @Test
    @Order(12)
    @DisplayName("CP-14 · No se puede bajar la capacidad por debajo de los comensales")
    void bajarCapacidadPorDebajoDeLosComensales() throws SQLException {
        repositorio.asignarComensales(idMesaCuatro, 4);

        assertThrows(SQLException.class,
                () -> repositorio.cambiarCapacidadMesa(idMesaCuatro, 2),
                "dejar la capacidad por debajo de los comensales debia impedirse");

        assertEquals(4, buscar(idMesaCuatro).getCapacidad(),
                "la capacidad no debio cambiar");
    }

    // ------------------------------------------------------------------
    // CP-12 a CP-13 · Consistencia
    // ------------------------------------------------------------------

    @Test
    @Order(13)
    @DisplayName("CP-12 · Lo que muestra el repositorio coincide con la base")
    void consistenciaConLaBaseDeDatos() throws SQLException {
        repositorio.asignarComensales(idMesaCuatro, 3);

        Mesa mesa = buscar(idMesaCuatro);

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT m.numero_mesa, m.capacidad, m.cantidad_comensales, e.codigo_estado " +
                     "FROM mesas m JOIN estados_mesa e ON e.id_estado_mesa = m.id_estado_mesa " +
                     "WHERE m.id_mesa = ?")) {

            stmt.setInt(1, idMesaCuatro);

            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "la mesa no esta en la base");

                assertAll(
                        () -> assertEquals(rs.getInt("numero_mesa"), mesa.getNumeroMesa(), "numero"),
                        () -> assertEquals(rs.getInt("capacidad"), mesa.getCapacidad(), "capacidad"),
                        () -> assertEquals(rs.getString("codigo_estado"), mesa.getCodigoEstado(), "estado"),
                        () -> assertEquals(3, rs.getInt("cantidad_comensales"), "comensales"));
            }
        }
    }

    @Test
    @Order(14)
    @DisplayName("CP-13 · Varias asignaciones y liberaciones seguidas dejan la mesa consistente")
    void ciclosConsecutivos() throws SQLException {
        for (int vuelta = 1; vuelta <= 5; vuelta++) {
            int comensales = (vuelta % 4) + 1;

            assertTrue(repositorio.asignarComensales(idMesaCuatro, comensales),
                    "fallo la asignacion en la vuelta " + vuelta);
            assertEquals("OCUPADA", buscar(idMesaCuatro).getCodigoEstado().toUpperCase(),
                    "estado incorrecto tras asignar en la vuelta " + vuelta);
            assertEquals(comensales, comensalesEnBaseDeDatos(idMesaCuatro),
                    "comensales incorrectos en la vuelta " + vuelta);

            repositorio.liberarMesa(idMesaCuatro);
            assertEquals("LIBRE", buscar(idMesaCuatro).getCodigoEstado().toUpperCase(),
                    "estado incorrecto tras liberar en la vuelta " + vuelta);
            assertNull(comensalesEnBaseDeDatos(idMesaCuatro),
                    "quedaron comensales tras liberar en la vuelta " + vuelta);
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static Mesa buscar(int idMesa) throws SQLException {
        Optional<Mesa> mesa = repositorio.findById(idMesa);
        assertTrue(mesa.isPresent(), "no se encontro la mesa " + idMesa);
        return mesa.get();
    }

    private static Integer comensalesEnBaseDeDatos(int idMesa) throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT cantidad_comensales FROM mesas WHERE id_mesa = ?")) {

            stmt.setInt(1, idMesa);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int valor = rs.getInt("cantidad_comensales");
                return rs.wasNull() ? null : valor;
            }
        }
    }

    private static void asegurarEstados(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO estados_mesa (codigo_estado, descripcion) " +
                "SELECT ?, ? WHERE NOT EXISTS " +
                "  (SELECT 1 FROM estados_mesa WHERE UPPER(codigo_estado) = ?)")) {

            for (String[] estado : new String[][]{
                    {"LIBRE", "Libre"}, {"OCUPADA", "Ocupada"}, {"RESERVADA", "Reservada"}}) {
                stmt.setString(1, estado[0]);
                stmt.setString(2, estado[1]);
                stmt.setString(3, estado[0]);
                stmt.executeUpdate();
            }
        }
    }

    private static int crearZonaDePrueba(Connection conn) throws SQLException {
        try (PreparedStatement buscar = conn.prepareStatement(
                "SELECT id_zona FROM zonas WHERE nombre_zona = ?")) {
            buscar.setString(1, ZONA_PRUEBA);
            try (ResultSet rs = buscar.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_zona");
                }
            }
        }

        try (PreparedStatement insertar = conn.prepareStatement(
                "INSERT INTO zonas (nombre_zona) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            insertar.setString(1, ZONA_PRUEBA);
            insertar.executeUpdate();

            try (ResultSet llaves = insertar.getGeneratedKeys()) {
                assertTrue(llaves.next(), "no se pudo crear la zona de prueba");
                return llaves.getInt(1);
            }
        }
    }

    private static int crearMesa(Connection conn, int numeroMesa, int capacidad) throws SQLException {
        try (PreparedStatement borrar = conn.prepareStatement(
                "DELETE FROM mesas WHERE numero_mesa = ?")) {
            borrar.setInt(1, numeroMesa);
            borrar.executeUpdate();
        }

        try (PreparedStatement insertar = conn.prepareStatement(
                "INSERT INTO mesas (numero_mesa, codigo_mesa, capacidad, id_zona, " +
                "                   id_estado_mesa, is_active) " +
                "VALUES (?, ?, ?, ?, (SELECT id_estado_mesa FROM estados_mesa " +
                "                     WHERE UPPER(codigo_estado) = 'LIBRE'), 1)",
                Statement.RETURN_GENERATED_KEYS)) {

            insertar.setInt(1, numeroMesa);
            insertar.setString(2, String.format("P-%04d", numeroMesa));
            insertar.setInt(3, capacidad);
            insertar.setInt(4, idZona);
            insertar.executeUpdate();

            try (ResultSet llaves = insertar.getGeneratedKeys()) {
                assertTrue(llaves.next(), "no se pudo crear la mesa de prueba " + numeroMesa);
                return llaves.getInt(1);
            }
        }
    }
}
