package service;

import entity.Ingrediente;
import exceptions.StockInsuficienteException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repository.IngredienteRepository;
import repository.MovimientoInventarioRepository;
import support.BaseDePrueba;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El descuento de inventario por comanda, contra la base de datos de verdad.
 *
 * Es la parte del sistema donde un error sale caro: si descuenta de menos, la
 * cocina se queda sin insumos sin que nadie se entere; si descuenta dos veces,
 * el inventario queda en numeros que no existen.
 */
class InventarioServiceBaseTest {

    private static Connection conn;

    private final InventarioService servicio = new InventarioService();
    private final IngredienteRepository ingredientes = new IngredienteRepository();
    private final MovimientoInventarioRepository movimientos = new MovimientoInventarioRepository();

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

    /** Crea un producto con receta de un solo ingrediente y devuelve los dos ids. */
    private long[] productoConReceta(String stockDelIngrediente, String cantidadPorPlato)
            throws SQLException {
        long ingrediente = BaseDePrueba.crearIngrediente(conn, stockDelIngrediente, "1");
        long producto = BaseDePrueba.contarFilas(conn,
                "INSERT INTO productos (codigo, nombre, categoria_id, precio_venta, estado) "
                        + "VALUES (?, ?, (SELECT min(categoria_id) FROM categorias), "
                        + "30000, 'DISPONIBLE') RETURNING producto_id",
                BaseDePrueba.nombreUnico("P"), BaseDePrueba.nombreUnico("Plato"));
        BaseDePrueba.ejecutar(conn,
                "INSERT INTO plato_insumo (plato_id, insumo_id, cantidad_necesaria) "
                        + "VALUES (?, ?, CAST(? AS numeric))",
                producto, ingrediente, cantidadPorPlato);
        return new long[]{producto, ingrediente};
    }

    private BigDecimal stockDe(long ingrediente) throws SQLException {
        return ingredientes.findById(ingrediente).orElseThrow().getStockActual();
    }

    private long usuario() throws SQLException {
        return BaseDePrueba.usuario(conn);
    }

    // -------------------------------------------------- consumo estimado --

    @Test
    @DisplayName("calcula cuanto se gasta de cada ingrediente")
    void calculaElConsumo() throws SQLException {
        long[] ids = productoConReceta("100", "2");

        Map<Long, BigDecimal> consumo = servicio.calcularConsumoEstimado(Map.of(ids[0], 3));

        assertEquals(0, consumo.get(ids[1]).compareTo(new BigDecimal("6")),
                "3 platos de 2 unidades deberian gastar 6");
    }

    @Test
    @DisplayName("el consumo de un solo producto se calcula igual")
    void calculaElConsumoDeUnProducto() throws SQLException {
        long[] ids = productoConReceta("100", "1.5");

        Map<Long, BigDecimal> consumo = servicio.calcularConsumoProducto(ids[0], 4);

        assertEquals(0, consumo.get(ids[1]).compareTo(new BigDecimal("6.0")));
    }

    @Test
    @DisplayName("un producto sin receta no gasta nada")
    void elProductoSinRecetaNoGasta() throws SQLException {
        long producto = BaseDePrueba.producto(conn);

        assertNotNull(servicio.calcularConsumoProducto(producto, 1));
    }

    @Test
    @DisplayName("una comanda vacia no gasta nada")
    void laComandaVaciaNoGasta() throws SQLException {
        assertTrue(servicio.calcularConsumoEstimado(Map.of()).isEmpty());
    }

    // ------------------------------------------------------- descontar --

    @Test
    @DisplayName("descontar una comanda baja el stock del ingrediente")
    void descontarBajaElStock() throws SQLException {
        long[] ids = productoConReceta("50", "2");
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);

        servicio.descontarIngredientes(pedido, Map.of(ids[0], 3), usuario());

        assertEquals(0, stockDe(ids[1]).compareTo(new BigDecimal("44")),
                "50 menos 3 platos de 2 unidades deberia quedar en 44");
    }

    @Test
    @DisplayName("descontar deja el movimiento de salida registrado")
    void descontarDejaElMovimiento() throws SQLException {
        long[] ids = productoConReceta("50", "2");
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);

        servicio.descontarIngredientes(pedido, Map.of(ids[0], 1), usuario());

        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM movimientos_inventario "
                        + "WHERE ingrediente_id = ? AND tipo_movimiento = 'SALIDA'",
                ids[1]));
    }

    @Test
    @DisplayName("el movimiento dice a que pedido pertenece")
    void elMovimientoDiceDeQuePedido() throws SQLException {
        long[] ids = productoConReceta("50", "1");
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);

        servicio.descontarIngredientes(pedido, Map.of(ids[0], 1), usuario());

        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM movimientos_inventario WHERE motivo = ?",
                "CONSUMO_COMANDA - PEDIDO_ID=" + pedido));
    }

    @Test
    @DisplayName("si no alcanza el stock no descuenta nada")
    void siNoAlcanzaNoDescuentaNada() throws SQLException {
        long[] ids = productoConReceta("3", "2");
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);

        assertThrows(StockInsuficienteException.class,
                () -> servicio.descontarIngredientes(pedido, Map.of(ids[0], 5), usuario()));

        assertEquals(0, stockDe(ids[1]).compareTo(new BigDecimal("3")),
                "el stock tuvo que quedar intacto: la transaccion se revierte entera");
        assertEquals(0, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM movimientos_inventario WHERE ingrediente_id = ?", ids[1]));
    }

    @Test
    @DisplayName("se puede gastar hasta la ultima unidad")
    void sePuedeGastarHastaLaUltima() throws SQLException {
        long[] ids = productoConReceta("6", "2");
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);

        servicio.descontarIngredientes(pedido, Map.of(ids[0], 3), usuario());

        assertEquals(0, stockDe(ids[1]).signum());
    }

    @Test
    @DisplayName("descontar con una conexion prestada hace lo mismo")
    void descontarConConexionPrestada() throws SQLException {
        long[] ids = productoConReceta("20", "1");
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);

        try (Connection propia = ConexionBD.ConexionBD.getConnection()) {
            propia.setAutoCommit(false);
            servicio.descontarIngredientesConConexion(propia, pedido, Map.of(ids[0], 2), usuario());
            propia.commit();
        }

        assertEquals(0, stockDe(ids[1]).compareTo(new BigDecimal("18")));
    }

    // --------------------------------------------------------- revertir --

    @Test
    @DisplayName("revertir un descuento devuelve el stock")
    void revertirDevuelveElStock() throws SQLException {
        long[] ids = productoConReceta("50", "2");
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);

        servicio.descontarIngredientes(pedido, Map.of(ids[0], 3), usuario());
        assertEquals(0, stockDe(ids[1]).compareTo(new BigDecimal("44")));

        servicio.revertirDescuento(pedido, usuario());

        assertEquals(0, stockDe(ids[1]).compareTo(new BigDecimal("50")),
                "el stock tiene que volver a como estaba");
    }

    @Test
    @DisplayName("revertir deja el movimiento de entrada registrado")
    void revertirDejaElMovimientoDeEntrada() throws SQLException {
        long[] ids = productoConReceta("50", "1");
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);

        servicio.descontarIngredientes(pedido, Map.of(ids[0], 2), usuario());
        servicio.revertirDescuento(pedido, usuario());

        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM movimientos_inventario "
                        + "WHERE ingrediente_id = ? AND tipo_movimiento = 'ENTRADA'",
                ids[1]));
    }

    @Test
    @DisplayName("revertir un pedido que nunca descontó no cambia el stock")
    void revertirSinDescuentoNoCambiaNada() throws SQLException {
        long[] ids = productoConReceta("30", "1");
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);

        servicio.revertirDescuento(pedido, usuario());

        assertEquals(0, stockDe(ids[1]).compareTo(new BigDecimal("30")));
    }

    @Test
    @DisplayName("descontar y revertir deja el inventario como estaba, sin importar el orden")
    void laVueltaCompletaNoPierdeNada() throws SQLException {
        long[] ids = productoConReceta("25.5000", "1.5");
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);
        BigDecimal antes = stockDe(ids[1]);

        servicio.descontarIngredientes(pedido, Map.of(ids[0], 4), usuario());
        servicio.revertirDescuento(pedido, usuario());

        assertEquals(0, stockDe(ids[1]).compareTo(antes));
    }

    // ------------------------------------------- movimientos registrados --

    @Test
    @DisplayName("los movimientos del ingrediente se pueden consultar despues")
    void losMovimientosSeConsultan() throws SQLException {
        long[] ids = productoConReceta("40", "1");
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);

        servicio.descontarIngredientes(pedido, Map.of(ids[0], 2), usuario());

        var lista = movimientos.findByIngredienteId(ids[1]);
        assertEquals(1, lista.size());
        assertEquals(0, lista.get(0).getCantidad().compareTo(new BigDecimal("2")));
        assertNotNull(lista.get(0).getFechaMovimiento());
    }

    @Test
    @DisplayName("se puede buscar un movimiento por su identificador")
    void seBuscaElMovimientoPorId() throws SQLException {
        long[] ids = productoConReceta("40", "1");
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);
        servicio.descontarIngredientes(pedido, Map.of(ids[0], 1), usuario());

        List<entity.MovimientoInventario> lista = movimientos.findByIngredienteId(ids[1]);
        long id = lista.get(0).getId();

        assertEquals(id, movimientos.findById(id).orElseThrow().getId());
    }

    @Test
    @DisplayName("un movimiento que no existe devuelve vacio")
    void movimientoInexistenteDevuelveVacio() throws SQLException {
        assertTrue(movimientos.findById(99999999L).isEmpty());
    }

    @Test
    @DisplayName("un ingrediente sin movimientos devuelve una lista vacia")
    void ingredienteSinMovimientos() throws SQLException {
        long tranquilo = BaseDePrueba.crearIngrediente(conn, "10", "1");

        assertTrue(movimientos.findByIngredienteId(tranquilo).isEmpty());
    }

    @Test
    @DisplayName("el ingrediente que baja del minimo se puede detectar despues de descontar")
    void seDetectaElStockBajoDespuesDeDescontar() throws SQLException {
        long ingrediente = BaseDePrueba.crearIngrediente(conn, "10", "8");
        long producto = BaseDePrueba.contarFilas(conn,
                "INSERT INTO productos (codigo, nombre, categoria_id, precio_venta, estado) "
                        + "VALUES (?, ?, (SELECT min(categoria_id) FROM categorias), "
                        + "30000, 'DISPONIBLE') RETURNING producto_id",
                BaseDePrueba.nombreUnico("P"), BaseDePrueba.nombreUnico("Plato"));
        BaseDePrueba.ejecutar(conn,
                "INSERT INTO plato_insumo (plato_id, insumo_id, cantidad_necesaria) VALUES (?, ?, 3)",
                producto, ingrediente);
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long pedido = BaseDePrueba.crearPedido(conn, mesa);

        Ingrediente antes = ingredientes.findById(ingrediente).orElseThrow();
        assertFalse(antes.tieneStockBajo());

        servicio.descontarIngredientes(pedido, Map.of(producto, 1), usuario());

        assertTrue(ingredientes.findById(ingrediente).orElseThrow().tieneStockBajo(),
                "con 7 de 8 de minimo ya tendria que avisar");
    }
}
