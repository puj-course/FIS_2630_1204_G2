package repository;

import entity.Adicional;
import entity.NotaMesa;
import entity.UsuarioAutenticable;
import entity.Zona;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import support.BaseDePrueba;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Los repositorios pequenos contra la base de datos de verdad: zonas,
 * adicionales, notas de mesa y usuarios.
 *
 * Son cortos pero los usa toda la aplicacion, asi que vale la pena que esten
 * probados contra el esquema real y no contra un simulacro.
 */
class CatalogosRepositoryBaseTest {

    private static Connection conn;

    private final ZonaRepository zonas = new ZonaRepository();
    private final AdicionalRepository adicionales = new AdicionalRepository();
    private final NotaMesaRepository notas = new NotaMesaRepository();
    private final UsuarioRepository usuarios = new UsuarioRepository();

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

    // ------------------------------------------------------------- zonas --

    @Test
    @DisplayName("se encuentra una zona por su identificador")
    void seEncuentraLaZona() throws SQLException {
        long id = BaseDePrueba.zona(conn);

        Zona z = zonas.findById(id).orElseThrow();

        assertEquals(id, z.getId());
        assertNotNull(z.getNombreZona());
        assertFalse(z.getNombreZona().isBlank());
    }

    @Test
    @DisplayName("una zona que no existe devuelve vacio")
    void zonaInexistenteDevuelveVacio() throws SQLException {
        assertTrue(zonas.findById(99999999L).isEmpty());
    }

    @Test
    @DisplayName("la zona del salon aparece entre las activas")
    void laZonaApareceEntreLasActivas() throws SQLException {
        long id = BaseDePrueba.zona(conn);

        assertTrue(zonas.findAllActivas().stream().anyMatch(z -> z.getId() == id));
    }

    @Test
    @DisplayName("todas las zonas activas vienen marcadas como activas")
    void lasActivasVienenMarcadas() throws SQLException {
        for (Zona z : zonas.findAllActivas()) {
            assertEquals(1, z.getIsActive(), "la zona " + z.getId() + " no esta activa");
        }
    }

    // -------------------------------------------------------- adicionales --

    private Adicional nuevoAdicional(String precio) {
        Adicional a = new Adicional();
        a.setNombre(BaseDePrueba.nombreUnico("Queso extra"));
        a.setPrecioAdicional(new BigDecimal(precio));
        a.setIsActive(true);
        return a;
    }

    @Test
    @DisplayName("guardar un adicional le asigna identificador")
    void guardarAdicionalAsignaIdentificador() throws SQLException {
        Adicional guardado = adicionales.save(nuevoAdicional("3000.00"));

        assertNotNull(guardado.getId());
        assertTrue(guardado.getId() > 0);
    }

    @Test
    @DisplayName("el adicional guardado se vuelve a leer igual")
    void elAdicionalSeVuelveALeer() throws SQLException {
        Adicional original = adicionales.save(nuevoAdicional("3500.00"));

        Adicional leido = adicionales.findById(original.getId()).orElseThrow();

        assertEquals(original.getNombre(), leido.getNombre());
        assertEquals(0, leido.getPrecioAdicional().compareTo(new BigDecimal("3500")));
        assertTrue(leido.getIsActive());
    }

    @Test
    @DisplayName("un adicional que no existe devuelve vacio")
    void adicionalInexistenteDevuelveVacio() throws SQLException {
        assertTrue(adicionales.findById(99999999L).isEmpty());
    }

    @Test
    @DisplayName("el adicional activo aparece en la lista de la carta")
    void elAdicionalActivoApareceEnLaCarta() throws SQLException {
        Adicional a = adicionales.save(nuevoAdicional("2000.00"));

        assertTrue(adicionales.findAllActivos().stream()
                .anyMatch(x -> x.getId().equals(a.getId())));
    }

    @Test
    @DisplayName("un adicional desactivado desaparece de la carta")
    void elAdicionalDesactivadoDesaparece() throws SQLException {
        Adicional a = adicionales.save(nuevoAdicional("2000.00"));

        BaseDePrueba.ejecutar(conn, "UPDATE adicional SET is_active = false WHERE id = ?", a.getId());

        assertFalse(adicionales.findAllActivos().stream()
                .anyMatch(x -> x.getId().equals(a.getId())));
    }

    @Test
    @DisplayName("un adicional gratis se puede guardar")
    void elAdicionalGratisSeGuarda() throws SQLException {
        Adicional gratis = adicionales.save(nuevoAdicional("0.00"));

        assertEquals(0, adicionales.findById(gratis.getId()).orElseThrow()
                .getPrecioAdicional().signum());
    }

    // ------------------------------------------------------ notas de mesa --

    @Test
    @DisplayName("una mesa sin notas devuelve vacio")
    void mesaSinNotasDevuelveVacio() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        assertTrue(notas.obtenerNotaPorMesa(mesa).isEmpty());
    }

    @Test
    @DisplayName("la nota guardada se vuelve a leer con su texto")
    void laNotaSeVuelveALeer() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        notas.guardarNota(mesa, "Cliente alergico al mani");

        NotaMesa leida = notas.obtenerNotaPorMesa(mesa).orElseThrow();
        assertEquals("Cliente alergico al mani", leida.getTextoNota());
        assertEquals(mesa, leida.getIdMesa());
        assertNotNull(leida.getFechaCreacion());
    }

    @Test
    @DisplayName("volver a guardar la nota reemplaza la anterior, no la duplica")
    void volverAGuardarReemplaza() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        notas.guardarNota(mesa, "Primera nota");
        notas.guardarNota(mesa, "Segunda nota");

        assertEquals("Segunda nota", notas.obtenerNotaPorMesa(mesa).orElseThrow().getTextoNota());
        assertEquals(1, BaseDePrueba.contarFilas(conn,
                "SELECT count(*) FROM nota_mesa WHERE id_mesa = ?", mesa));
    }

    @Test
    @DisplayName("eliminar la nota deja la mesa sin notas")
    void eliminarDejaLaMesaSinNotas() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);
        notas.guardarNota(mesa, "Mesa de cumpleanos");

        notas.eliminarNota(mesa);

        assertTrue(notas.obtenerNotaPorMesa(mesa).isEmpty());
    }

    @Test
    @DisplayName("eliminar una nota que no existe no revienta")
    void eliminarUnaQueNoExisteNoRevienta() throws SQLException {
        int mesa = BaseDePrueba.crearMesa(conn, 4);

        assertDoesNotThrow(() -> notas.eliminarNota(mesa));
    }

    // --------------------------------------------------------- usuarios --

    @Test
    @DisplayName("se encuentra un usuario por su codigo de empleado")
    void seEncuentraPorCodigoDeEmpleado() throws SQLException {
        Optional<UsuarioAutenticable> u = usuarios.buscarPorIdentificador("ADM-001");

        assertTrue(u.isPresent(), "la base de prueba deberia traer el usuario ADM-001");
        assertEquals("ADM-001", u.get().codigoEmpleado());
        assertNotNull(u.get().nombre_rol());
    }

    @Test
    @DisplayName("el usuario encontrado trae el hash, no la contrasena")
    void elUsuarioTraeElHash() throws SQLException {
        UsuarioAutenticable u = usuarios.buscarPorIdentificador("ADM-001").orElseThrow();

        assertNotNull(u.passwordHash());
        assertFalse(u.passwordHash().isBlank());
        assertNotEquals("admin123", u.passwordHash());
    }

    @Test
    @DisplayName("un identificador que no existe devuelve vacio")
    void identificadorInexistenteDevuelveVacio() throws SQLException {
        assertTrue(usuarios.buscarPorIdentificador("NO-EXISTE-999").isEmpty());
    }

    @Test
    @DisplayName("cambiar el hash deja el nuevo valor guardado")
    void cambiarElHashLoGuarda() throws SQLException {
        UsuarioAutenticable u = usuarios.buscarPorIdentificador("ADM-001").orElseThrow();
        String anterior = u.passwordHash();

        usuarios.actualizarPasswordHash(u.idUsuario(), "sha256$nuevo$" + "a".repeat(64));

        String ahora = usuarios.buscarPorIdentificador("ADM-001").orElseThrow().passwordHash();
        assertNotEquals(anterior, ahora);

        // Se deja como estaba para no romper las otras pruebas ni el login.
        usuarios.actualizarPasswordHash(u.idUsuario(), anterior);
        assertEquals(anterior, usuarios.buscarPorIdentificador("ADM-001").orElseThrow().passwordHash());
    }
}
