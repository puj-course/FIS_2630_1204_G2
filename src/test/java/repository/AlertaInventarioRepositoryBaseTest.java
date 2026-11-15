package repository;

import entity.AlertaInventario;
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
 * AlertaInventarioRepository contra la base de datos de verdad.
 *
 * Es lo que ve el administrador cuando un ingrediente baja del minimo, y lo
 * que evita que se le repita la misma alerta cada vez que se vende un plato.
 */
class AlertaInventarioRepositoryBaseTest {

    private static Connection conn;

    private final AlertaInventarioRepository repo = new AlertaInventarioRepository();

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

    private AlertaInventario alertaPara(long ingredienteId) throws SQLException {
        AlertaInventario a = new AlertaInventario();
        // La restriccion chk_alerta_item_unico exige que la alerta sea de un
        // ingrediente o de un producto, nunca de los dos a la vez.
        a.setIngredienteId(ingredienteId);
        a.setProductoId(null);
        a.setStockActual(new BigDecimal("2.0000"));
        a.setStockMinimo(new BigDecimal("5.0000"));
        a.setCantidadSugerida(new BigDecimal("3.0000"));
        a.setEstado("PENDIENTE");
        return a;
    }

    private long ingredienteEscaso() throws SQLException {
        return BaseDePrueba.crearIngrediente(conn, "2", "5");
    }

    // ----------------------------------------------------------- guardar --

    @Test
    @DisplayName("guardar una alerta le asigna identificador y fecha de creacion")
    void guardarAsignaIdentificadorYFecha() throws SQLException {
        AlertaInventario guardada = repo.save(alertaPara(ingredienteEscaso()));

        assertNotNull(guardada.getId());
        assertTrue(guardada.getId() > 0);
        assertNotNull(guardada.getCreatedAt());
    }

    @Test
    @DisplayName("la alerta guardada nace pendiente y sin fecha de cierre")
    void naceePendienteYSinCierre() throws SQLException {
        AlertaInventario guardada = repo.save(alertaPara(ingredienteEscaso()));

        assertTrue(guardada.estaPendiente());
        assertNull(guardada.getResolvedAt());
    }

    @Test
    @DisplayName("guarda el stock que disparo la alerta y cuanto hay que comprar")
    void guardaLosNumerosDeLaAlerta() throws SQLException {
        long ingrediente = ingredienteEscaso();
        repo.save(alertaPara(ingrediente));

        AlertaInventario leida = repo.findPendienteByIngredienteId(ingrediente).orElseThrow();

        assertEquals(0, leida.getStockActual().compareTo(new BigDecimal("2")));
        assertEquals(0, leida.getStockMinimo().compareTo(new BigDecimal("5")));
        assertEquals(0, leida.getCantidadSugerida().compareTo(new BigDecimal("3")));
    }

    // ------------------------------------------------------------ buscar --

    @Test
    @DisplayName("se encuentra la alerta pendiente de un ingrediente")
    void seEncuentraLaPendiente() throws SQLException {
        long ingrediente = ingredienteEscaso();
        AlertaInventario guardada = repo.save(alertaPara(ingrediente));

        assertEquals(guardada.getId(),
                repo.findPendienteByIngredienteId(ingrediente).orElseThrow().getId());
    }

    @Test
    @DisplayName("un ingrediente sin alertas devuelve vacio")
    void sinAlertasDevuelveVacio() throws SQLException {
        long tranquilo = BaseDePrueba.crearIngrediente(conn, "100", "5");

        assertTrue(repo.findPendienteByIngredienteId(tranquilo).isEmpty());
    }

    @Test
    @DisplayName("la alerta aparece en la lista de pendientes")
    void apareceEnLaListaDePendientes() throws SQLException {
        AlertaInventario guardada = repo.save(alertaPara(ingredienteEscaso()));

        assertTrue(repo.findPendientes().stream()
                .anyMatch(a -> a.getId().equals(guardada.getId())));
    }

    @Test
    @DisplayName("todas las de la lista de pendientes estan pendientes")
    void todasLasDeLaListaEstanPendientes() throws SQLException {
        repo.save(alertaPara(ingredienteEscaso()));

        for (AlertaInventario a : repo.findPendientes()) {
            assertTrue(a.estaPendiente(), "la alerta " + a.getId() + " no esta pendiente");
        }
    }

    // ----------------------------------------------------------- resolver --

    @Test
    @DisplayName("resolver una alerta la saca de los pendientes")
    void resolverLaSacaDeLosPendientes() throws SQLException {
        long ingrediente = ingredienteEscaso();
        AlertaInventario guardada = repo.save(alertaPara(ingrediente));

        repo.resolver(guardada.getId());

        assertTrue(repo.findPendienteByIngredienteId(ingrediente).isEmpty());
        assertFalse(repo.findPendientes().stream()
                .anyMatch(a -> a.getId().equals(guardada.getId())));
    }

    @Test
    @DisplayName("resolver una alerta que no existe no revienta")
    void resolverUnaQueNoExisteNoRevienta() {
        assertDoesNotThrow(() -> repo.resolver(99999999L));
    }

    @Test
    @DisplayName("el contador de pendientes sube cuando se crea una alerta")
    void elContadorSube() throws SQLException {
        long antes = repo.countPendientes();

        repo.save(alertaPara(ingredienteEscaso()));

        assertEquals(antes + 1, repo.countPendientes());
    }

    @Test
    @DisplayName("el contador de pendientes baja cuando se resuelve")
    void elContadorBaja() throws SQLException {
        AlertaInventario guardada = repo.save(alertaPara(ingredienteEscaso()));
        long conLaAlerta = repo.countPendientes();

        repo.resolver(guardada.getId());

        assertEquals(conLaAlerta - 1, repo.countPendientes());
    }

    @Test
    @DisplayName("el contador nunca es negativo")
    void elContadorNuncaEsNegativo() throws SQLException {
        assertTrue(repo.countPendientes() >= 0);
    }
}
