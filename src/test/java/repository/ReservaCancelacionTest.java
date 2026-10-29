package repository;

import ConexionBD.ConexionBD;
import entity.Reserva;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.ReservaService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cancelación de reservas")
class ReservaCancelacionTest {

    private static final String ZONA_PRUEBA = "ZONA PRUEBAS HU-164";
    private static final int NUMERO_MESA = 9855;

    private static int idZona;
    private static int idMesa;
    private static int idReserva;

    private final ReservaService reservaService = new ReservaService();

    @BeforeAll
    static void prepararDatos() throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);

            try {
                asegurarEstado(conn, "DISPONIBLE");
                asegurarEstado(conn, "RESERVADA");
                idZona = crearZona(conn);
                idMesa = crearMesa(conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    @BeforeEach
    void prepararReserva() throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);

            try {
                try (PreparedStatement borrar = conn.prepareStatement(
                        "DELETE FROM reservas WHERE id_mesa = ?")) {
                    borrar.setInt(1, idMesa);
                    borrar.executeUpdate();
                }

                establecerEstadoMesa(conn, "RESERVADA");

                try (PreparedStatement insertar = conn.prepareStatement(
                        "INSERT INTO reservas " +
                                "(id_mesa, fecha_hora_reserva, nombre_cliente, " +
                                "cantidad_personas, is_active, fecha_cancelacion) " +
                                "VALUES (?, ?, ?, ?, 1, NULL) RETURNING id_reserva")) {

                    insertar.setInt(1, idMesa);
                    insertar.setTimestamp(
                            2, java.sql.Timestamp.valueOf(LocalDateTime.now().plusHours(2)));
                    insertar.setString(3, "Cliente prueba HU-164");
                    insertar.setInt(4, 2);

                    try (ResultSet rs = insertar.executeQuery()) {
                        assertTrue(rs.next(), "No se creó la reserva de prueba");
                        idReserva = rs.getInt("id_reserva");
                    }
                }

                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    @AfterAll
    static void limpiarDatos() {
        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Primero se eliminan los registros que dependen de la mesa.
                try (PreparedStatement borrarHistorial = conn.prepareStatement(
                        "DELETE FROM historial_estado_mesa WHERE mesa_id = ?")) {
                    borrarHistorial.setInt(1, idMesa);
                    borrarHistorial.executeUpdate();
                }

                try (PreparedStatement borrarReservas = conn.prepareStatement(
                        "DELETE FROM reservas WHERE id_mesa = ?")) {
                    borrarReservas.setInt(1, idMesa);
                    borrarReservas.executeUpdate();
                }

                try (PreparedStatement borrarMesa = conn.prepareStatement(
                        "DELETE FROM mesas WHERE id_mesa = ?")) {
                    borrarMesa.setInt(1, idMesa);
                    borrarMesa.executeUpdate();
                }

                try (PreparedStatement borrarZona = conn.prepareStatement(
                        "DELETE FROM zonas WHERE id_zona = ?")) {
                    borrarZona.setInt(1, idZona);
                    borrarZona.executeUpdate();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                System.err.println(
                        "No se pudieron limpiar los datos HU-164: " + e.getMessage()
                );
            }
        } catch (SQLException e) {
            System.err.println(
                    "No se pudo conectar para limpiar HU-164: " + e.getMessage()
            );
        }
    }

    @Test
    @DisplayName("CP-164-01 - Cancela la reserva y deja la mesa DISPONIBLE")
    void cancelarReservaLiberaMesa() throws SQLException {
        Reserva resultado = reservaService.cancelarReserva(idReserva);

        assertNotNull(resultado);
        assertFalse(resultado.isActiva(), "La reserva debería quedar inactiva");
        assertNotNull(resultado.getFechaCancelacion(),
                "Debe registrarse la fecha de cancelación");

        try (Connection conn = ConexionBD.getConnection()) {
            assertAll(
                    () -> assertEquals(0, consultarReservaActiva(conn),
                            "La reserva debe quedar inactiva en la base"),
                    () -> assertNotNull(consultarFechaCancelacion(conn),
                            "La fecha de cancelación debe persistirse"),
                    () -> assertEquals("DISPONIBLE", consultarEstadoMesa(conn),
                            "La mesa debe quedar DISPONIBLE"));
        }
    }


    @Test
    @DisplayName("CP-164-02 - Si falla el historial, se revierte la cancelación")
    void errorAlLiberarMesaRevierteCancelacion() throws SQLException {
        crearTriggerFalloHistorial();

        try {
            assertThrows(
                    SQLException.class,
                    () -> reservaService.cancelarReserva(idReserva),
                    "La cancelación debe fallar si no puede registrar el historial"
            );

            try (Connection conn = ConexionBD.getConnection()) {
                assertAll(
                        () -> assertEquals(
                                1,
                                consultarReservaActiva(conn),
                                "La reserva debe seguir activa"
                        ),
                        () -> assertNull(
                                consultarFechaCancelacion(conn),
                                "La fecha de cancelación debe seguir siendo NULL"
                        ),
                        () -> assertEquals(
                                "RESERVADA",
                                consultarEstadoMesa(conn),
                                "La mesa debe conservar su estado anterior"
                        )
                );
            }
        } finally {
            eliminarTriggerFalloHistorial();
        }
    }


    private static void asegurarEstado(Connection conn, String codigo)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO estados_mesa (codigo_estado, descripcion) " +
                        "SELECT ?, ? WHERE NOT EXISTS " +
                        "(SELECT 1 FROM estados_mesa WHERE UPPER(codigo_estado) = UPPER(?))")) {
            stmt.setString(1, codigo);
            stmt.setString(2, codigo);
            stmt.setString(3, codigo);
            stmt.executeUpdate();
        }
    }

    private static int crearZona(Connection conn) throws SQLException {
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
                "INSERT INTO zonas (nombre_zona) VALUES (?) RETURNING id_zona")) {
            insertar.setString(1, ZONA_PRUEBA);

            try (ResultSet rs = insertar.executeQuery()) {
                assertTrue(rs.next(), "No se pudo crear la zona de prueba");
                return rs.getInt("id_zona");
            }
        }
    }
    private static int crearMesa(Connection conn) throws SQLException {
        // Reutilizar la mesa si ya existe para esta zona.
        try (PreparedStatement buscar = conn.prepareStatement(
                "SELECT id_mesa FROM mesas " +
                        "WHERE numero_mesa = ? AND id_zona = ?")) {
            buscar.setInt(1, NUMERO_MESA);
            buscar.setInt(2, idZona);

            try (ResultSet rs = buscar.executeQuery()) {
                if (rs.next()) {
                    int mesaExistente = rs.getInt("id_mesa");

                    // Limpiar reservas anteriores de esa mesa.
                    try (PreparedStatement borrar = conn.prepareStatement(
                            "DELETE FROM reservas WHERE id_mesa = ?")) {
                        borrar.setInt(1, mesaExistente);
                        borrar.executeUpdate();
                    }

                    return mesaExistente;
                }
            }
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO mesas (numero_mesa, codigo_mesa, capacidad, id_zona, " +
                        "id_estado_mesa, is_active) " +
                        "VALUES (?, ?, 4, ?, " +
                        "(SELECT id_estado_mesa FROM estados_mesa " +
                        "WHERE UPPER(codigo_estado) = 'DISPONIBLE'), 1) " +
                        "RETURNING id_mesa")) {

            stmt.setInt(1, NUMERO_MESA);
            stmt.setString(2, "P-9855");
            stmt.setInt(3, idZona);

            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "No se pudo crear la mesa de prueba");
                return rs.getInt("id_mesa");
            }
        }
    }

    private static void establecerEstadoMesa(Connection conn, String codigo)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE mesas SET id_estado_mesa = (" +
                        "SELECT id_estado_mesa FROM estados_mesa " +
                        "WHERE UPPER(codigo_estado) = UPPER(?)) WHERE id_mesa = ?")) {
            stmt.setString(1, codigo);
            stmt.setInt(2, idMesa);
            stmt.executeUpdate();
        }
    }

    private static int consultarReservaActiva(Connection conn)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT is_active FROM reservas WHERE id_reserva = ?")) {
            stmt.setInt(1, idReserva);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }

    private static java.sql.Timestamp consultarFechaCancelacion(Connection conn)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT fecha_cancelacion FROM reservas WHERE id_reserva = ?")) {
            stmt.setInt(1, idReserva);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getTimestamp(1);
            }
        }
    }

    private static String consultarEstadoMesa(Connection conn)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT e.codigo_estado FROM mesas m " +
                        "JOIN estados_mesa e ON e.id_estado_mesa = m.id_estado_mesa " +
                        "WHERE m.id_mesa = ?")) {
            stmt.setInt(1, idMesa);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getString(1);
            }
        }
    }

    private boolean consultarReservaActivaActual() throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            return consultarReservaActiva(conn) == 1;
        }
    }

    private java.sql.Timestamp consultarFechaCancelacionActual()
            throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            return consultarFechaCancelacion(conn);
        }
    }

    private static void crearTriggerFalloHistorial() throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE OR REPLACE FUNCTION fallo_historial_prueba()
                RETURNS trigger AS $$
                BEGIN
                    RAISE EXCEPTION 'Fallo intencional de prueba HU-055';
                END;
                $$ LANGUAGE plpgsql
                """);

            stmt.execute("""
                CREATE TRIGGER trigger_fallo_historial_prueba
                BEFORE INSERT ON historial_estado_mesa
                FOR EACH ROW
                EXECUTE FUNCTION fallo_historial_prueba()
                """);
        }
    }

    private static void eliminarTriggerFalloHistorial() throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {

            stmt.execute("""
                DROP TRIGGER IF EXISTS trigger_fallo_historial_prueba
                ON historial_estado_mesa
                """);

            stmt.execute("""
                DROP FUNCTION IF EXISTS fallo_historial_prueba()
                """);
        }
    }

}