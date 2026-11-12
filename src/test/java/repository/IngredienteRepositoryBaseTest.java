package repository;

import entity.Ingrediente;
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
 * IngredienteRepository contra la base de datos de verdad.
 *
 * Es el repositorio del que cuelga todo el inventario: si guarda mal, las
 * alertas de stock y el descuento por comanda quedan mal desde el principio.
 */
class IngredienteRepositoryBaseTest {

    private static Connection conn;
    private static long unidad;

    private final IngredienteRepository repo = new IngredienteRepository();

    @BeforeAll
    static void abrirConexion() throws SQLException {
        conn = BaseDePrueba.conectarOOmitir();
        unidad = BaseDePrueba.unidadDeMedida(conn);
    }

    @AfterAll
    static void cerrarConexion() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    private Ingrediente nuevo(String stockActual, String stockMinimo) {
        Ingrediente i = new Ingrediente();
        i.setNombre(BaseDePrueba.nombreUnico("Aguacate"));
        i.setUnidadMedidaId(unidad);
        i.setCostoUnitario(new BigDecimal("3500.00"));
        i.setStockActual(new BigDecimal(stockActual));
        i.setStockMinimo(new BigDecimal(stockMinimo));
        i.setEstado("ACTIVO");
        return i;
    }

    // ----------------------------------------------------------- guardar --

    @Test
    @DisplayName("guardar un ingrediente nuevo le asigna identificador")
    void guardarAsignaIdentificador() throws SQLException {
        Ingrediente guardado = repo.save(nuevo("12.5", "5"));

        assertNotNull(guardado.getId());
        assertTrue(guardado.getId() > 0);
    }

    @Test
    @DisplayName("lo que se guarda es lo que se vuelve a leer")
    void loGuardadoSeVuelveALeer() throws SQLException {
        Ingrediente original = nuevo("12.5", "5");
        repo.save(original);

        Ingrediente leido = repo.findById(original.getId()).orElseThrow();

        assertEquals(original.getNombre(), leido.getNombre());
        assertEquals(0, leido.getStockActual().compareTo(new BigDecimal("12.5")));
        assertEquals(0, leido.getStockMinimo().compareTo(new BigDecimal("5")));
        assertEquals("ACTIVO", leido.getEstado());
    }

    @Test
    @DisplayName("guardar un ingrediente que ya tiene identificador lo actualiza, no lo duplica")
    void guardarConIdentificadorActualiza() throws SQLException {
        Ingrediente i = repo.save(nuevo("10", "2"));
        long id = i.getId();

        i.setStockActual(new BigDecimal("99"));
        repo.save(i);

        assertEquals(id, i.getId(), "no deberia crear otra fila");
        assertEquals(0, repo.findById(id).orElseThrow()
                .getStockActual().compareTo(new BigDecimal("99")));
    }

    @Test
    @DisplayName("sin unidad de medida se niega a guardar")
    void sinUnidadDeMedidaNoGuarda() {
        Ingrediente sinUnidad = nuevo("1", "1");
        sinUnidad.setUnidadMedidaId(null);

        SQLException error = assertThrows(SQLException.class, () -> repo.save(sinUnidad));
        assertTrue(error.getMessage().contains("unidad_medida_id"));
    }

    // ------------------------------------------------------------ buscar --

    @Test
    @DisplayName("buscar un identificador que no existe devuelve vacio, no revienta")
    void buscarInexistenteDevuelveVacio() throws SQLException {
        assertTrue(repo.findById(99999999L).isEmpty());
    }

    @Test
    @DisplayName("buscar con una conexion prestada da el mismo resultado")
    void buscarConConexionPrestada() throws SQLException {
        Ingrediente i = repo.save(nuevo("7", "3"));

        assertEquals(i.getNombre(), repo.findById(conn, i.getId()).orElseThrow().getNombre());
    }

    @Test
    @DisplayName("findByIdForUpdate trae el ingrediente dentro de la transaccion")
    void buscarParaActualizar() throws SQLException {
        Ingrediente i = repo.save(nuevo("8", "4"));

        Ingrediente bloqueado = repo.findByIdForUpdate(conn, i.getId()).orElseThrow();

        assertEquals(i.getId(), bloqueado.getId());
        assertEquals(0, bloqueado.getStockActual().compareTo(new BigDecimal("8")));
    }

    // ------------------------------------------------------------ listar --

    @Test
    @DisplayName("el ingrediente activo aparece en la lista de activos")
    void elActivoApareceEnLaLista() throws SQLException {
        Ingrediente i = repo.save(nuevo("5", "1"));

        assertTrue(repo.findActivos().stream()
                .anyMatch(x -> x.getId().equals(i.getId())));
    }

    @Test
    @DisplayName("el ingrediente inactivo no aparece en la lista de activos")
    void elInactivoNoAparece() throws SQLException {
        Ingrediente i = nuevo("5", "1");
        i.setEstado("INACTIVO");
        repo.save(i);

        assertFalse(repo.findActivos().stream()
                .anyMatch(x -> x.getId().equals(i.getId())));
    }

    @Test
    @DisplayName("se puede buscar por un pedazo del nombre")
    void buscarPorPedazoDelNombre() throws SQLException {
        Ingrediente i = repo.save(nuevo("5", "1"));

        assertTrue(repo.findActivosPorNombre(i.getNombre()).stream()
                .anyMatch(x -> x.getId().equals(i.getId())));
    }

    @Test
    @DisplayName("un nombre que no existe devuelve una lista vacia, no nula")
    void nombreQueNoExisteDevuelveListaVacia() throws SQLException {
        assertTrue(repo.findActivosPorNombre("zzz-no-existe-zzz").isEmpty());
    }

    @Test
    @DisplayName("findAllActivos tambien lista el ingrediente recien creado")
    void findAllActivosLoLista() throws SQLException {
        Ingrediente i = repo.save(nuevo("5", "1"));

        assertTrue(repo.findAllActivos().stream()
                .anyMatch(x -> x.getId().equals(i.getId())));
    }

    // ------------------------------------------------------------- stock --

    @Test
    @DisplayName("actualizar el stock deja el valor nuevo en la base")
    void actualizarStockDejaElValorNuevo() throws SQLException {
        Ingrediente i = repo.save(nuevo("10", "2"));

        repo.actualizarStock(conn, i.getId(), new BigDecimal("3.25"));

        assertEquals(0, repo.findById(i.getId()).orElseThrow()
                .getStockActual().compareTo(new BigDecimal("3.25")));
    }

    @Test
    @DisplayName("actualizar el stock a cero tambien funciona")
    void actualizarStockACero() throws SQLException {
        Ingrediente i = repo.save(nuevo("10", "2"));

        repo.actualizarStock(conn, i.getId(), BigDecimal.ZERO);

        assertEquals(0, repo.findById(i.getId()).orElseThrow().getStockActual().signum());
    }

    @Test
    @DisplayName("el ingrediente leido sabe si esta por debajo del minimo")
    void elLeidoSabeSiEstaBajo() throws SQLException {
        Ingrediente escaso = repo.save(nuevo("1", "5"));
        Ingrediente sobrado = repo.save(nuevo("50", "5"));

        assertTrue(repo.findById(escaso.getId()).orElseThrow().tieneStockBajo());
        assertFalse(repo.findById(sobrado.getId()).orElseThrow().tieneStockBajo());
    }
}
