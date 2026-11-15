package service;

import entity.Adicional;
import entity.DetallePedidoAdicional;
import entity.Plato;
import entity.ReglaDescuento;
import exceptions.AsignacionActivaException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repository.AdicionalRepository;
import repository.DetallePedidoRepository;
import repository.PlatoRepository;
import repository.ReglaDescuentoRepository;
import support.BaseDePrueba;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Los servicios y repositorios chicos que todavia no tenian pruebas contra la
 * base: adicionales, reglas de descuento, asignacion de meseros y el alta de
 * platos en la carta.
 */
class ServiciosMenoresBaseTest {

    private static Connection conn;

    private final AdicionalService adicionales = new AdicionalService();
    private final AdicionalRepository adicionalRepo = new AdicionalRepository();
    private final AsignacionMesaService asignaciones = new AsignacionMesaService();
    private final ReglaDescuentoRepository reglas = new ReglaDescuentoRepository();
    private final PlatoRepository platos = new PlatoRepository();
    private final DetallePedidoRepository detalles = new DetallePedidoRepository();

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

    private Adicional adicionalGuardado(String precio) throws SQLException {
        Adicional a = new Adicional();
        a.setNombre(BaseDePrueba.nombreUnico("Porcion"));
        a.setPrecioAdicional(new BigDecimal(precio));
        a.setIsActive(true);
        return adicionalRepo.save(a);
    }

    // -------------------------------------------------------- adicionales --

    @Test
    @DisplayName("el adicional activo aparece en la lista del servicio")
    void elAdicionalApareceEnLaLista() throws SQLException {
        Adicional a = adicionalGuardado("2500");

        assertTrue(adicionales.listarActivos().stream()
                .anyMatch(x -> x.getId().equals(a.getId())));
    }

    @Test
    @DisplayName("se obtiene un adicional por su identificador")
    void seObtieneElAdicional() throws SQLException {
        Adicional a = adicionalGuardado("2500");

        assertEquals(a.getNombre(), adicionales.obtener(a.getId()).getNombre());
    }

    @Test
    @DisplayName("pedir un adicional que no existe avisa con una excepcion del dominio")
    void adicionalInexistenteAvisa() {
        assertThrows(Exception.class, () -> adicionales.obtener(99999999L));
    }

    @Test
    @DisplayName("el sobrecosto es el precio por la cantidad")
    void elSobrecostoEsPrecioPorCantidad() throws SQLException {
        Adicional a = adicionalGuardado("3000");

        assertEquals(0, adicionales.calcularSobrecosto(a, 3).compareTo(new BigDecimal("9000")));
    }

    @Test
    @DisplayName("un adicional sin precio no cobra nada")
    void elAdicionalSinPrecioNoCobra() {
        Adicional sinPrecio = new Adicional();
        sinPrecio.setNombre("Salsa de la casa");

        assertEquals(0, adicionales.calcularSobrecosto(sinPrecio, 3).signum());
    }

    @Test
    @DisplayName("pasar un adicional nulo revienta, no devuelve cero")
    void elAdicionalNuloRevienta() {
        // Queda documentado: el metodo revisa que el precio no sea nulo, pero
        // no que el adicional lo sea. Quien lo llame tiene que haberlo buscado
        // antes.
        assertThrows(NullPointerException.class, () -> adicionales.calcularSobrecosto(null, 3));
    }

    @Test
    @DisplayName("agregar un adicional a un renglon del pedido lo deja guardado")
    void seAgregaElAdicionalAlRenglon() throws SQLException {
        long pedido = BaseDePrueba.crearPedido(conn, BaseDePrueba.crearMesa(conn, 4));
        entity.DetallePedido renglon = new entity.DetallePedido();
        renglon.setPedidoId(pedido);
        renglon.setProductoId(BaseDePrueba.producto(conn));
        renglon.setPrecioUnitario(new BigDecimal("20000"));
        renglon.setCantidad(1);
        detalles.save(renglon);

        Adicional a = adicionalGuardado("2000");
        DetallePedidoAdicional union = adicionales.agregarADetalle(renglon.getId(), a.getId(), 2);

        assertNotNull(union.getId());
        assertEquals(2, union.getCantidad());
        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM detalle_pedido_adicional WHERE detalle_pedido_id = ?",
                renglon.getId()));
    }

    // -------------------------------------------------- asignacion de mesa --

    @Test
    @DisplayName("asignar una mesa a un mesero deja la asignacion activa")
    void asignarDejaLaAsignacionActiva() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        entity.AsignacionMesa a = asignaciones.asignar(mesa, BaseDePrueba.usuario(conn));

        assertNotNull(a.getId());
        assertTrue(a.getIsActive());
    }

    @Test
    @DisplayName("una mesa ya asignada no se le puede entregar a otro mesero")
    void unaMesaAsignadaNoSeReasigna() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long usuario = BaseDePrueba.usuario(conn);
        asignaciones.asignar(mesa, usuario);

        assertThrows(AsignacionActivaException.class, () -> asignaciones.asignar(mesa, usuario));
    }

    @Test
    @DisplayName("al liberar la mesa se puede volver a asignar")
    void alLiberarSePuedeReasignar() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        long usuario = BaseDePrueba.usuario(conn);
        asignaciones.asignar(mesa, usuario);

        asignaciones.liberar(mesa);

        assertDoesNotThrow(() -> asignaciones.asignar(mesa, usuario));
    }

    @Test
    @DisplayName("liberar una mesa que no estaba asignada no revienta")
    void liberarUnaLibreNoRevienta() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        assertDoesNotThrow(() -> asignaciones.liberar(mesa));
    }

    // ---------------------------------------------------- reglas de descuento --

    @Test
    @DisplayName("la regla guardada aparece en la lista")
    void laReglaGuardadaAparece() throws SQLException {
        int antes = reglas.obtenerTodos().size();

        reglas.guardar(new ReglaDescuento("modificacion", 15.0, true));

        assertEquals(antes + 1, reglas.obtenerTodos().size());
    }

    @Test
    @DisplayName("la regla conserva su valor y si es porcentaje")
    void laReglaConservaSusDatos() throws SQLException {
        reglas.guardar(new ReglaDescuento("adicion", 7500.0, false));

        List<ReglaDescuento> todas = reglas.obtenerTodos();
        ReglaDescuento ultima = todas.get(todas.size() - 1);

        assertEquals(7500.0, ultima.getValor());
        assertFalse(ultima.isEsPorcentaje());
        assertEquals("adicion", ultima.getTipoCambio());
    }

    @Test
    @DisplayName("todas las reglas leidas traen identificador")
    void todasLasReglasTraenIdentificador() throws SQLException {
        reglas.guardar(new ReglaDescuento("modificacion", 10.0, true));

        for (ReglaDescuento r : reglas.obtenerTodos()) {
            assertTrue(r.getId() > 0, "la regla llego sin identificador");
        }
    }

    // ------------------------------------------------- alta de platos --

    @Test
    @DisplayName("guardar un plato lo deja en la tabla de productos")
    void guardarElPlatoLoDejaEnProductos() throws SQLException {
        String codigo = BaseDePrueba.nombreUnico("PL");
        Plato p = new Plato(codigo, BaseDePrueba.nombreUnico("Sopa"), "Del dia",
                "Otros", 10000, 4000);

        platos.guardar(p);

        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM productos WHERE codigo = ?", codigo));
    }

    @Test
    @DisplayName("el plato guardado queda en la categoria que se le puso")
    void elPlatoQuedaEnSuCategoria() throws SQLException {
        String codigo = BaseDePrueba.nombreUnico("PL");
        platos.guardar(new Plato(codigo, BaseDePrueba.nombreUnico("Sopa"), "", "Otros", 10000, 4000));

        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM productos p JOIN categorias c ON c.categoria_id = p.categoria_id "
                        + "WHERE p.codigo = ? AND c.nombre = 'Otros'", codigo));
    }

    @Test
    @DisplayName("un plato con una categoria que no existe no se pierde")
    void elPlatoConCategoriaDesconocidaNoSePierde() throws SQLException {
        // Queda en la primera categoria en vez de reventar, que es lo que
        // pasaba antes porque la consulta escribia en una columna inexistente.
        String codigo = BaseDePrueba.nombreUnico("PL");

        platos.guardar(new Plato(codigo, BaseDePrueba.nombreUnico("Plato"), "",
                "Categoria-Que-No-Existe", 15000, 6000));

        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM productos WHERE codigo = ?", codigo));
    }

    @Test
    @DisplayName("el plato guardado conserva el precio y el costo")
    void elPlatoConservaPrecioYCosto() throws SQLException {
        String codigo = BaseDePrueba.nombreUnico("PL");
        platos.guardar(new Plato(codigo, BaseDePrueba.nombreUnico("Sopa"), "", "Otros", 10000, 4000));

        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM productos WHERE codigo = ? "
                        + "AND precio_venta = 10000 AND costo = 4000", codigo));
    }
}
