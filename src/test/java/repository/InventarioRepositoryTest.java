package repository;

import ConexionBD.ConexionBD;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InventarioRepository - Pruebas de integración con esquema Gastroflow real")
class InventarioRepositoryTest {

    private static InventarioRepository repository;
    private Connection conn;

    private static long categoriaIdTest;
    private static long productoIdTest;
    private static long ingredienteIdTest;

    @BeforeAll
    static void globalSetup() throws SQLException {
        repository = new InventarioRepository();

        // Insertar datos base globales cumpliendo con las restricciones del DDL
        try (Connection c = ConexionBD.getConnection();
             Statement st = c.createStatement()) {

            // 1. Crear categoría válida requerida por productos
            st.executeUpdate(
                    "INSERT INTO categorias (nombre, descripcion, orden_visual, estado) " +
                            "VALUES ('Categoría Test Repo', 'Test', 1, 'ACTIVO')",
                    Statement.RETURN_GENERATED_KEYS
            );
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) {
                    categoriaIdTest = rs.getLong(1);
                }
            }

            // 2. Crear producto de prueba cumpliendo columnas obligatorias y checks
            st.executeUpdate(
                    "INSERT INTO productos (codigo, nombre, descripcion, categoria_id, precio_venta, costo, estado) " +
                            "VALUES ('PROD-TEST-999', 'Producto Test Repo', 'Desc', " + categoriaIdTest + ", 20000.00, 5000.00, 'DISPONIBLE')",
                    Statement.RETURN_GENERATED_KEYS
            );
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) {
                    productoIdTest = rs.getLong(1);
                }
            }

            // 3. Crear ingrediente de prueba con stock inicial y unidad de medida obligatoria
            st.executeUpdate(
                    "INSERT INTO ingredientes (nombre, unidad_medida, costo_unitario, stock_actual, stock_minimo, estado) " +
                            "VALUES ('Ingrediente Test Repo', 'KG', 1000.00, 100.0000, 10.0000, 'ACTIVO')",
                    Statement.RETURN_GENERATED_KEYS
            );
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) {
                    ingredienteIdTest = rs.getLong(1);
                }
            }

            // 4. Crear un usuario válido requerido por los movimientos de inventario (FK usuario_id)
            // Aseguramos un rol y tipo de documento básicos si no existen para la prueba
            st.executeUpdate(
                    "INSERT INTO roles (nombre_rol, descripcion) VALUES ('ROL_TEST_REPO', 'Test') ON CONFLICT (nombre_rol) DO NOTHING"
            );
            st.executeUpdate(
                    "INSERT INTO tipos_documento (codigo, nombre) VALUES ('CC_TEST', 'Cédula Test') ON CONFLICT (codigo) DO NOTHING"
            );

            long rolId = obtenerIdRolTest(c);
            long tipoDocId = obtenerIdTipoDocTest(c);

            st.executeUpdate(
                    "INSERT INTO usuarios (codigo_empleado, id_tipo_documento, numero_documento, nombre, apellido, correo, password_hash, id_rol, is_active) " +
                            "VALUES ('EMP-TEST-99', " + tipoDocId + ", '999999999', 'Test', 'User', 'test.repo@gastroflow.local', 'HASH', " + rolId + ", 1)"
            );
        }
    }

    @AfterAll
    static void globalTeardown() throws SQLException {
        try (Connection c = ConexionBD.getConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM producto_ingrediente WHERE producto_id = " + productoIdTest);
            st.executeUpdate("DELETE FROM movimientos_inventario WHERE ingrediente_id = " + ingredienteIdTest);
            st.executeUpdate("DELETE FROM productos WHERE producto_id = " + productoIdTest);
            st.executeUpdate("DELETE FROM ingredientes WHERE ingrediente_id = " + ingredienteIdTest);
            st.executeUpdate("DELETE FROM categorias WHERE categoria_id = " + categoriaIdTest);
            st.executeUpdate("DELETE FROM usuarios WHERE codigo_empleado = 'EMP-TEST-99'");
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        conn = ConexionBD.getConnection();
        conn.setAutoCommit(false); // Aislamiento por cada prueba mediante transacciones
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.rollback();
            conn.close();
        }
    }

    @Test
    @DisplayName("Obtener stock actual con bloqueo FOR UPDATE exitoso")
    void testObtenerStockForUpdate() throws SQLException {
        BigDecimal stock = repository.obtenerStockForUpdate(conn, ingredienteIdTest);

        assertNotNull(stock);
        assertEquals(0, new BigDecimal("100.0000").compareTo(stock),
                "El stock inicial debe coincidir con el definido");
    }

    @Test
    @DisplayName("Descontar y devolver stock modifica el saldo correctamente")
    void testDescontarYDevolverStock() throws SQLException {
        repository.descontarStock(conn, ingredienteIdTest, new BigDecimal("15.0000"));
        BigDecimal stockTrasDescuento = repository.obtenerStockForUpdate(conn, ingredienteIdTest);
        assertEquals(0, new BigDecimal("85.0000").compareTo(stockTrasDescuento));

        repository.devolverStock(conn, ingredienteIdTest, new BigDecimal("10.0000"));
        BigDecimal stockTrasDevolucion = repository.obtenerStockForUpdate(conn, ingredienteIdTest);
        assertEquals(0, new BigDecimal("95.0000").compareTo(stockTrasDevolucion));
    }

    @Test
    @DisplayName("Registrar salida de inventario y recuperarla por ID de pedido")
    void testRegistrarYObtenerSalidasPorPedido() throws SQLException {
        long pedidoId = 998877L;
        String motivo = "CONSUMO_COMANDA - PEDIDO_ID=" + pedidoId;
        long usuarioId = obtenerIdUsuarioTest(conn);

        repository.registrarSalidaIngrediente(conn, ingredienteIdTest, new BigDecimal("5.0000"), motivo, usuarioId);

        List<InventarioRepository.MovimientoPedido> salidas = repository.obtenerSalidasPorPedido(conn, pedidoId);

        assertFalse(salidas.isEmpty(), "Debe retornar el movimiento registrado");
        assertEquals(1, salidas.size());
        assertEquals(ingredienteIdTest, salidas.get(0).getIngredienteId());
        assertEquals(0, new BigDecimal("5.0000").compareTo(salidas.get(0).getCantidad()));
    }

    @Test
    @DisplayName("Obtener nombre y stock mínimo del ingrediente")
    void testObtenerNombreYStockMinimo() throws SQLException {
        String nombre = repository.obtenerNombreIngrediente(conn, ingredienteIdTest);
        assertEquals("Ingrediente Test Repo", nombre);

        BigDecimal stockMinimo = repository.obtenerStockMinimo(conn, ingredienteIdTest);
        assertEquals(0, new BigDecimal("10.0000").compareTo(stockMinimo));
    }

    // Métodos auxiliares privados para la gestión de IDs maestros en el test
    private static long obtenerIdRolTest(Connection c) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT id_rol FROM roles WHERE nombre_rol = 'ROL_TEST_REPO'")) {
            if (rs.next()) return rs.getLong(1);
        }
        return 1L;
    }

    private static long obtenerIdTipoDocTest(Connection c) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT id_tipo_documento FROM tipos_documento WHERE codigo = 'CC_TEST'")) {
            if (rs.next()) return rs.getLong(1);
        }
        return 1L;
    }

    private long obtenerIdUsuarioTest(Connection c) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT id_usuario FROM usuarios WHERE codigo_empleado = 'EMP-TEST-99'")) {
            if (rs.next()) return rs.getLong(1);
        }
        return 1L;
    }
}