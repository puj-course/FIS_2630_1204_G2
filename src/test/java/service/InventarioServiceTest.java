package service;

import entity.ProductoIngrediente;
import ConexionBD.ConexionBD;
import exceptions.StockInsuficienteException;
import Interface.InsumoObserver;
import org.junit.jupiter.api.*;
import service.InventarioService;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HU - Centralización de Inventario en Ingredientes y Patrón Observer")
class InventarioServiceTest {

    private InventarioService inventarioService;
    private static long ingredienteIdPrueba;
    private static long productoIdPrueba;

    @BeforeAll
    static void configurarDatosGlobales() throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement()) {

            // Insertar un ingrediente de prueba con stock controlado
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ingredientes (nombre, stock_actual, stock_minimo, estado) VALUES ('Ingrediente Test', 50.00, 10.00, 'ACTIVO')",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        ingredienteIdPrueba = rs.getLong(1);
                    }
                }
            }

            // Insertar un producto de prueba
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO productos (nombre, precio, estado) VALUES ('Producto Test', 20000.00, 'DISPONIBLE')",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        productoIdPrueba = rs.getLong(1);
                    }
                }
            }

            // Asociar producto e ingrediente en la tabla intermedia 'producto_ingrediente' (Receta)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO producto_ingrediente (producto_id, ingrediente_id, cantidad_necesaria) VALUES (?, ?, ?)")) {
                ps.setLong(1, productoIdPrueba);
                ps.setLong(2, ingredienteIdPrueba);
                ps.setBigDecimal(3, new BigDecimal("5.00")); // Cada producto consume 5 unidades del ingrediente
                ps.executeUpdate();
            }
        }
    }

    @AfterAll
    static void limpiarDatosGlobales() throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM producto_ingrediente WHERE producto_id = " + productoIdPrueba);
            stmt.executeUpdate("DELETE FROM productos WHERE producto_id = " + productoIdPrueba);
            stmt.executeUpdate("DELETE FROM ingredientes WHERE ingrediente_id = " + ingredienteIdPrueba);
        }
    }

    @BeforeEach
    void setUp() {
        inventarioService = new InventarioService();
    }

    @Test
    @DisplayName("Cálculo correcto del consumo estimado de ingredientes para una comanda")
    void calcularConsumoEstimadoExitoso() throws SQLException {
        Map<Long, Integer> productosPedido = new HashMap<>();
        productosPedido.put(productoIdPrueba, 3); // 3 productos * 5 unidades de ingrediente = 15

        Map<Long, BigDecimal> consumo = inventarioService.calcularConsumoEstimado(productosPedido);

        assertFalse(consumo.isEmpty(), "El consumo total no debe estar vacío");
        assertTrue(consumo.containsKey(ingredienteIdPrueba), "Debe incluir el ingrediente de prueba");
        assertEquals(0, new BigDecimal("15.00").compareTo(consumo.get(ingredienteIdPrueba)),
                "El consumo total calculado debe ser 15.00");
    }

    @Test
    @DisplayName("Descuento exitoso de inventario basado en ingredientes y registro de movimiento")
    void descontarIngredientesExitoso() throws SQLException {
        // Asegurar stock inicial alto para la prueba
        actualizarStockIngrediente(100.00);

        Map<Long, Integer> productosPedido = new HashMap<>();
        productosPedido.put(productoIdPrueba, 2); // Consume 10 unidades (2 * 5)

        long pedidoIdFake = 99999L;
        long usuarioId = 1L;

        // Ejecutar descuento
        assertDoesNotThrow(() -> inventarioService.descontarIngredientes(pedidoIdFake, productosPedido, usuarioId));

        // Verificar que el stock bajó a 90.00
        BigDecimal stockActual = obtenerStockActualBD();
        assertEquals(0, new BigDecimal("90.00").compareTo(stockActual), "El stock final debe ser 90.00");

        // Limpiar el movimiento registrado para dejar limpia la base de pruebas
        limpiarMovimientoPedido(pedidoIdFake);
    }

    @Test
    @DisplayName("Lanza StockInsuficienteException cuando el stock del ingrediente no alcanza")
    void stockInsuficienteLanzaExcepcion() throws SQLException {
        // Dejar stock bajo (2 unidades, y se necesitan 10)
        actualizarStockIngrediente(2.00);

        Map<Long, Integer> productosPedido = new HashMap<>();
        productosPedido.put(productoIdPrueba, 2);

        long pedidoIdFake = 88888L;
        long usuarioId = 1L;

        Exception exception = assertThrows(StockInsuficienteException.class, () -> {
            inventarioService.descontarIngredientes(pedidoIdFake, productosPedido, usuarioId);
        });

        assertTrue(exception.getMessage().contains("Stock insuficiente"),
                "El mensaje de error debe indicar stock insuficiente");
    }

    @Test
    @DisplayName("Patrón Observer: Notifica alerta cuando el stock cae por debajo del mínimo")
    void patronObserverDisparaAlertaStockBajo() throws SQLException {
        // Stock inicial 12.00, mínimo 10.00. Al descontar 5, quedará en 7.00 (por debajo del mínimo).
        actualizarStockIngrediente(12.00);

        // Creamos un Observer mock para capturar si se disparó la notificación
        final boolean[] alertaDisparada = {false};
        InsumoObserver observerMock = (nombreInsumo, stockActual, stockMinimo) -> {
            if (stockActual <= stockMinimo) {
                alertaDisparada[0] = true;
            }
        };

        inventarioService.agregarObservador(observerMock);

        Map<Long, Integer> productosPedido = new HashMap<>();
        productosPedido.put(productoIdPrueba, 1); // Consume 5 unidades -> Stock post-descuento: 7.00

        long pedidoIdFake = 77777L;
        inventarioService.descontarIngredientes(pedidoIdFake, productosPedido, 1L);

        assertTrue(alertaDisparada[0], "El observador debió activarse al quedar el stock por debajo del mínimo");

        limpiarMovimientoPedido(pedidoIdFake);
    }

    // --- Métodos Auxiliares para las Pruebas ---

    private void actualizarStockIngrediente(double nuevoStock) throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE ingredientes SET stock_actual = ? WHERE ingrediente_id = ?")) {
            ps.setBigDecimal(1, BigDecimal.valueOf(nuevoStock));
            ps.setLong(2, ingredienteIdPrueba);
            ps.executeUpdate();
        }
    }

    private BigDecimal obtenerStockActualBD() throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT stock_actual FROM ingredientes WHERE ingrediente_id = ?")) {
            ps.setLong(1, ingredienteIdPrueba);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private void limpiarMovimientoPedido(long pedidoId) throws SQLException {
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM movimiento_inventario WHERE motivo LIKE '%PEDIDO_ID=" + pedidoId + "%'");
        }
    }
}