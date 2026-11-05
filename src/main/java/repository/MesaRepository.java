package repository;

import ConexionBD.ConexionBD;
import entity.Mesa;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MesaRepository {

    private static final String SELECT_BASE =
            "SELECT m.id_mesa, m.numero_mesa, m.codigo_mesa, m.capacidad, " +
                    "       m.id_zona, z.nombre_zona, " +
                    "       m.id_estado_mesa, e.codigo_estado " +
                    "FROM mesas m " +
                    "JOIN zonas z ON m.id_zona = z.id_zona " +
                    "JOIN estados_mesa e ON m.id_estado_mesa = e.id_estado_mesa ";

    private static final int ZONA_POR_DEFECTO = 1;

    /**
     * Obtiene todas las mesas activas.
     */
    public List<Mesa> obtenerTodas() throws SQLException {

        List<Mesa> mesas = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE m.is_active = 1 " + "ORDER BY m.numero_mesa, m.id_mesa";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Mesa mesa = mapearMesa(rs);
                completarInformacionAtencion(conn, mesa);
                mesas.add(mesa);
            }
        }
        return mesas;
    }

    /**
     * Busca una mesa por su ID.
     */
    public Optional<Mesa> findById(int idMesa) throws SQLException {
        String sql = SELECT_BASE + "WHERE m.id_mesa = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             stmt.setInt(1, idMesa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearMesa(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Busca una mesa utilizando una conexión existente.
     * Se utiliza para operaciones transaccionales.
     */
    public Optional<Mesa> findById(Connection conn, int idMesa) throws SQLException {
        String sql = SELECT_BASE + "WHERE m.id_mesa = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMesa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearMesa(rs));
                }
                return Optional.empty();
            }
        }
    }
    /**
     * Completa información adicional de la mesa.
     *
     * cantidad_comensales no existe actualmente en la tabla mesas,
     * por lo que se deja como null.
     */
    private void completarInformacionAtencion(Connection conn, Mesa mesa) throws SQLException {
        mesa.setCantidadComensales(obtenerEnteroMesa(
                conn,
                mesa.getIdMesa(),
                "cantidad_comensales",
                "comensales",
                "numero_comensales"));
        mesa.setPedidoActivo(obtenerPedidoActivo(conn, mesa.getIdMesa()));
        mesa.setMeseroResponsable(obtenerMeseroResponsable(conn, mesa.getIdMesa()));
    }
    /**
     * Busca una columna de cantidad de comensales solamente si existe.
     */
    private Integer obtenerEnteroMesa(Connection conn, int idMesa, String... columnas) throws SQLException {
        for (String columna : columnas) {
            if (!existeColumna(conn, "mesas", columna)) {
                continue;
            }
            String sql = "SELECT " + columna + " FROM mesas WHERE id_mesa = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, idMesa);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int valor = rs.getInt(columna);
                        return rs.wasNull() ? null : valor;
                    }
                }
            }
        }
        return null;
    }
    /**
     * Obtiene el pedido activo asociado a una mesa.
     */
    private String obtenerPedidoActivo(Connection conn, int idMesa) throws SQLException {
        String tabla = primeraTablaExistente(conn, "pedidos", "pedido");
        if (tabla == null) {
            return null;
        }
        String idMesaColumna = primeraColumnaExistente(conn, tabla, "id_mesa", "mesa_id"
                );
        if (idMesaColumna == null) {
            return null;
        }
        String idPedidoColumna = primeraColumnaExistente(conn, tabla, "pedido_id", "id_pedido", "id"
                );

        String estadoColumna = primeraColumnaExistente(conn, tabla, "estado", "codigo_estado", "estado_pedido"
                );

        String totalColumna = primeraColumnaExistente(conn, tabla, "total", "valor_total", "monto_total"
                );

        if (idPedidoColumna == null) {
            return null;
        }
        StringBuilder sql = new StringBuilder("SELECT " + idPedidoColumna);
        if (estadoColumna != null) {
            sql.append(", ").append(estadoColumna);
        }
        if (totalColumna != null) {
            sql.append(", ").append(totalColumna);
        }
        sql.append(" FROM ")
                .append(tabla)
                .append(" WHERE ")
                .append(idMesaColumna)
                .append(" = ?");
        if (estadoColumna != null) {
            sql.append(" AND UPPER(").append(estadoColumna).append(") NOT IN " + "('PAGADO', 'CANCELADO', 'CERRADO', 'FINALIZADO')");
        }
        sql.append(" ORDER BY ")
                .append(idPedidoColumna)
                .append(" DESC LIMIT 1");
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, idMesa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String pedido = "Pedido #" + rs.getString(idPedidoColumna);
                if (estadoColumna != null) {
                    pedido += " - " + rs.getString(estadoColumna);
                }
                if (totalColumna != null) {
                    pedido += " - Total: " + rs.getBigDecimal(totalColumna);
                }
                return pedido;
            }
        }
    }

    /**
     * Obtiene el nombre del mesero asociado al pedido activo.
     */
    private String obtenerMeseroResponsable(Connection conn, int idMesa) throws SQLException {
        if (existeColumna(conn, "mesas", "mesero_responsable")) {
            return obtenerTextoMesa(conn, idMesa, "mesero_responsable");
        }

        if (existeColumna(conn, "mesas", "nombre_mesero")) {
            return obtenerTextoMesa(conn, idMesa, "nombre_mesero");
        }
        String tablaPedidos = primeraTablaExistente(
                        conn,
                        "pedidos",
                        "pedido"
                );
        String tablaMeseros =
                primeraTablaExistente(
                        conn,
                        "meseros",
                        "usuarios",
                        "empleados"
                );
        if (tablaPedidos == null || tablaMeseros == null) {
            return null;
        }
        String idMesaColumna = primeraColumnaExistente(
                        conn,
                        tablaPedidos,
                        "id_mesa",
                        "mesa_id"
                );
        if (idMesaColumna == null) {
            return null;
        }
        String idMeseroPedido = primeraColumnaExistente(conn, tablaPedidos, "id_mesero", "mesero_id", "id_usuario", "usuario_id"
                );

        String idMeseroTabla = primeraColumnaExistente(conn, tablaMeseros, "id_mesero", "mesero_id", "id_usuario", "usuario_id", "id"
                );

        String nombreMesero = primeraColumnaExistente(
                        conn,
                        tablaMeseros,
                        "nombre",
                        "nombre_completo",
                        "username",
                        "usuario"
                );
        String idPedidoColumna = primeraColumnaExistente(
                        conn,
                        tablaPedidos,
                        "pedido_id",
                        "id_pedido",
                        "id"
                );
        String estadoColumna = primeraColumnaExistente(
                        conn,
                        tablaPedidos,
                        "estado",
                        "codigo_estado",
                        "estado_pedido"
                );
        if (idMeseroPedido == null || idMeseroTabla == null || nombreMesero == null || idPedidoColumna == null) {
            return null;
        }
        StringBuilder sql = new StringBuilder("SELECT m." + nombreMesero + " FROM " + tablaPedidos + " p ");
        sql.append("JOIN ")
                .append(tablaMeseros)
                .append(" m ON p.")
                .append(idMeseroPedido)
                .append(" = m.")
                .append(idMeseroTabla);
        sql.append(" WHERE p.")
                .append(idMesaColumna)
                .append(" = ?");
        if (estadoColumna != null) {
            sql.append(" AND UPPER(p.").append(estadoColumna).append(") NOT IN " + "('PAGADO', 'CANCELADO', 'CERRADO', 'FINALIZADO')");
        }

        sql.append(" ORDER BY p.")
                .append(idPedidoColumna)
                .append(" DESC LIMIT 1");
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, idMesa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(nombreMesero);
                }
                return null;
            }
        }
    }
    /**
     * Obtiene un texto directamente de la tabla mesas.
     */
    private String obtenerTextoMesa(Connection conn, int idMesa, String columna) throws SQLException {
        String sql = "SELECT " + columna + " FROM mesas WHERE id_mesa = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMesa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(columna);
                }
                return null;
            }
        }
    }
    /**
     * Busca la primera tabla que exista.
     */
    private String primeraTablaExistente(Connection conn, String... tablas) throws SQLException {
        for (String tabla : tablas) {
            if (existeTabla(conn, tabla)) {
                return tabla;
            }
        }
        return null;
    }
    /**
     * Busca la primera columna existente.
     */
    private String primeraColumnaExistente(Connection conn, String tabla, String... columnas) throws SQLException {
        for (String columna : columnas) {
            if (existeColumna(conn, tabla, columna)) {
                return columna;
            }
        }
        return null;
    }

    /**
     * Comprueba si existe una tabla.
     */
    private boolean existeTabla(Connection conn, String tabla) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getTables(null, null, tabla, null)) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = metaData.getTables(null, null, tabla.toUpperCase(), null)) {
            return rs.next();
        }
    }
    /**
     * Comprueba si existe una columna.
     * Comprueba si existe una columna.
     */
    private boolean existeColumna(Connection conn, String tabla, String columna) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tabla, columna)) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = metaData.getColumns(null, null, tabla.toUpperCase(), columna.toUpperCase())) {

            return rs.next();
        }
    }
    /**
     * Asigna una cantidad de comensales a una mesa.
     * La tabla actual no contiene una columna para guardar
     * cantidad_comensales, por lo que aquí únicamente se valida
     * la capacidad y se cambia el estado a OCUPADA.
     */
    public boolean asignarComensales(int idMesa, int cantidadComensales) throws SQLException {

        if (cantidadComensales <= 0) {
            throw new IllegalArgumentException("La cantidad de comensales debe ser mayor a cero.");
        }
        String sql =
                "UPDATE mesas " +
                        "SET cantidad_comensales = ?, " +
                        "    id_estado_mesa = (" +
                        "        SELECT id_estado_mesa FROM estados_mesa " +
                        "        WHERE UPPER(codigo_estado) = 'OCUPADA'" +
                        "    ) " +
                        "WHERE id_mesa = ? " +
                        "AND ? <= capacidad " +
                        "AND UPPER((" +
                        "    SELECT codigo_estado FROM estados_mesa " +
                        "    WHERE id_estado_mesa = mesas.id_estado_mesa" +
                        ")) IN ('LIBRE', 'DISPONIBLE')";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cantidadComensales);
            stmt.setInt(2, idMesa);
            stmt.setInt(3, cantidadComensales);
            return stmt.executeUpdate() > 0;
        }
    }

    public void actualizarComensales(long mesaId, int cantidadComensales) throws SQLException {
        // HU-155: la clave primaria de mesas es id_mesa. Con "id" el UPDATE
        // fallaba siempre con "column \"id\" does not exist" y la cantidad
        // de comensales nunca llegaba a guardarse.
        String sql = "UPDATE mesas SET cantidad_comensales = ? WHERE id_mesa = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, cantidadComensales);
            stmt.setLong(2, mesaId);
            stmt.executeUpdate();
        }
    }

    /**
     * Obtiene el ID de un estado de mesa.
     */
    private Integer obtenerIdEstado(Connection conn, String... codigosEstado) throws SQLException {
        String sql = "SELECT id_estado_mesa " + "FROM estados_mesa " + "WHERE UPPER(codigo_estado) = UPPER(?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String codigo : codigosEstado) {
                stmt.setString(1, codigo);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                       return rs.getInt("id_estado_mesa");
                    }
                }
            }
        }
        return null;
    }
    /**
     * Libera una mesa y la devuelve a DISPONIBLE.
     */
    public void liberarMesa(int idMesa) throws SQLException {
        String sql =
                "UPDATE mesas " +
                        "SET id_estado_mesa = ?, " +
                        "    cantidad_comensales = NULL " +
                        "WHERE id_mesa = ?";

        try (Connection conn = ConexionBD.getConnection()) {
            Integer idLibre = obtenerIdEstado(conn, "LIBRE", "DISPONIBLE");

            if (idLibre == null) {
                throw new SQLException("No existe un estado LIBRE o DISPONIBLE en estados_mesa.");
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, idLibre);
                stmt.setInt(2, idMesa);
                stmt.executeUpdate();
            }
        }
    }
    /**
     * Agrega una nueva mesa.
     */
    public void agregarMesa(int numeroMesa, int capacidad) throws SQLException {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que cero.");
        }

        String sqlActiva =
                "SELECT 1 FROM mesas "
                        + "WHERE numero_mesa = ? AND id_zona = ? AND is_active = 1 LIMIT 1";

        String sqlInactiva =
                "SELECT id_mesa FROM mesas "
                        + "WHERE numero_mesa = ? AND id_zona = ? AND is_active = 0 "
                        + "ORDER BY id_mesa DESC LIMIT 1";

        String sqlReactivar =
                "UPDATE mesas SET "
                        + "is_active = 1, "
                        + "capacidad = ?, "
                        + "codigo_mesa = ?, "
                        + "id_estado_mesa = 1 "
                        + "WHERE id_mesa = ?";

        String sqlInsertar =
                "INSERT INTO mesas "
                        + "(numero_mesa, codigo_mesa, capacidad, id_zona, id_estado_mesa, is_active) "
                        + "VALUES (?, ?, ?, ?, 1, 1)";

        String codigo = generarCodigoMesa(ZONA_POR_DEFECTO, numeroMesa);

        try (Connection conn = ConexionBD.getConnection()) {
            // 1) Ya activa → error
            try (PreparedStatement stmt = conn.prepareStatement(sqlActiva)) {
                stmt.setInt(1, numeroMesa);
                stmt.setInt(2, ZONA_POR_DEFECTO);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        throw new SQLException(
                                "Ya existe una mesa activa con el número "
                                        + numeroMesa + " en esta zona.",
                                "23505");
                    }
                }
            }

            // 2) Inactiva → REACTIVAR (no solo return)
            try (PreparedStatement stmt = conn.prepareStatement(sqlInactiva)) {
                stmt.setInt(1, numeroMesa);
                stmt.setInt(2, ZONA_POR_DEFECTO);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int idMesa = rs.getInt("id_mesa");
                        try (PreparedStatement up = conn.prepareStatement(sqlReactivar)) {
                            up.setInt(1, capacidad);
                            up.setString(2, codigo);
                            up.setInt(3, idMesa);
                            int filas = up.executeUpdate();
                            if (filas == 0) {
                                throw new SQLException(
                                        "No se pudo reactivar la mesa " + numeroMesa);
                            }
                        }
                        return;
                    }
                }
            }

            // 3) No existe → insertar
            try (PreparedStatement stmt = conn.prepareStatement(sqlInsertar)) {
                stmt.setInt(1, numeroMesa);
                stmt.setString(2, codigo);
                stmt.setInt(3, capacidad);
                stmt.setInt(4, ZONA_POR_DEFECTO);
                stmt.executeUpdate();
            }
        }
    }
    /**
     * Genera el código de una mesa.
     */
    private String generarCodigoMesa(int idZona, int numeroMesa) {
    return String.format("MESA-%03d", numeroMesa);
}

    /**
     * Cambia el estado usando una conexión existente.
     */
    public void cambiarEstadoMesa(Connection conn, int idMesa, String codigoEstado) throws SQLException {
        String sql =
                "UPDATE mesas " +
                "SET id_estado_mesa = (" +
                "        SELECT id_estado_mesa " +
                "        FROM estados_mesa " +
                "        WHERE UPPER(codigo_estado) = UPPER(?)" +
                "    ), " +
                "    cantidad_comensales = CASE " +
                "        WHEN UPPER(?) IN ('LIBRE', 'DISPONIBLE') THEN NULL " +
                "        ELSE cantidad_comensales " +
                "    END " +
                "WHERE id_mesa = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigoEstado);
            stmt.setString(2, codigoEstado);
            stmt.setInt(3, idMesa);

            int filasActualizadas = stmt.executeUpdate();
            if (filasActualizadas == 0) {
                throw new SQLException("No se pudo cambiar el estado: la mesa no existe.");
            }
        }
    }

    /**
     * Cambia la capacidad de una mesa.
     */
    public void cambiarCapacidadMesa(int idMesa, int capacidad) throws SQLException {

        if (capacidad <= 0) {throw new SQLException("La capacidad debe ser mayor que cero.");
        }

        String sql =
                "UPDATE mesas " +
                        "SET capacidad = ? " +
                        "WHERE id_mesa = ? " +
                        "AND (cantidad_comensales IS NULL " +
                        "     OR cantidad_comensales <= ?)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, capacidad);
            stmt.setInt(2, idMesa);
            stmt.setInt(3, capacidad);
            int filasActualizadas = stmt.executeUpdate();

            if (filasActualizadas == 0) {
                throw new SQLException(
                        "No se pudo cambiar la capacidad. " +
                                "La mesa no existe o la capacidad es inferior " +
                                "a la cantidad de comensales actuales."
                );
            }
        }
    }
    /**
     * Convierte un ResultSet en una entidad Mesa.
     */
    private Mesa mapearMesa(ResultSet rs) throws SQLException {
        Mesa mesa = new Mesa();
        mesa.setIdMesa(rs.getInt("id_mesa"));
        mesa.setNumeroMesa(rs.getInt("numero_mesa"));
        mesa.setCodigoMesa(rs.getString("codigo_mesa"));
        mesa.setCapacidad(rs.getInt("capacidad"));
        mesa.setIdZona(rs.getInt("id_zona"));
        mesa.setNombreZona(rs.getString("nombre_zona"));
        mesa.setIdEstadoMesa(rs.getInt("id_estado_mesa"));
        mesa.setCodigoEstado(rs.getString("codigo_estado"));
        return mesa;
    }
    /**
     * Desactiva una mesa solamente si no tiene pedidos activos.
     * La validación y la desactivación se realizan en una transacción.
     */
    public void quitarMesa(int idMesa) throws SQLException {
        String sqlBloquearMesa =
                "SELECT id_mesa FROM mesas " +
                        "WHERE id_mesa = ? AND is_active = 1 FOR UPDATE";

        String sqlPedidoActivo =
                "SELECT 1 FROM pedidos " +
                        "WHERE mesa_id = ? " +
                        "AND UPPER(estado) NOT IN " +
                        "('CANCELADO', 'COMPLETADO', 'ENTREGADO', " +
                        "'PAGADO', 'CERRADO', 'FINALIZADO') " +
                        "LIMIT 1";

        String sqlDesactivar =
                "UPDATE mesas SET is_active = 0 " +
                        "WHERE id_mesa = ? AND is_active = 1";

        try (Connection conn = ConexionBD.getConnection()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                // Verifica que la mesa exista y la bloquea durante la operación.
                try (PreparedStatement stmt =
                             conn.prepareStatement(sqlBloquearMesa)) {
                    stmt.setInt(1, idMesa);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new exceptions.MesaNotFoundException(
                                    "La mesa no existe o ya fue retirada."
                            );
                        }
                    }
                }

                // No se permite retirar mesas con pedidos activos.
                try (PreparedStatement stmt =
                             conn.prepareStatement(sqlPedidoActivo)) {
                    stmt.setInt(1, idMesa);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            throw new exceptions.MesaConPedidoActivoException(
                                    "No se puede quitar esta mesa porque tiene " +
                                            "un pedido activo. Finaliza o cancela el " +
                                            "pedido antes de retirar la mesa."
                            );
                        }
                    }
                }

                // Desactiva la mesa sin eliminar su historial.
                try (PreparedStatement stmt =
                             conn.prepareStatement(sqlDesactivar)) {
                    stmt.setInt(1, idMesa);

                    if (stmt.executeUpdate() != 1) {
                        throw new SQLException(
                                "No se pudo retirar la mesa."
                        );
                    }
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommitOriginal);
            }
        }
    }
}
