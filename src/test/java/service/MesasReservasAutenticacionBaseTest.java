package service;

import dao.ProductoDAO;
import entity.EstadoMesa;
import entity.Mesa;
import entity.Producto;
import entity.Reserva;
import entity.Zona;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repository.MesaRepository;
import repository.ReservaRepository;
import support.BaseDePrueba;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mesas, reservas, inicio de sesion y el menu, contra la base de datos de
 * verdad. Es lo primero que toca cualquiera que abre la aplicacion.
 */
class MesasReservasAutenticacionBaseTest {

    private static Connection conn;

    private final MesaService mesas = new MesaService();
    private final MesaRepository mesaRepo = new MesaRepository();
    private final ReservaRepository reservas = new ReservaRepository();
    private final AutenticacionService autenticacion = new AutenticacionService();
    private final ProductoDAO productos = new ProductoDAO();

    @BeforeAll
    static void abrirConexion() {
        conn = BaseDePrueba.conectarOOmitir();
    }

    @AfterAll
    static void cerrarConexion() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    /**
     * Cierra la sesion despues de cada prueba.
     *
     * SesionUsuario es un Singleton: un inicio de sesion correcto deja al
     * usuario abierto para toda la maquina virtual, y Maven corre todas las
     * clases de prueba en la misma. Sin esto, las pruebas de login de aqui
     * dejaban la sesion abierta y hacian fallar a AutenticacionServiceTest,
     * que empieza comprobando que no haya ninguna.
     */
    @AfterEach
    void cerrarLaSesionAbierta() {
        session.SesionUsuario.getInstance().cerrarSesion();
    }

    private int siguienteNumeroDeMesa() throws SQLException {
        return (int) BaseDePrueba.contarFilas(conn,
                "SELECT coalesce(max(numero_mesa), 0) + 1 FROM mesas");
    }

    // -------------------------------------------------------------- mesas --

    @Test
    @DisplayName("registrar una mesa la deja en el salon")
    void registrarDejaLaMesaEnElSalon() throws SQLException {
        int numero = siguienteNumeroDeMesa();

        mesas.registrarMesa(numero, 4);

        assertTrue(mesas.listarMesas().stream().anyMatch(m -> m.getNumeroMesa() == numero));
    }

    @Test
    @DisplayName("la mesa registrada nace con la capacidad que se le puso")
    void laMesaNaceConSuCapacidad() throws SQLException {
        int numero = siguienteNumeroDeMesa();
        mesas.registrarMesa(numero, 6);

        Mesa m = mesas.listarMesas().stream()
                .filter(x -> x.getNumeroMesa() == numero).findFirst().orElseThrow();

        assertEquals(6, m.getCapacidad());
    }

    @Test
    @DisplayName("cambiar la capacidad la deja actualizada")
    void cambiarLaCapacidad() throws SQLException {
        int numero = siguienteNumeroDeMesa();
        mesas.registrarMesa(numero, 4);
        Mesa m = mesas.listarMesas().stream()
                .filter(x -> x.getNumeroMesa() == numero).findFirst().orElseThrow();

        mesas.cambiarCapacidad(m.getIdMesa(), 8);

        assertEquals(8, mesaRepo.findById(m.getIdMesa()).orElseThrow().getCapacidad());
    }

    @Test
    @DisplayName("no se pueden registrar dos mesas con el mismo numero")
    void noSeRepiteElNumeroDeMesa() throws SQLException {
        int numero = siguienteNumeroDeMesa();
        mesas.registrarMesa(numero, 4);

        assertThrows(SQLException.class, () -> mesas.registrarMesa(numero, 4));
    }

    @Test
    @DisplayName("cambiar el estado de la mesa queda guardado")
    void cambiarElEstadoDeLaMesa() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        mesas.cambiarEstado(mesa, "OCUPADA", "APERTURA_PEDIDO");

        assertEquals("OCUPADA", mesaRepo.findById(mesa).orElseThrow().getCodigoEstado());
    }

    @Test
    @DisplayName("el cambio de estado queda en el historial")
    void elCambioQuedaEnElHistorial() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        mesas.cambiarEstado(mesa, "OCUPADA", "APERTURA_PEDIDO");

        assertTrue(BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM historial_estado_mesa WHERE mesa_id = ?", mesa) > 0);
    }

    @Test
    @DisplayName("cambiar el estado con una conexion prestada hace lo mismo")
    void cambiarElEstadoConConexionPrestada() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        mesas.cambiarEstado(conn, mesa, "MANTENIMIENTO", "CIERRE_PEDIDO");

        assertEquals("MANTENIMIENTO", mesaRepo.findById(mesa).orElseThrow().getCodigoEstado());
    }

    @Test
    @DisplayName("un estado que no esta en la tabla se rechaza")
    void elEstadoQueNoExisteSeRechaza() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        // Queda documentado: el enum enums.EstadoMesa tiene EN_LIMPIEZA, pero la
        // tabla estados_mesa solo trae DISPONIBLE, LIBRE, MANTENIMIENTO, OCUPADA
        // y RESERVADA. Usar la constante del enum para cambiar el estado falla.
        assertThrows(exceptions.MesaNotFoundException.class,
                () -> mesas.cambiarEstado(mesa, "EN_LIMPIEZA", "CIERRE_PEDIDO"));
    }

    @Test
    @DisplayName("se consulta la zona de una mesa")
    void seConsultaLaZona() throws SQLException {
        Zona z = mesas.obtenerZona(BaseDePrueba.zona(conn));

        assertNotNull(z);
        assertNotNull(z.getNombreZona());
    }

    @Test
    @DisplayName("se consulta un estado de mesa por su codigo")
    void seConsultaElEstadoPorCodigo() throws SQLException {
        EstadoMesa e = mesas.obtenerEstadoPorCodigo("DISPONIBLE");

        assertNotNull(e);
        assertEquals("DISPONIBLE", e.getCodigoEstado());
    }

    @Test
    @DisplayName("anotar cuantos comensales se sentaron queda guardado")
    void seAnotanLosComensales() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 6);

        mesas.actualizarCantidadComensales(mesa, 3);

        // Se comprueba contra la base y no contra findById, porque findById no
        // rellena los datos de atencion: eso solo lo hace el listado del salon.
        assertEquals(3, BaseDePrueba.contarFilas(conn,
                "SELECT cantidad_comensales FROM mesas WHERE id_mesa = ?", mesa));
    }

    @Test
    @DisplayName("no se pueden sentar mas comensales de los que caben")
    void noCabenMasComensalesQueLaCapacidad() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        assertThrows(IllegalArgumentException.class,
                () -> mesas.actualizarCantidadComensales(mesa, 10));
    }

    @Test
    @DisplayName("no se pueden sentar cero comensales")
    void noSeSientanCeroComensales() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        assertThrows(IllegalArgumentException.class,
                () -> mesas.actualizarCantidadComensales(mesa, 0));
    }

    @Test
    @DisplayName("el listado del salon si trae los comensales anotados")
    void elListadoTraeLosComensales() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 6);
        mesas.actualizarCantidadComensales(mesa, 3);

        Mesa m = mesas.listarMesas().stream()
                .filter(x -> x.getIdMesa() == mesa).findFirst().orElseThrow();

        assertEquals(3, m.getCantidadComensales());
    }

    @Test
    @DisplayName("quitar una mesa la saca del salon")
    void quitarLaMesaLaSacaDelSalon() throws SQLException {
        int numero = siguienteNumeroDeMesa();
        mesas.registrarMesa(numero, 4);
        Mesa m = mesas.listarMesas().stream()
                .filter(x -> x.getNumeroMesa() == numero).findFirst().orElseThrow();

        mesas.quitarMesa(m.getIdMesa());

        assertFalse(mesas.listarMesas().stream().anyMatch(x -> x.getIdMesa() == m.getIdMesa()));
    }

    @Test
    @DisplayName("todas las mesas del salon traen zona y estado")
    void todasLasMesasTraenZonaYEstado() throws SQLException {
        BaseDePrueba.crearMesa(conn, 4);

        List<Mesa> todas = mesas.listarMesas();
        assertFalse(todas.isEmpty());
        for (Mesa m : todas) {
            assertNotNull(m.getNombreZona(), "la mesa " + m.getIdMesa() + " no trae zona");
            assertNotNull(m.getCodigoEstado(), "la mesa " + m.getIdMesa() + " no trae estado");
        }
    }

    @Test
    @DisplayName("una mesa que no existe devuelve vacio")
    void mesaInexistenteDevuelveVacio() throws SQLException {
        assertTrue(mesaRepo.findById(99999999).isEmpty());
    }

    // ----------------------------------------------------------- reservas --

    /** Deja una mesa reservada y devuelve el id de la reserva. */
    private int reservarMesa(int mesa, LocalDateTime cuando) throws SQLException {
        mesas.cambiarEstado(mesa, "RESERVADA", "RESERVA");
        return (int) BaseDePrueba.contarFilas(conn,
                "INSERT INTO reservas (id_mesa, fecha_hora_reserva, nombre_cliente, "
                        + "cantidad_personas, is_active) VALUES (?, ?, ?, ?, 1) RETURNING id_reserva",
                mesa, java.sql.Timestamp.valueOf(cuando), BaseDePrueba.nombreUnico("Cliente"), 4);
    }

    @Test
    @DisplayName("se encuentra una reserva por su identificador")
    void seEncuentraLaReserva() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        int id = reservarMesa(mesa, LocalDateTime.now().plusHours(2));

        Reserva r = reservas.findById(id).orElseThrow();

        assertEquals(id, r.getIdReserva());
        assertEquals(mesa, r.getIdMesa());
        assertTrue(r.isActiva());
    }

    @Test
    @DisplayName("una reserva que no existe devuelve vacio")
    void reservaInexistenteDevuelveVacio() throws SQLException {
        assertTrue(reservas.findById(99999999).isEmpty());
    }

    @Test
    @DisplayName("la mesa reservada aparece en la lista de reservadas")
    void laMesaReservadaAparece() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        int id = reservarMesa(mesa, LocalDateTime.now().plusHours(2));

        assertTrue(reservas.obtenerMesasReservadas().stream()
                .anyMatch(r -> r.getIdReserva() == id));
    }

    @Test
    @DisplayName("cancelar la reserva la deja inactiva y con fecha de cancelacion")
    void cancelarDejaLaReservaInactiva() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        int id = reservarMesa(mesa, LocalDateTime.now().plusHours(2));

        reservas.cancelar(id, LocalDateTime.now());

        Reserva r = reservas.findById(id).orElseThrow();
        assertFalse(r.isActiva());
        assertNotNull(r.getFechaCancelacion());
    }

    @Test
    @DisplayName("la reserva cancelada sale de la lista de reservadas")
    void laCanceladaSaleDeLaLista() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        int id = reservarMesa(mesa, LocalDateTime.now().plusHours(2));

        reservas.cancelar(id, LocalDateTime.now());

        assertFalse(reservas.obtenerMesasReservadas().stream()
                .anyMatch(r -> r.getIdReserva() == id));
    }

    @Test
    @DisplayName("cancelar con una conexion prestada hace lo mismo")
    void cancelarConConexionPrestada() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        int id = reservarMesa(mesa, LocalDateTime.now().plusHours(2));

        reservas.cancelar(conn, id, LocalDateTime.now());

        assertFalse(reservas.findById(id).orElseThrow().isActiva());
    }

    @Test
    @DisplayName("archivar la reserva la saca de las activas")
    void archivarLaSacaDeLasActivas() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        int id = reservarMesa(mesa, LocalDateTime.now().plusHours(2));

        reservas.archivarReserva(id);

        assertFalse(reservas.obtenerMesasReservadas().stream()
                .anyMatch(r -> r.getIdReserva() == id));
    }

    @Test
    @DisplayName("las reservas del turno se pueden consultar")
    void lasReservasDelTurnoSeConsultan() throws SQLException {
        assertNotNull(reservas.obtenerReservasDelTurnoActual());
    }

    @Test
    @DisplayName("la reserva trae el nombre del cliente y para cuantos es")
    void laReservaTraeClienteYPersonas() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        int id = reservarMesa(mesa, LocalDateTime.now().plusHours(2));

        Reserva r = reservas.findById(id).orElseThrow();
        assertNotNull(r.getNombreCliente());
        assertEquals(4, r.getCantidadPersonas());
        assertNotNull(r.getFechaHoraReserva());
    }

    // ------------------------------------------------------ autenticacion --

    @Test
    @DisplayName("un usuario con la contrasena correcta entra")
    void entraConLaContrasenaCorrecta() {
        assertEquals(AutenticacionService.Resultado.OK,
                autenticacion.iniciarSesion("ADM-001", "admin123"));
    }

    @Test
    @DisplayName("cada rol de la base puede entrar con su clave")
    void cadaRolEntraConSuClave() {
        assertEquals(AutenticacionService.Resultado.OK,
                autenticacion.iniciarSesion("MES-001", "mesero123"));
        assertEquals(AutenticacionService.Resultado.OK,
                autenticacion.iniciarSesion("COC-001", "cocina123"));
        assertEquals(AutenticacionService.Resultado.OK,
                autenticacion.iniciarSesion("CAJ-001", "cajero123"));
    }

    @Test
    @DisplayName("una contrasena equivocada no entra")
    void laContrasenaEquivocadaNoEntra() {
        assertEquals(AutenticacionService.Resultado.CREDENCIALES_INVALIDAS,
                autenticacion.iniciarSesion("ADM-001", "la-que-no-es"));
    }

    @Test
    @DisplayName("un usuario que no existe no entra")
    void elUsuarioInexistenteNoEntra() {
        assertEquals(AutenticacionService.Resultado.CREDENCIALES_INVALIDAS,
                autenticacion.iniciarSesion("NO-EXISTE-999", "cualquiera"));
    }

    @Test
    @DisplayName("sin usuario o sin contrasena avisa que faltan campos")
    void sinDatosAvisaCamposVacios() {
        assertEquals(AutenticacionService.Resultado.CAMPOS_VACIOS,
                autenticacion.iniciarSesion("", "admin123"));
        assertEquals(AutenticacionService.Resultado.CAMPOS_VACIOS,
                autenticacion.iniciarSesion("ADM-001", ""));
        assertEquals(AutenticacionService.Resultado.CAMPOS_VACIOS,
                autenticacion.iniciarSesion(null, null));
    }

    @Test
    @DisplayName("un usuario desactivado no entra, aunque la clave sea correcta")
    void elUsuarioInactivoNoEntra() throws SQLException {
        BaseDePrueba.ejecutar(conn,
                "UPDATE usuarios SET is_active = 0 WHERE codigo_empleado = 'COC-001'");
        try {
            assertEquals(AutenticacionService.Resultado.USUARIO_INACTIVO,
                    autenticacion.iniciarSesion("COC-001", "cocina123"));
        } finally {
            BaseDePrueba.ejecutar(conn,
                    "UPDATE usuarios SET is_active = 1 WHERE codigo_empleado = 'COC-001'");
        }
    }

    @Test
    @DisplayName("cada resultado del login tiene un mensaje para la pantalla")
    void cadaResultadoTieneMensaje() {
        for (AutenticacionService.Resultado r : AutenticacionService.Resultado.values()) {
            assertNotNull(AutenticacionService.mensajeDe(r), "el resultado " + r + " no tiene mensaje");
        }
    }

    // -------------------------------------------------------------- menu --

    @Test
    @DisplayName("el menu llega agrupado por categorias")
    void elMenuLlegaPorCategorias() throws SQLException {
        Map<String, List<Producto>> menu = productos.obtenerMenuPorCategorias();

        assertNotNull(menu);
        for (Map.Entry<String, List<Producto>> grupo : menu.entrySet()) {
            assertNotNull(grupo.getKey());
            assertFalse(grupo.getValue().isEmpty(),
                    "la categoria " + grupo.getKey() + " llego vacia");
        }
    }

    @Test
    @DisplayName("los productos del menu traen codigo, nombre y precio")
    void losProductosDelMenuEstanCompletos() throws SQLException {
        for (List<Producto> grupo : productos.obtenerMenuPorCategorias().values()) {
            for (Producto p : grupo) {
                assertNotNull(p.getCodigo());
                assertNotNull(p.getNombre());
                assertNotNull(p.getPrecioVenta());
            }
        }
    }

    @Test
    @DisplayName("preguntar por la disponibilidad desde el menu no revienta")
    void preguntarDisponibilidadDesdeElMenu() throws SQLException {
        long producto = BaseDePrueba.producto(conn);

        assertDoesNotThrow(() -> productos.estaDisponible(producto));
        assertDoesNotThrow(() -> productos.estaDisponible(producto, 2));
    }
}
