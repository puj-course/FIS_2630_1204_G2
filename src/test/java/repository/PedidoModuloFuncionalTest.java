package repository;

import ConexionDB.ConexionBD;
import entity.EstadoPedido;
import entity.Mesa;
import entity.Pedido;
import exceptions.MesaNotFoundException;
import exceptions.MesaOcupadaException;
import exceptions.PedidoNoEditableException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import service.PedidoService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-XXX · Pruebas funcionales del módulo de cocina (pedidos).
 *
 * Cubre el registro (crearPedido), la consulta (findById) y la actualización
 * (cerrarPedido y save) de los pedidos. Son pruebas de integración contra una
 * base PostgreSQL real: parte de las reglas vive en el SQL (tipo JSONB de
 * "productos", llaves foráneas, catálogo de estados de mesa).
 *
 * Si no hay base disponible la clase entera se omite en vez de fallar.
 *
 * Los datos son propios: se crea una zona y dos mesas con números 8101 y 8102,
 * y al terminar se borran junto con sus pedidos e historial. Se reutiliza un
 * usuario que ya exista en la base, porque pedidos.usuario_id es obligatorio.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("HU-XXX · Módulo de cocina: pedidos")
class PedidoModuloFuncionalTest {

    private static final String ZONA_PRUEBA = "ZONA PRUEBAS HU-XXX";
    private static final int NUMERO_BASE = 8100;

    private static final PedidoRepository repositorio = new PedidoRepository();
    private static final MesaRepository mesas = new MesaRepository();
    private static final PedidoService servicio = new PedidoService();

    private static int idZona;
    private static int idMesaUno;
    private static int idMesaDos;
    private static long idUsuario;

    // ------------------------------------------------------------------
    // Preparación
    // ------------------------------------------------------------------

    @BeforeAll
    static void prepararDatos() {
        try (Connection conn = ConexionBD.getConnection()) {
            asegurarEstados(conn);
            idUsuario = buscarUsuario(conn);
            idZona = crearZonaDePrueba(conn);
            idMesaUno = crearMesa(conn, NUMERO_BASE + 1);
            idMesaDos = crearMesa(conn, NUMERO_BASE + 2);
        } catch (SQLException e) {
            Assumptions.abort("No hay base de datos disponible, se omiten las pruebas "
                    + "del modulo de pedidos: " + e.getMessage());
        }
    }

    @AfterAll
    static void limpiarDatos() {
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement()) {

            String ids = idMesaUno + "," + idMesaDos;
            stmt.executeUpdate("DELETE FROM pedidos WHERE mesa_id IN (" + ids + ")");
            stmt.executeUpdate("DELETE FROM historial_estado_mesa WHERE mesa_id IN (" + ids + ")");
            stmt.executeUpdate("DELETE FROM mesas WHERE id_mesa IN (" + ids + ")");
            stmt.executeUpdate("DELETE FROM zonas WHERE nombre_zona = '" + ZONA_PRUEBA + "'");

        } catch (SQLException e) {
            System.err.println("No se pudieron borrar los datos de prueba: " + e.getMessage());
        }
    }

    /** Cada caso empieza con las dos mesas libres y sin pedidos ni historial. */
    @BeforeEach
    void reiniciar() throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement()) {

            String ids = idMesaUno + "," + idMesaDos;
            stmt.executeUpdate("DELETE FROM pedidos WHERE mesa_id IN (" + ids + ")");
            stmt.executeUpdate("DELETE FROM historial_estado_mesa WHERE mesa_id IN (" + ids + ")");
            stmt.executeUpdate(
                    "UPDATE mesas SET id_estado_mesa = (SELECT id_estado_mesa FROM estados_mesa " +
                    "                                   WHERE UPPER(codigo_estado) = 'LIBRE') " +
                    "WHERE id_mesa IN (" + ids + ")");
        }
    }

    // ------------------------------------------------------------------
    // CP-01 a CP-03 · Registro
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("CP-01 · Se registra un pedido PENDIENTE para una mesa libre")
    void registrarPedido() throws SQLException {
        Pedido pedido = servicio.crearPedido(idMesaUno, idUsuario);

        assertNotNull(pedido.getId(), "el pedido no recibio id de la base");
        assertAll(
                () -> assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado(), "estado"),
                () -> assertTrue(pedido.getNumeroPedido().startsWith("PED-"), "numero de pedido"),
                () -> assertEquals("PENDIENTE", estadoEnBaseDeDatos(pedido.getId()), "estado en la base"));
    }

    @Test
    @Order(2)
    @DisplayName("CP-02 · Al registrar el pedido la mesa pasa a OCUPADA")
    void laMesaQuedaOcupada() throws SQLException {
        assertEquals("LIBRE", estadoDeMesa(idMesaUno), "la mesa no empezo libre");

        servicio.crearPedido(idMesaUno, idUsuario);

        assertEquals("OCUPADA", estadoDeMesa(idMesaUno), "la mesa no quedo ocupada");
    }

    @Test
    @Order(3)
    @DisplayName("CP-03 · El cambio de estado de la mesa queda en el historial")
    void quedaHistorial() throws SQLException {
        servicio.crearPedido(idMesaUno, idUsuario);

        assertEquals(1, filasDeHistorial(idMesaUno, "APERTURA_PEDIDO"),
                "debia haber un registro APERTURA_PEDIDO en el historial");
    }

    // ------------------------------------------------------------------
    // CP-04 a CP-05 · Consulta
    // ------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("CP-04 · Lo que devuelve findById coincide con la base")
    void consultaPorId() throws SQLException {
        Pedido creado = servicio.crearPedido(idMesaUno, idUsuario);

        Optional<Pedido> consultado = repositorio.findById(creado.getId());
        assertTrue(consultado.isPresent(), "findById no encontro el pedido recien creado");
        Pedido pedido = consultado.get();

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT numero_pedido, mesa_id, usuario_id, estado FROM pedidos WHERE pedido_id = ?")) {
            stmt.setLong(1, creado.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "el pedido no esta en la base");
                assertAll(
                        () -> assertEquals(rs.getString("numero_pedido"), pedido.getNumeroPedido(), "numero"),
                        () -> assertEquals(rs.getLong("mesa_id"), pedido.getMesaId(), "mesa"),
                        () -> assertEquals(rs.getLong("usuario_id"), pedido.getUsuarioId(), "usuario"),
                        () -> assertEquals(rs.getString("estado"), pedido.getEstado().name(), "estado"));
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("CP-05 · Consultar un pedido que no existe devuelve vacio")
    void consultaDePedidoInexistente() throws SQLException {
        assertFalse(repositorio.findById(-1L).isPresent(),
                "findById devolvio un pedido con un id que no existe");
    }

    // ------------------------------------------------------------------
    // CP-06 · Regla de comanda activa
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("CP-06 · Una mesa con comanda activa rechaza otro pedido")
    void mesaConComandaActiva() throws SQLException {
        servicio.crearPedido(idMesaUno, idUsuario);

        assertThrows(MesaOcupadaException.class,
                () -> servicio.crearPedido(idMesaUno, idUsuario),
                "la mesa ya tenia una comanda activa y acepto otra");

        assertEquals(1, pedidosDeMesa(idMesaUno), "no debia crearse un segundo pedido");
    }

    // ------------------------------------------------------------------
    // CP-07 a CP-11 · Actualización
    // ------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("CP-07 · Al cerrar el pedido queda COMPLETADO")
    void cerrarPedido() throws SQLException {
        Pedido pedido = servicio.crearPedido(idMesaUno, idUsuario);

        Pedido cerrado = servicio.cerrarPedido(pedido.getId(), null);

        assertEquals(EstadoPedido.COMPLETADO, cerrado.getEstado(), "estado devuelto");
        assertEquals("COMPLETADO", estadoEnBaseDeDatos(pedido.getId()), "estado en la base");
    }

    @Test
    @Order(8)
    @DisplayName("CP-08 · Al cerrar el pedido la mesa vuelve a LIBRE")
    void cerrarLiberaLaMesa() throws SQLException {
        Pedido pedido = servicio.crearPedido(idMesaUno, idUsuario);
        assertEquals("OCUPADA", estadoDeMesa(idMesaUno));

        servicio.cerrarPedido(pedido.getId(), null);

        assertEquals("LIBRE", estadoDeMesa(idMesaUno), "la mesa no se libero");
        assertEquals(1, filasDeHistorial(idMesaUno, "CIERRE_PEDIDO"),
                "debia haber un registro CIERRE_PEDIDO en el historial");
    }

    @Test
    @Order(9)
    @DisplayName("CP-09 · Un pedido ya cerrado no se puede cerrar otra vez")
    void cerrarDosVeces() throws SQLException {
        Pedido pedido = servicio.crearPedido(idMesaUno, idUsuario);
        servicio.cerrarPedido(pedido.getId(), null);

        assertThrows(PedidoNoEditableException.class,
                () -> servicio.cerrarPedido(pedido.getId(), null),
                "un pedido COMPLETADO se dejo cerrar de nuevo");

        assertEquals("COMPLETADO", estadoEnBaseDeDatos(pedido.getId()));
    }

    @Test
    @Order(10)
    @DisplayName("CP-10 · Cerrar un pedido que no existe lanza error")
    void cerrarPedidoInexistente() {
        assertThrows(MesaNotFoundException.class,
                () -> servicio.cerrarPedido(-1L, null),
                "cerrar un pedido inexistente debia lanzar error");
    }

    @Test
    @Order(11)
    @DisplayName("CP-11 · El pedido pasa a EN_PREPARACION y guarda sus productos")
    void actualizarPedido() throws SQLException {
        Pedido pedido = servicio.crearPedido(idMesaUno, idUsuario);

        String lineas = "[{\"producto_id\":1,\"cantidad\":2}]";
        pedido.setEstado(EstadoPedido.EN_PREPARACION);
        pedido.setProductos(lineas);
        repositorio.save(pedido);

        Pedido leido = repositorio.findById(pedido.getId()).orElseThrow();
        assertAll(
                () -> assertEquals(EstadoPedido.EN_PREPARACION, leido.getEstado(), "estado"),
                // JSONB reordena llaves y espacios, por eso se compara en SQL y no como texto.
                () -> assertTrue(productosIguales(pedido.getId(), lineas), "productos"));
    }

    // ------------------------------------------------------------------
    // CP-12 a CP-13 · Reglas de la mesa a lo largo del ciclo
    // ------------------------------------------------------------------

    @Test
    @Order(12)
    @DisplayName("CP-12 · Un pedido CANCELADO no bloquea la mesa")
    void pedidoCanceladoNoBloquea() throws SQLException {
        Pedido cancelado = new Pedido();
        cancelado.setNumeroPedido("PED-CANC-" + System.nanoTime());
        cancelado.setMesaId((long) idMesaUno);
        cancelado.setUsuarioId(idUsuario);
        cancelado.setEstado(EstadoPedido.CANCELADO);
        repositorio.save(cancelado);

        Pedido nuevo = servicio.crearPedido(idMesaUno, idUsuario);

        assertNotNull(nuevo.getId(), "la mesa con un pedido cancelado debia aceptar uno nuevo");
    }

    @Test
    @Order(13)
    @DisplayName("CP-13 · Tras cerrar un pedido la mesa acepta uno nuevo")
    void cicloCompletoEnLaMismaMesa() throws SQLException {
        Pedido primero = servicio.crearPedido(idMesaUno, idUsuario);
        servicio.cerrarPedido(primero.getId(), null);

        Pedido segundo = servicio.crearPedido(idMesaUno, idUsuario);

        assertNotNull(segundo.getId(),
                "la mesa quedo LIBRE pero no acepto un pedido nuevo");
        assertEquals("OCUPADA", estadoDeMesa(idMesaUno));
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static String estadoEnBaseDeDatos(long idPedido) throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT estado FROM pedidos WHERE pedido_id = ?")) {
            stmt.setLong(1, idPedido);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static boolean productosIguales(long idPedido, String json) throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT productos = ?::jsonb FROM pedidos WHERE pedido_id = ?")) {
            stmt.setString(1, json);
            stmt.setLong(2, idPedido);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private static String estadoDeMesa(int idMesa) throws SQLException {
        Optional<Mesa> mesa = mesas.findById(idMesa);
        assertTrue(mesa.isPresent(), "no se encontro la mesa " + idMesa);
        return mesa.get().getCodigoEstado().toUpperCase();
    }

    private static int pedidosDeMesa(int idMesa) throws SQLException {
        return contar("SELECT count(*) FROM pedidos WHERE mesa_id = ?", idMesa, null);
    }

    private static int filasDeHistorial(int idMesa, String motivo) throws SQLException {
        return contar("SELECT count(*) FROM historial_estado_mesa WHERE mesa_id = ? AND motivo = ?",
                idMesa, motivo);
    }

    private static int contar(String sql, int idMesa, String motivo) throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMesa);
            if (motivo != null) {
                stmt.setString(2, motivo);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static long buscarUsuario(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id_usuario FROM usuarios ORDER BY id_usuario LIMIT 1")) {
            if (!rs.next()) {
                Assumptions.abort("No hay ningun usuario en la base, se omiten las pruebas de pedidos.");
            }
            return rs.getLong(1);
        }
    }

    private static void asegurarEstados(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO estados_mesa (codigo_estado, descripcion) " +
                "SELECT ?, ? WHERE NOT EXISTS " +
                "  (SELECT 1 FROM estados_mesa WHERE UPPER(codigo_estado) = ?)")) {

            for (String[] estado : new String[][]{{"LIBRE", "Libre"}, {"OCUPADA", "Ocupada"}}) {
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

    private static int crearMesa(Connection conn, int numeroMesa) throws SQLException {
        try (PreparedStatement borrar = conn.prepareStatement(
                "DELETE FROM mesas WHERE numero_mesa = ?")) {
            borrar.setInt(1, numeroMesa);
            borrar.executeUpdate();
        }

        try (PreparedStatement insertar = conn.prepareStatement(
                "INSERT INTO mesas (numero_mesa, codigo_mesa, capacidad, id_zona, " +
                "                   id_estado_mesa, is_active) " +
                "VALUES (?, ?, 4, ?, (SELECT id_estado_mesa FROM estados_mesa " +
                "                     WHERE UPPER(codigo_estado) = 'LIBRE'), 1)",
                Statement.RETURN_GENERATED_KEYS)) {

            insertar.setInt(1, numeroMesa);
            insertar.setString(2, String.format("P-%04d", numeroMesa));
            insertar.setInt(3, idZona);
            insertar.executeUpdate();

            try (ResultSet llaves = insertar.getGeneratedKeys()) {
                assertTrue(llaves.next(), "no se pudo crear la mesa de prueba " + numeroMesa);
                return llaves.getInt(1);
            }
        }
    }
}
