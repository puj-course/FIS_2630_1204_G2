package repository;

import dao.DisponibilidadDAO;
import dto.ProductoDisponibilidad;
import entity.AsignacionMesa;
import entity.DetallePedido;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import support.BaseDePrueba;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Los renglones de un pedido, la disponibilidad de los productos y la
 * asignacion de meseros a mesas, contra la base de datos de verdad.
 */
class PedidoDetalleRepositoryBaseTest {

    private static Connection conn;

    private final DetallePedidoRepository detalles = new DetallePedidoRepository();
    private final AsignacionMesaRepository asignaciones = new AsignacionMesaRepository();
    private final DisponibilidadDAO disponibilidad = new DisponibilidadDAO();

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

    private long pedidoNuevo() throws SQLException {
        return BaseDePrueba.crearPedido(conn, BaseDePrueba.crearMesa(conn, 4));
    }

    private DetallePedido renglon(long pedido, long producto, int cantidad, String precio) {
        DetallePedido d = new DetallePedido();
        d.setPedidoId(pedido);
        d.setProductoId(producto);
        d.setPrecioUnitario(new BigDecimal(precio));
        d.setCantidad(cantidad);
        return d;
    }

    // ------------------------------------------------ renglones del pedido --

    @Test
    @DisplayName("guardar un renglon le asigna identificador")
    void guardarRenglonAsignaIdentificador() throws SQLException {
        DetallePedido guardado = detalles.save(
                renglon(pedidoNuevo(), BaseDePrueba.producto(conn), 2, "32000"));

        assertNotNull(guardado.getId());
        assertTrue(guardado.getId() > 0);
    }

    @Test
    @DisplayName("el renglon guardado se vuelve a leer con sus datos")
    void elRenglonSeVuelveALeer() throws SQLException {
        long pedido = pedidoNuevo();
        long producto = BaseDePrueba.producto(conn);
        detalles.save(renglon(pedido, producto, 3, "12000"));

        List<DetallePedido> leidos = detalles.findByPedidoId(pedido);

        assertEquals(1, leidos.size());
        assertEquals(producto, leidos.get(0).getProductoId());
        assertEquals(3, leidos.get(0).getCantidad());
        assertEquals(0, leidos.get(0).getPrecioUnitario().compareTo(new BigDecimal("12000")));
    }

    @Test
    @DisplayName("el subtotal del renglon queda calculado en la base")
    void elSubtotalQuedaCalculado() throws SQLException {
        long pedido = pedidoNuevo();
        detalles.save(renglon(pedido, BaseDePrueba.producto(conn), 3, "12000"));

        assertEquals(0, detalles.findByPedidoId(pedido).get(0)
                .getSubtotal().compareTo(new BigDecimal("36000")));
    }

    @Test
    @DisplayName("un pedido sin renglones devuelve una lista vacia")
    void pedidoSinRenglonesDevuelveListaVacia() throws SQLException {
        assertTrue(detalles.findByPedidoId(pedidoNuevo()).isEmpty());
    }

    @Test
    @DisplayName("un pedido puede llevar varios renglones")
    void unPedidoLlevaVariosRenglones() throws SQLException {
        long pedido = pedidoNuevo();
        long producto = BaseDePrueba.producto(conn);

        detalles.save(renglon(pedido, producto, 1, "10000"));
        detalles.save(renglon(pedido, producto, 2, "5000"));

        assertEquals(2, detalles.findByPedidoId(pedido).size());
    }

    @Test
    @DisplayName("guardar un renglon que ya tiene identificador lo actualiza")
    void guardarConIdentificadorActualiza() throws SQLException {
        long pedido = pedidoNuevo();
        DetallePedido d = detalles.save(renglon(pedido, BaseDePrueba.producto(conn), 1, "10000"));

        d.setCantidad(5);
        detalles.save(d);

        List<DetallePedido> leidos = detalles.findByPedidoId(pedido);
        assertEquals(1, leidos.size(), "no deberia crear otro renglon");
        assertEquals(5, leidos.get(0).getCantidad());
    }

    @Test
    @DisplayName("se pueden guardar todos los renglones de una comanda de una vez")
    void seGuardanTodosDeUnaVez() throws SQLException {
        long pedido = pedidoNuevo();
        long producto = BaseDePrueba.producto(conn);

        detalles.guardarDetallesConConn(conn, List.of(
                renglon(pedido, producto, 1, "10000"),
                renglon(pedido, producto, 2, "7500")));

        assertEquals(2, detalles.findByPedidoId(pedido).size());
    }

    @Test
    @DisplayName("guardar una lista vacia no escribe nada ni revienta")
    void listaVaciaNoEscribeNada() throws SQLException {
        long pedido = pedidoNuevo();

        assertDoesNotThrow(() -> detalles.guardarDetallesConConn(conn, List.of()));
        assertTrue(detalles.findByPedidoId(pedido).isEmpty());
    }

    @Test
    @DisplayName("guardar con una conexion prestada funciona igual")
    void guardarConConexionPrestada() throws SQLException {
        long pedido = pedidoNuevo();

        detalles.saveWithConn(conn, renglon(pedido, BaseDePrueba.producto(conn), 2, "9000"));

        assertEquals(1, detalles.findByPedidoId(pedido).size());
    }

    // -------------------------------------------------- asignacion de mesa --

    @Test
    @DisplayName("una mesa sin mesero no tiene asignacion activa")
    void mesaSinMeseroNoTieneAsignacion() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        assertTrue(asignaciones.findActivaByMesaId(mesa).isEmpty());
    }

    @Test
    @DisplayName("al asignar una mesa queda la asignacion activa")
    void alAsignarQuedaActiva() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        AsignacionMesa a = new AsignacionMesa();
        a.setMesaId((long) mesa);
        a.setUsuarioId(BaseDePrueba.usuario(conn));

        AsignacionMesa guardada = asignaciones.save(a);

        assertNotNull(guardada.getId());
        AsignacionMesa leida = asignaciones.findActivaByMesaId(mesa).orElseThrow();
        assertEquals(guardada.getId(), leida.getId());
        assertTrue(leida.getIsActive());
        assertNotNull(leida.getFechaAsignacion());
    }

    @Test
    @DisplayName("la asignacion guarda a que mesero se le entrego la mesa")
    void laAsignacionGuardaElMesero() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long usuario = BaseDePrueba.usuario(conn);
        AsignacionMesa a = new AsignacionMesa();
        a.setMesaId((long) mesa);
        a.setUsuarioId(usuario);
        asignaciones.save(a);

        assertEquals(usuario, asignaciones.findActivaByMesaId(mesa).orElseThrow().getUsuarioId());
    }

    @Test
    @DisplayName("al cerrar la asignacion la mesa vuelve a quedar libre")
    void alCerrarlaLaMesaQuedaLibre() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        AsignacionMesa a = new AsignacionMesa();
        a.setMesaId((long) mesa);
        a.setUsuarioId(BaseDePrueba.usuario(conn));
        AsignacionMesa guardada = asignaciones.save(a);

        BaseDePrueba.ejecutar(conn,
                "UPDATE asignacion_mesa SET is_active = false WHERE id = ?", guardada.getId());

        assertTrue(asignaciones.findActivaByMesaId(mesa).isEmpty());
    }

    // ---------------------------------------------------- disponibilidad --

    @Test
    @DisplayName("la carta de disponibilidad trae todos los productos")
    void laCartaTraeLosProductos() throws SQLException {
        List<ProductoDisponibilidad> carta = disponibilidad.listarProductos();

        assertFalse(carta.isEmpty());
        for (ProductoDisponibilidad p : carta) {
            assertNotNull(p.getCodigo());
            assertNotNull(p.getNombre());
            assertNotNull(p.getEstado());
        }
    }

    @Test
    @DisplayName("recalcular la disponibilidad de todos devuelve cuantos se revisaron")
    void recalcularTodosDevuelveCuantos() throws SQLException {
        assertTrue(disponibilidad.recalcularTodos() >= 0);
    }

    @Test
    @DisplayName("recalcular un producto devuelve el estado en que quedo")
    void recalcularUnoDevuelveSuEstado() throws SQLException {
        String estado = disponibilidad.recalcularProducto(BaseDePrueba.producto(conn));

        assertNotNull(estado);
        assertTrue(estado.equals("DISPONIBLE") || estado.equals("AGOTADO"),
                "estado inesperado: " + estado);
    }

    @Test
    @DisplayName("preguntar por la disponibilidad de un producto no revienta")
    void preguntarDisponibilidadNoRevienta() throws SQLException {
        long producto = BaseDePrueba.producto(conn);

        assertDoesNotThrow(() -> disponibilidad.estaDisponible(producto));
        assertDoesNotThrow(() -> disponibilidad.estaDisponible(producto, 2));
    }

    @Test
    @DisplayName("un pedido vacio no tiene faltantes")
    void elPedidoVacioNoTieneFaltantes() throws SQLException {
        assertTrue(disponibilidad.faltantesDelPedido(Map.of()).isEmpty());
    }

    @Test
    @DisplayName("se puede preguntar por los faltantes de una comanda")
    void sePreguntaPorLosFaltantes() throws SQLException {
        List<String> faltantes =
                disponibilidad.faltantesDelPedido(Map.of(BaseDePrueba.producto(conn), 1));

        assertNotNull(faltantes, "deberia devolver una lista, aunque sea vacia");
    }

    @Test
    @DisplayName("un producto que no existe no esta disponible")
    void elProductoInexistenteNoEstaDisponible() throws SQLException {
        assertFalse(disponibilidad.estaDisponible(99999999L));
    }
}
