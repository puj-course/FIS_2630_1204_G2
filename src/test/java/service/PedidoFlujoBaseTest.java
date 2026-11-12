package service;

import dto.ItemPedido;
import entity.Pedido;
import entity.Producto;
import enums.EstadoPedido;
import exceptions.MesaNotFoundException;
import exceptions.MesaOcupadaException;
import exceptions.PedidoNoEditableException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repository.PedidoRepository;
import support.BaseDePrueba;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El ciclo de vida de un pedido contra la base de datos de verdad: abrirlo,
 * ponerle platos, confirmarlo, cerrarlo o cancelarlo.
 *
 * Es el camino que recorre el mesero en cada mesa, y hasta hace poco estaba
 * roto en dos sitios: el detalle del pedido escribia en una columna que no
 * existe y la fachada no descontaba inventario.
 */
class PedidoFlujoBaseTest {

    private static Connection conn;
    private static long usuario;

    private final PedidoService pedidos = new PedidoService();
    private final PedidoRepository repo = new PedidoRepository();
    private final PedidoFacade facade = new PedidoFacade();

    @BeforeAll
    static void abrirConexion() throws SQLException {
        conn = BaseDePrueba.conectarOOmitir();
        usuario = BaseDePrueba.usuario(conn);
    }

    @AfterAll
    static void cerrarConexion() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    private int mesaLibre() throws SQLException {
        return BaseDePrueba.crearMesa(conn, 4);
    }

    /** Crea un producto con receta suficiente y devuelve el producto ya armado. */
    private Producto productoVendible(String precio) throws SQLException {
        long ingrediente = BaseDePrueba.crearIngrediente(conn, "1000", "1");
        String codigo = BaseDePrueba.nombreUnico("P");
        String nombre = BaseDePrueba.nombreUnico("Plato");
        long id = BaseDePrueba.contarFilas(conn,
                "INSERT INTO productos (codigo, nombre, categoria_id, precio_venta, estado) "
                        + "VALUES (?, ?, (SELECT min(categoria_id) FROM categorias), "
                        + "CAST(? AS numeric), 'DISPONIBLE') RETURNING producto_id",
                codigo, nombre, precio);
        BaseDePrueba.ejecutar(conn,
                "INSERT INTO plato_insumo (plato_id, insumo_id, cantidad_necesaria) VALUES (?, ?, 1)",
                id, ingrediente);
        return new Producto(id, codigo, nombre, "", "Plato fuerte", new BigDecimal(precio), "DISPONIBLE");
    }

    // ------------------------------------------------------ abrir pedido --

    @Test
    @DisplayName("crear un pedido lo deja pendiente y con numero")
    void crearDejaPendienteYConNumero() throws SQLException {
        Pedido p = pedidos.crearPedido(mesaLibre(), usuario);

        assertNotNull(p.getId());
        assertEquals(EstadoPedido.PENDIENTE, p.getEstado());
        assertTrue(p.getNumeroPedido().startsWith("PED-"));
    }

    @Test
    @DisplayName("al abrir el pedido la mesa queda ocupada")
    void alAbrirLaMesaQuedaOcupada() throws SQLException {
        int mesa = mesaLibre();
        pedidos.crearPedido(mesa, usuario);

        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM mesas m JOIN estados_mesa e ON e.id_estado_mesa = m.id_estado_mesa "
                        + "WHERE m.id_mesa = ? AND e.codigo_estado = 'OCUPADA'", mesa));
    }

    @Test
    @DisplayName("una mesa con comanda abierta no acepta otra")
    void unaMesaNoAceptaDosComandas() throws SQLException {
        int mesa = mesaLibre();
        pedidos.crearPedido(mesa, usuario);

        assertThrows(MesaOcupadaException.class, () -> pedidos.crearPedido(mesa, usuario));
    }

    @Test
    @DisplayName("el pedido activo de la mesa se encuentra despues")
    void seEncuentraElPedidoActivo() throws SQLException {
        int mesa = mesaLibre();
        Pedido p = pedidos.crearPedido(mesa, usuario);

        assertEquals(p.getId(), pedidos.obtenerPedidoActivo(mesa).orElseThrow().getId());
    }

    @Test
    @DisplayName("una mesa sin comanda no tiene pedido activo")
    void mesaSinComandaNoTienePedido() throws SQLException {
        assertTrue(pedidos.obtenerPedidoActivo(mesaLibre()).isEmpty());
    }

    @Test
    @DisplayName("obtenerOCrear devuelve el que ya hay en vez de abrir otro")
    void obtenerOCrearNoAbreDos() throws SQLException {
        int mesa = mesaLibre();
        Pedido primero = pedidos.obtenerOCrearPedido(mesa, usuario);
        Pedido segundo = pedidos.obtenerOCrearPedido(mesa, usuario);

        assertEquals(primero.getId(), segundo.getId());
    }

    // ---------------------------------------------------- importes y estado --

    @Test
    @DisplayName("actualizar los importes los deja guardados")
    void actualizarImportesLosGuarda() throws SQLException {
        Pedido p = pedidos.crearPedido(mesaLibre(), usuario);

        pedidos.actualizarImportes(p.getId(), "[]",
                new BigDecimal("30000"), new BigDecimal("5700"));

        Pedido leido = repo.findById(p.getId()).orElseThrow();
        assertEquals(0, leido.getSubtotal().compareTo(new BigDecimal("30000")));
        assertEquals(0, leido.getImpuestos().compareTo(new BigDecimal("5700")));
    }

    @Test
    @DisplayName("marcar el inventario como descontado queda registrado")
    void marcarInventarioDescontado() throws SQLException {
        Pedido p = pedidos.crearPedido(mesaLibre(), usuario);

        pedidos.marcarInventarioDescontado(p.getId());

        assertTrue(repo.findById(p.getId()).orElseThrow().isInventarioDescontado());
    }

    @Test
    @DisplayName("cambiar el estado del pedido lo deja en el nuevo estado")
    void cambiarElEstado() throws SQLException {
        Pedido p = pedidos.crearPedido(mesaLibre(), usuario);

        repo.actualizarEstado(p.getId(), EstadoPedido.EN_PREPARACION);

        assertEquals(EstadoPedido.EN_PREPARACION, repo.findById(p.getId()).orElseThrow().getEstado());
    }

    @Test
    @DisplayName("el pedido en preparacion aparece en el tablero de cocina")
    void apareceEnElTableroDeCocina() throws SQLException {
        Pedido p = pedidos.crearPedido(mesaLibre(), usuario);
        repo.actualizarEstado(p.getId(), EstadoPedido.EN_PREPARACION);

        assertTrue(repo.findPedidosParaCocina().stream()
                .anyMatch(x -> x.getId().equals(p.getId())));
    }

    @Test
    @DisplayName("actualizar el pedido entero lo guarda")
    void actualizarElPedidoEntero() throws SQLException {
        Pedido p = pedidos.crearPedido(mesaLibre(), usuario);
        p.setTotal(new BigDecimal("41700"));

        pedidos.actualizarPedido(p);

        assertEquals(0, repo.findById(p.getId()).orElseThrow()
                .getTotal().compareTo(new BigDecimal("41700")));
    }

    @Test
    @DisplayName("un pedido que no existe devuelve vacio")
    void pedidoInexistenteDevuelveVacio() throws SQLException {
        assertTrue(repo.findById(99999999L).isEmpty());
    }

    @Test
    @DisplayName("se encuentra el pedido activo con una conexion prestada")
    void seEncuentraElActivoConConexionPrestada() throws SQLException {
        int mesa = mesaLibre();
        Pedido p = pedidos.crearPedido(mesa, usuario);

        assertEquals(p.getId(),
                repo.findActivoByMesaIdWithConn(conn, mesa).orElseThrow().getId());
    }

    // ------------------------------------------------- cerrar y cancelar --

    @Test
    @DisplayName("cerrar el pedido lo deja entregado")
    void cerrarLoDejaEntregado() throws SQLException {
        Pedido p = pedidos.crearPedido(mesaLibre(), usuario);

        Pedido cerrado = pedidos.cerrarPedido(p.getId(), "DISPONIBLE");

        assertEquals(EstadoPedido.ENTREGADO, cerrado.getEstado());
    }

    @Test
    @DisplayName("al cerrar el pedido la mesa vuelve a quedar disponible")
    void alCerrarLaMesaQuedaDisponible() throws SQLException {
        int mesa = mesaLibre();
        Pedido p = pedidos.crearPedido(mesa, usuario);

        pedidos.cerrarPedido(p.getId(), "DISPONIBLE");

        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM mesas m JOIN estados_mesa e ON e.id_estado_mesa = m.id_estado_mesa "
                        + "WHERE m.id_mesa = ? AND e.codigo_estado = 'DISPONIBLE'", mesa));
    }

    @Test
    @DisplayName("un pedido ya cerrado no se puede volver a cerrar")
    void noSeCierraDosVeces() throws SQLException {
        Pedido p = pedidos.crearPedido(mesaLibre(), usuario);
        pedidos.cerrarPedido(p.getId(), "DISPONIBLE");

        assertThrows(PedidoNoEditableException.class,
                () -> pedidos.cerrarPedido(p.getId(), "DISPONIBLE"));
    }

    @Test
    @DisplayName("cancelar el pedido lo deja cancelado")
    void cancelarLoDejaCancelado() throws SQLException {
        Pedido p = pedidos.crearPedido(mesaLibre(), usuario);

        Pedido cancelado = pedidos.cancelarPedido(p.getId(), "DISPONIBLE");

        assertEquals(EstadoPedido.CANCELADO, cancelado.getEstado());
    }

    @Test
    @DisplayName("un pedido cancelado no se puede cancelar de nuevo")
    void noSeCancelaDosVeces() throws SQLException {
        Pedido p = pedidos.crearPedido(mesaLibre(), usuario);
        pedidos.cancelarPedido(p.getId(), "DISPONIBLE");

        assertThrows(PedidoNoEditableException.class,
                () -> pedidos.cancelarPedido(p.getId(), "DISPONIBLE"));
    }

    @Test
    @DisplayName("al cancelar, la mesa queda libre para otra comanda")
    void alCancelarLaMesaQuedaLibre() throws SQLException {
        int mesa = mesaLibre();
        Pedido p = pedidos.crearPedido(mesa, usuario);
        pedidos.cancelarPedido(p.getId(), "DISPONIBLE");

        assertDoesNotThrow(() -> pedidos.crearPedido(mesa, usuario));
    }

    @Test
    @DisplayName("cerrar un pedido que no existe avisa que no lo encuentra")
    void cerrarUnoQueNoExiste() {
        assertThrows(MesaNotFoundException.class,
                () -> pedidos.cerrarPedido(99999999L, "DISPONIBLE"));
    }

    @Test
    @DisplayName("cancelar un pedido que no existe avisa que no lo encuentra")
    void cancelarUnoQueNoExiste() {
        assertThrows(MesaNotFoundException.class,
                () -> pedidos.cancelarPedido(99999999L, "DISPONIBLE"));
    }

    // -------------------------------------------------------- la fachada --

    @Test
    @DisplayName("confirmar un pedido por la fachada lo deja guardado con sus renglones")
    void confirmarGuardaElPedidoConSusRenglones() throws Exception {
        int mesa = mesaLibre();
        Producto producto = productoVendible("32000");

        Pedido p = facade.confirmarPedido(mesa, usuario, List.of(new ItemPedido(producto, 2)));

        assertNotNull(p.getId());
        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM detalle_pedido WHERE pedido_id = ?", p.getId()));
    }

    @Test
    @DisplayName("confirmar deja el subtotal del pedido guardado")
    void confirmarGuardaElSubtotal() throws Exception {
        int mesa = mesaLibre();
        Producto producto = productoVendible("12000");

        Pedido p = facade.confirmarPedido(mesa, usuario, List.of(new ItemPedido(producto, 3)));

        assertEquals(0, repo.findById(p.getId()).orElseThrow()
                .getSubtotal().compareTo(new BigDecimal("36000")));
    }

    @Test
    @DisplayName("confirmar descuenta el inventario de los platos pedidos")
    void confirmarDescuentaElInventario() throws Exception {
        int mesa = mesaLibre();
        Producto producto = productoVendible("20000");

        facade.confirmarPedido(mesa, usuario, List.of(new ItemPedido(producto, 2)));

        assertTrue(BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM movimientos_inventario WHERE tipo_movimiento = 'SALIDA' "
                        + "AND ingrediente_id IN (SELECT insumo_id FROM plato_insumo WHERE plato_id = ?)",
                producto.getProductoId()) > 0,
                "la fachada tiene que descontar inventario al confirmar");
    }

    @Test
    @DisplayName("confirmar dos veces el mismo pedido se rechaza, para no descontar dos veces")
    void noSeConfirmaDosVeces() throws Exception {
        int mesa = mesaLibre();
        Producto producto = productoVendible("10000");
        facade.confirmarPedido(mesa, usuario, List.of(new ItemPedido(producto, 1)));

        // Es la proteccion que evita que el inventario quede en numeros que no
        // existen si al mesero se le va el dedo dos veces en el boton.
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> facade.confirmarPedido(mesa, usuario, List.of(new ItemPedido(producto, 1))));
        assertTrue(error.getMessage().toLowerCase().contains("inventario"));
    }

    @Test
    @DisplayName("al confirmar, el pedido queda marcado como ya descontado")
    void alConfirmarQuedaMarcadoComoDescontado() throws Exception {
        int mesa = mesaLibre();
        Producto producto = productoVendible("10000");

        Pedido p = facade.confirmarPedido(mesa, usuario, List.of(new ItemPedido(producto, 1)));

        assertTrue(repo.findById(p.getId()).orElseThrow().isInventarioDescontado());
    }

    @Test
    @DisplayName("validar la disponibilidad de un pedido que si se puede hacer no devuelve faltantes")
    void validarDisponibilidadSinFaltantes() throws SQLException {
        Producto producto = productoVendible("15000");

        assertTrue(facade.validarDisponibilidad(List.of(new ItemPedido(producto, 1))).isEmpty());
    }

    @Test
    @DisplayName("validar un pedido vacio no devuelve faltantes")
    void validarPedidoVacio() throws SQLException {
        assertTrue(facade.validarDisponibilidad(List.of()).isEmpty());
    }
}
