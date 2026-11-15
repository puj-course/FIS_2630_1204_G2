package dao;

import dto.CuentaPago;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import support.BaseDePrueba;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PagoDAO contra la base de datos de verdad.
 *
 * Es la pantalla de caja: busca la cuenta de un pedido, la lista para que el
 * cajero la elija y la cierra. Cerrar mal una cuenta deja plata sin cobrar,
 * asi que aqui se prueban tambien todas las validaciones que lo impiden.
 */
class PagoDAOBaseTest {

    private static Connection conn;
    private static int cajero;
    private static int mesero;

    private final PagoDAO dao = new PagoDAO();

    @BeforeAll
    static void abrirConexion() throws SQLException {
        conn = BaseDePrueba.conectarOOmitir();
        cajero = (int) idDeUsuarioConRol("CAJERO");
        mesero = (int) idDeUsuarioConRol("MESERO");
    }

    @AfterAll
    static void cerrarConexion() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    private static long idDeUsuarioConRol(String rol) throws SQLException {
        long id = BaseDePrueba.contarFilas(conn,
                "SELECT u.id_usuario FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol "
                        + "WHERE UPPER(r.nombre_rol) = ? AND u.is_active = 1 ORDER BY 1 LIMIT 1",
                rol);
        if (id == 0) {
            org.junit.jupiter.api.Assumptions.abort("No hay un usuario con rol " + rol);
        }
        return id;
    }

    /** Crea un pedido con importes y devuelve su numero. */
    private String pedidoCon(String subtotal, String impuestos) throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        String numero = BaseDePrueba.nombreUnico("PED");
        BaseDePrueba.ejecutar(conn,
                "INSERT INTO pedidos (numero_pedido, mesa_id, usuario_id, productos, estado, "
                        + "subtotal, impuestos, total) "
                        + "VALUES (?, ?, ?, CAST('[]' AS jsonb), 'COMPLETADO', "
                        + "CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric))",
                numero, mesa, mesero, subtotal, impuestos,
                new BigDecimal(subtotal).add(new BigDecimal(impuestos)).toPlainString());
        return numero;
    }

    // ------------------------------------------------------ buscar cuenta --

    @Test
    @DisplayName("se encuentra la cuenta de un pedido por su numero")
    void seEncuentraLaCuenta() throws SQLException {
        String numero = pedidoCon("30000", "5700");

        CuentaPago cuenta = dao.buscarCuenta(numero).orElseThrow();

        assertEquals(numero, cuenta.getNumeroPedido());
        assertEquals(0, cuenta.getSubtotal().compareTo(new BigDecimal("30000")));
        assertEquals(0, cuenta.getImpuestos().compareTo(new BigDecimal("5700")));
    }

    @Test
    @DisplayName("la cuenta recien abierta todavia no esta pagada")
    void laCuentaNuevaNoEstaPagada() throws SQLException {
        assertFalse(dao.buscarCuenta(pedidoCon("20000", "3800")).orElseThrow().isPagada());
    }

    @Test
    @DisplayName("la cuenta trae el numero de la mesa")
    void laCuentaTraeLaMesa() throws SQLException {
        assertNotNull(dao.buscarCuenta(pedidoCon("20000", "3800")).orElseThrow().getNumeroMesa());
    }

    @Test
    @DisplayName("un numero de pedido que no existe devuelve vacio")
    void numeroInexistenteDevuelveVacio() throws SQLException {
        assertTrue(dao.buscarCuenta("NO-EXISTE-0000").isEmpty());
    }

    @Test
    @DisplayName("un pedido cancelado no sale como cuenta cobrable")
    void elCanceladoNoSale() throws SQLException {
        String numero = pedidoCon("15000", "2850");
        BaseDePrueba.ejecutar(conn,
                "UPDATE pedidos SET estado = 'CANCELADO' WHERE numero_pedido = ?", numero);

        assertTrue(dao.buscarCuenta(numero).isEmpty());
    }

    @Test
    @DisplayName("el total de la cuenta suma impuestos y resta descuento")
    void elTotalDeLaCuentaCuadra() throws SQLException {
        CuentaPago cuenta = dao.buscarCuenta(pedidoCon("30000", "5700")).orElseThrow();

        assertEquals(0, cuenta.calcularTotal().compareTo(new BigDecimal("35700")));

        cuenta.setDescuento(new BigDecimal("5000"));
        assertEquals(0, cuenta.calcularTotal().compareTo(new BigDecimal("30700")));
    }

    // ----------------------------------------------------- listar cuentas --

    @Test
    @DisplayName("el pedido aparece en la lista desplegable del cajero")
    void apareceEnLaLista() throws SQLException {
        String numero = pedidoCon("12000", "2280");

        assertTrue(dao.listarCuentas().stream()
                .anyMatch(c -> numero.equals(c.getNumeroPedido())));
    }

    @Test
    @DisplayName("la lista no trae pedidos cancelados")
    void laListaNoTraeCancelados() throws SQLException {
        String numero = pedidoCon("12000", "2280");
        BaseDePrueba.ejecutar(conn,
                "UPDATE pedidos SET estado = 'CANCELADO' WHERE numero_pedido = ?", numero);

        assertFalse(dao.listarCuentas().stream()
                .anyMatch(c -> numero.equals(c.getNumeroPedido())));
    }

    @Test
    @DisplayName("las cuentas sin pagar van antes que las pagadas")
    void lasSinPagarVanPrimero() throws SQLException {
        pedidoCon("12000", "2280");

        boolean vistaUnaPagada = false;
        for (CuentaPago c : dao.listarCuentas()) {
            if (c.isPagada()) {
                vistaUnaPagada = true;
            } else {
                assertFalse(vistaUnaPagada,
                        "la cuenta sin pagar " + c.getNumeroPedido() + " quedo despues de una pagada");
            }
        }
    }

    // -------------------------------------------------------- cerrar pago --

    @Test
    @DisplayName("cerrar una cuenta la deja marcada como pagada")
    void cerrarLaDejaPagada() throws SQLException {
        String numero = pedidoCon("30000", "5700");
        CuentaPago cuenta = dao.buscarCuenta(numero).orElseThrow();

        dao.cerrarPago(cuenta, cajero, null);

        assertTrue(dao.buscarCuenta(numero).orElseThrow().isPagada());
    }

    @Test
    @DisplayName("al cerrar, el pedido queda en ENTREGADO")
    void alCerrarElPedidoQuedaEntregado() throws SQLException {
        String numero = pedidoCon("30000", "5700");
        dao.cerrarPago(dao.buscarCuenta(numero).orElseThrow(), cajero, null);

        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM pedidos WHERE numero_pedido = ? AND estado = 'ENTREGADO'",
                numero));
    }

    @Test
    @DisplayName("el descuento y la propina quedan guardados en el pedido")
    void elDescuentoYLaPropinaQuedanGuardados() throws SQLException {
        String numero = pedidoCon("30000", "5700");
        CuentaPago cuenta = dao.buscarCuenta(numero).orElseThrow();
        cuenta.setDescuento(new BigDecimal("5000"));
        cuenta.setPropina(new BigDecimal("3000"));

        dao.cerrarPago(cuenta, cajero, "Cliente frecuente");

        CuentaPago leida = dao.buscarCuenta(numero).orElseThrow();
        assertEquals(0, leida.getDescuento().compareTo(new BigDecimal("5000")));
        assertEquals(0, leida.getPropina().compareTo(new BigDecimal("3000")));
    }

    @Test
    @DisplayName("no se puede cobrar dos veces la misma cuenta")
    void noSeCobraDosVeces() throws SQLException {
        String numero = pedidoCon("30000", "5700");
        dao.cerrarPago(dao.buscarCuenta(numero).orElseThrow(), cajero, null);

        CuentaPago otraVez = dao.buscarCuenta(numero).orElseThrow();
        assertThrows(SQLException.class, () -> dao.cerrarPago(otraVez, cajero, null));
    }

    @Test
    @DisplayName("solo un cajero puede cerrar la cuenta")
    void soloUnCajeroCierra() throws SQLException {
        CuentaPago cuenta = dao.buscarCuenta(pedidoCon("30000", "5700")).orElseThrow();

        assertThrows(SQLException.class, () -> dao.cerrarPago(cuenta, mesero, null));
    }

    // -------------------------------------------------------- validaciones --

    @Test
    @DisplayName("un descuento negativo se rechaza")
    void rechazaDescuentoNegativo() throws SQLException {
        CuentaPago cuenta = dao.buscarCuenta(pedidoCon("30000", "5700")).orElseThrow();
        cuenta.setDescuento(new BigDecimal("-100"));

        SQLException e = assertThrows(SQLException.class, () -> dao.cerrarPago(cuenta, cajero, "x"));
        assertTrue(e.getMessage().toLowerCase().contains("descuento"));
    }

    @Test
    @DisplayName("una propina negativa se rechaza")
    void rechazaPropinaNegativa() throws SQLException {
        CuentaPago cuenta = dao.buscarCuenta(pedidoCon("30000", "5700")).orElseThrow();
        cuenta.setPropina(new BigDecimal("-100"));

        SQLException e = assertThrows(SQLException.class, () -> dao.cerrarPago(cuenta, cajero, null));
        assertTrue(e.getMessage().toLowerCase().contains("propina"));
    }

    @Test
    @DisplayName("un descuento sin motivo se rechaza")
    void rechazaDescuentoSinMotivo() throws SQLException {
        CuentaPago cuenta = dao.buscarCuenta(pedidoCon("30000", "5700")).orElseThrow();
        cuenta.setDescuento(new BigDecimal("1000"));

        assertThrows(SQLException.class, () -> dao.cerrarPago(cuenta, cajero, null));
        assertThrows(SQLException.class, () -> dao.cerrarPago(cuenta, cajero, "   "));
    }

    @Test
    @DisplayName("un descuento mayor que el subtotal se rechaza")
    void rechazaDescuentoMayorQueElSubtotal() throws SQLException {
        CuentaPago cuenta = dao.buscarCuenta(pedidoCon("30000", "5700")).orElseThrow();
        cuenta.setDescuento(new BigDecimal("50000"));

        SQLException e = assertThrows(SQLException.class,
                () -> dao.cerrarPago(cuenta, cajero, "Cortesia"));
        assertTrue(e.getMessage().toLowerCase().contains("subtotal"));
    }

    @Test
    @DisplayName("un descuento igual al subtotal si se acepta")
    void aceptaDescuentoIgualAlSubtotal() throws SQLException {
        String numero = pedidoCon("30000", "5700");
        CuentaPago cuenta = dao.buscarCuenta(numero).orElseThrow();
        cuenta.setDescuento(new BigDecimal("30000"));

        assertDoesNotThrow(() -> dao.cerrarPago(cuenta, cajero, "Cortesia de la casa"));
        assertTrue(dao.buscarCuenta(numero).orElseThrow().isPagada());
    }
}
