package repository;

import conf.ConexionDB;
import entity.Mesa;
import java.sql.*;
import java.util.*;

public class MesaRepository {

    public List<Mesa> obtenerTodas() throws SQLException {
        List<Mesa> mesas = new ArrayList<>();
        String sql = "SELECT m.id_mesa, m.numero_mesa, m.codigo_mesa, m.capacidad, " +
                "       m.id_zona, z.nombre_zona, " +
                "       m.id_estado_mesa, e.codigo_estado " +
                "FROM mesas m " +
                "JOIN zonas z ON m.id_zona = z.id_zona " +
                "JOIN estados_mesa e ON m.id_estado_mesa = e.id_estado_mesa " +
                "WHERE m.is_active = 1 " +
                "ORDER BY m.numero_mesa";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Mesa mesa = new Mesa();
                mesa.setIdMesa(rs.getInt("id_mesa"));
                mesa.setNumeroMesa(rs.getInt("numero_mesa"));
                mesa.setCodigoMesa(rs.getString("codigo_mesa"));
                mesa.setCapacidad(rs.getInt("capacidad"));
                mesa.setIdZona(rs.getInt("id_zona"));
                mesa.setNombreZona(rs.getString("nombre_zona"));
                mesa.setIdEstadoMesa(rs.getInt("id_estado_mesa"));
                mesa.setCodigoEstado(rs.getString("codigo_estado"));
                completarInformacionAtencion(conn, mesa);
                mesas.add(mesa);
            }
        }
        return mesas;
    }

    private void completarInformacionAtencion(Connection conn, Mesa mesa) throws SQLException {
        mesa.setCantidadComensales(obtenerEnteroMesa(conn, mesa.getIdMesa(),
                "cantidad_comensales", "comensales", "numero_comensales"));
        mesa.setPedidoActivo(obtenerPedidoActivo(conn, mesa.getIdMesa()));
        mesa.setMeseroResponsable(obtenerMeseroResponsable(conn, mesa.getIdMesa()));
    }

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

    private String obtenerPedidoActivo(Connection conn, int idMesa) throws SQLException {
        String tabla = primeraTablaExistente(conn, "pedidos", "pedido");
        if (tabla == null || !existeAlgunaColumna(conn, tabla, "id_mesa", "mesa_id")) {
            return null;
        }

        String idMesaColumna = existeColumna(conn, tabla, "id_mesa") ? "id_mesa" : "mesa_id";
        String idPedidoColumna = primeraColumnaExistente(conn, tabla, "id_pedido", "pedido_id", "id");
        String estadoColumna = primeraColumnaExistente(conn, tabla, "estado", "codigo_estado", "estado_pedido");
        String totalColumna = primeraColumnaExistente(conn, tabla, "total", "valor_total", "monto_total");

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
        sql.append(" FROM ").append(tabla).append(" WHERE ").append(idMesaColumna).append(" = ?");
        if (estadoColumna != null) {
            sql.append(" AND UPPER(").append(estadoColumna).append(") NOT IN ('PAGADO', 'CANCELADO', 'CERRADO', 'FINALIZADO')");
        }
        sql.append(" ORDER BY ").append(idPedidoColumna).append(" DESC LIMIT 1");

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

    private String obtenerMeseroResponsable(Connection conn, int idMesa) throws SQLException {
        if (existeColumna(conn, "mesas", "mesero_responsable")) {
            return obtenerTextoMesa(conn, idMesa, "mesero_responsable");
        }
        if (existeColumna(conn, "mesas", "nombre_mesero")) {
            return obtenerTextoMesa(conn, idMesa, "nombre_mesero");
        }

        String tablaPedidos = primeraTablaExistente(conn, "pedidos", "pedido");
        String tablaMeseros = primeraTablaExistente(conn, "meseros", "usuarios", "empleados");
        if (tablaPedidos == null || tablaMeseros == null) {
            return null;
        }

        String idMesaColumna = existeColumna(conn, tablaPedidos, "id_mesa") ? "id_mesa" : "mesa_id";
        if (!existeColumna(conn, tablaPedidos, idMesaColumna)) {
            return null;
        }

        String idMeseroPedido = primeraColumnaExistente(conn, tablaPedidos, "id_mesero", "mesero_id", "id_usuario", "usuario_id");
        String idMeseroTabla = primeraColumnaExistente(conn, tablaMeseros, "id_mesero", "mesero_id", "id_usuario", "usuario_id", "id");
        String nombreMesero = primeraColumnaExistente(conn, tablaMeseros, "nombre", "nombre_completo", "username", "usuario");
        String idPedidoColumna = primeraColumnaExistente(conn, tablaPedidos, "id_pedido", "pedido_id", "id");
        String estadoColumna = primeraColumnaExistente(conn, tablaPedidos, "estado", "codigo_estado", "estado_pedido");

        if (idMeseroPedido == null || idMeseroTabla == null || nombreMesero == null || idPedidoColumna == null) {
            return null;
        }

        StringBuilder sql = new StringBuilder("SELECT m." + nombreMesero + " FROM " + tablaPedidos + " p ");
        sql.append("JOIN ").append(tablaMeseros).append(" m ON p.").append(idMeseroPedido)
                .append(" = m.").append(idMeseroTabla);
        sql.append(" WHERE p.").append(idMesaColumna).append(" = ?");
        if (estadoColumna != null) {
            sql.append(" AND UPPER(p.").append(estadoColumna).append(") NOT IN ('PAGADO', 'CANCELADO', 'CERRADO', 'FINALIZADO')");
        }
        sql.append(" ORDER BY p.").append(idPedidoColumna).append(" DESC LIMIT 1");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, idMesa);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(nombreMesero) : null;
            }
        }
    }

    private String obtenerTextoMesa(Connection conn, int idMesa, String columna) throws SQLException {
        String sql = "SELECT " + columna + " FROM mesas WHERE id_mesa = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMesa);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(columna) : null;
            }
        }
    }

    private String primeraTablaExistente(Connection conn, String... tablas) throws SQLException {
        for (String tabla : tablas) {
            if (existeTabla(conn, tabla)) {
                return tabla;
            }
        }
        return null;
    }

    private String primeraColumnaExistente(Connection conn, String tabla, String... columnas) throws SQLException {
        for (String columna : columnas) {
            if (existeColumna(conn, tabla, columna)) {
                return columna;
            }
        }
        return null;
    }

    private boolean existeAlgunaColumna(Connection conn, String tabla, String... columnas) throws SQLException {
        return primeraColumnaExistente(conn, tabla, columnas) != null;
    }

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

    public void agregarMesa(int numeroMesa) throws SQLException {
        String sql = "INSERT INTO mesas (numero_mesa, capacidad, id_zona, id_estado_mesa) " +
                "VALUES (?, 2, 1, 1)";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, numeroMesa);
            stmt.executeUpdate();
        }
    }
    public void quitarMesa(int idMesa) throws SQLException {
        String sql = "UPDATE mesas SET is_active = 0 WHERE id_mesa = ?";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMesa);
            stmt.executeUpdate();
        }
    }
}
