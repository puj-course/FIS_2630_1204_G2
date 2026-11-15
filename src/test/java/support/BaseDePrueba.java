package support;

import ConexionBD.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assumptions.abort;

/**
 * Ayudas para las pruebas que hablan con la base de datos de verdad.
 *
 * Todas crean sus propias filas con nombres que llevan un sufijo distinto en
 * cada corrida, asi que dos pruebas no se pisan y se pueden volver a correr
 * sobre la misma base sin limpiarla.
 */
public final class BaseDePrueba {

    /** Sufijo unico de esta corrida, para no chocar con datos que ya existan. */
    private static final String SUFIJO = Long.toString(System.nanoTime() % 100000000L);

    private BaseDePrueba() {
    }

    /**
     * Abre una conexion, o corta la prueba si no hay base disponible.
     * Se usa en el @BeforeAll para que la clase entera se omita en vez de
     * fallar cuando se corre sin PostgreSQL.
     */
    public static Connection conectarOOmitir() {
        try {
            return ConexionBD.getConnection();
        } catch (SQLException e) {
            return abort("No hay base de datos disponible: " + e.getMessage());
        }
    }

    /** Un nombre que no choca con nada, ni entre corridas ni entre pruebas. */
    public static String nombreUnico(String base) {
        return base + "-" + SUFIJO + "-" + Integer.toHexString(contador());
    }

    private static int siguiente = 0;

    private static synchronized int contador() {
        return ++siguiente;
    }

    // ------------------------------------------------------------ catalogos --

    /** Devuelve el id de cualquier unidad de medida, creando una si no hay. */
    public static long unidadDeMedida(Connection conn) throws SQLException {
        Long id = primerId(conn, "SELECT unidad_medida_id FROM unidades_medida ORDER BY 1 LIMIT 1");
        if (id != null) {
            return id;
        }
        return insertarYDevolverId(conn,
                "INSERT INTO unidades_medida (nombre, abreviatura) VALUES (?, ?) "
                        + "RETURNING unidad_medida_id",
                nombreUnico("Unidad"), "ux");
    }

    /** Devuelve el id de cualquier usuario del sistema. */
    public static long usuario(Connection conn) throws SQLException {
        Long id = primerId(conn, "SELECT id_usuario FROM usuarios ORDER BY 1 LIMIT 1");
        if (id == null) {
            abort("No hay usuarios sembrados en la base.");
        }
        return id;
    }

    /** Devuelve el id de cualquier zona. */
    public static long zona(Connection conn) throws SQLException {
        Long id = primerId(conn, "SELECT id_zona FROM zonas ORDER BY 1 LIMIT 1");
        if (id == null) {
            abort("No hay zonas sembradas en la base.");
        }
        return id;
    }

    /** Devuelve el id de cualquier producto. */
    public static long producto(Connection conn) throws SQLException {
        Long id = primerId(conn, "SELECT producto_id FROM productos ORDER BY 1 LIMIT 1");
        if (id == null) {
            abort("No hay productos sembrados en la base.");
        }
        return id;
    }

    // --------------------------------------------------------- filas nuevas --

    /** Crea un ingrediente con el stock que se le pida y devuelve su id. */
    public static long crearIngrediente(Connection conn, String stockActual, String stockMinimo)
            throws SQLException {
        return insertarYDevolverId(conn,
                "INSERT INTO ingredientes (nombre, unidad_medida_id, costo_unitario, "
                        + "stock_actual, stock_minimo, estado) "
                        + "VALUES (?, ?, 1000, CAST(? AS numeric), CAST(? AS numeric), 'ACTIVO') "
                        + "RETURNING ingrediente_id",
                nombreUnico("Ingrediente"), unidadDeMedida(conn), stockActual, stockMinimo);
    }

    /**
     * Crea una mesa en la primera zona disponible y devuelve su id.
     *
     * El numero se toma del siguiente libre, no se fija a mano: la restriccion
     * uk_mesas_num_zona no deja repetir numero dentro de una zona, y si se
     * fijara, la segunda corrida de las pruebas fallaria.
     */
    public static int crearMesa(Connection conn, int capacidad) throws SQLException {
        long idEstado = primerId(conn, "SELECT id_estado_mesa FROM estados_mesa ORDER BY 1 LIMIT 1");
        long idZona = zona(conn);
        Long siguiente = primerId(conn,
                "SELECT coalesce(max(numero_mesa), 0) + 1 FROM mesas");
        return (int) insertarYDevolverId(conn,
                "INSERT INTO mesas (numero_mesa, codigo_mesa, capacidad, id_zona, id_estado_mesa) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING id_mesa",
                siguiente.intValue(), nombreUnico("M"), capacidad, idZona, idEstado);
    }

    /** Crea un pedido pendiente para una mesa y devuelve su id. */
    public static long crearPedido(Connection conn, int idMesa) throws SQLException {
        return insertarYDevolverId(conn,
                "INSERT INTO pedidos (numero_pedido, mesa_id, usuario_id, productos, estado, "
                        + "subtotal, impuestos, total) "
                        + "VALUES (?, ?, ?, CAST('[]' AS jsonb), 'PENDIENTE', 0, 0, 0) "
                        + "RETURNING pedido_id",
                nombreUnico("PED"), idMesa, usuario(conn));
    }

    // ------------------------------------------------------------ utilidades --

    /** Cuenta las filas de una tabla. */
    public static long contarFilas(Connection conn, String consulta, Object... parametros)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(consulta)) {
            ponerParametros(stmt, parametros);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    /** Corre una sentencia que no devuelve filas. */
    public static void ejecutar(Connection conn, String sentencia, Object... parametros)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sentencia)) {
            ponerParametros(stmt, parametros);
            stmt.executeUpdate();
        }
    }

    private static Long primerId(Connection conn, String consulta) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(consulta)) {
            return rs.next() ? rs.getLong(1) : null;
        }
    }

    private static long insertarYDevolverId(Connection conn, String sentencia, Object... parametros)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sentencia)) {
            ponerParametros(stmt, parametros);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("La insercion no devolvio identificador.");
                }
                return rs.getLong(1);
            }
        }
    }

    private static void ponerParametros(PreparedStatement stmt, Object... parametros)
            throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            stmt.setObject(i + 1, parametros[i]);
        }
    }
}
